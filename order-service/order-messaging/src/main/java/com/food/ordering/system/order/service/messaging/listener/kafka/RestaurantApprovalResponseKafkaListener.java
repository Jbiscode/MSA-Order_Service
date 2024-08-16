package com.food.ordering.system.order.service.messaging.listener.kafka;

import com.food.ordering.system.kafka.consumer.KafkaConsumer;
import com.food.ordering.system.kafka.order.avro.model.OrderApprovalStatus;
import com.food.ordering.system.kafka.order.avro.model.RestaurantApprovalResponseAvroModel;
import com.food.ordering.system.order.service.domain.exception.OrderNotFoundException;
import com.food.ordering.system.order.service.domain.ports.input.message.listener.restaurantapproval.RestaurantApprovalResponseMessageListener;
import com.food.ordering.system.order.service.messaging.mapper.OrderMessagingDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.food.ordering.system.order.service.domain.entity.Order.FAILURE_MESSAGE_DELIMITER;

@Slf4j
@RequiredArgsConstructor
@Component
public class RestaurantApprovalResponseKafkaListener implements KafkaConsumer<RestaurantApprovalResponseAvroModel> {

    private final RestaurantApprovalResponseMessageListener restaurantApprovalResponseMessageListener;
    private final OrderMessagingDataMapper orderMessagingDataMapper;

    @Override
    @KafkaListener(id = "${kafka-consumer-config.restaurant-approval-consumer-group-id}",
            topics = "${order-service.restaurant-approval-response-topic-name}")
    public void receive(@Payload List<RestaurantApprovalResponseAvroModel> messages,
                        @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
                        @Header(KafkaHeaders.OFFSET) List<Long> offsets) {
        log.info("{}개의 메시지수신 ... messages='{}' key='{}' partitions='{}' and offset='{}'",
                messages.size()
                , messages
                , keys.toString()
                , partitions.toString()
                , offsets.toString());

        messages.forEach(restaurantApprovalResponseAvroModel -> {

            try {
                if (OrderApprovalStatus.APPROVED == restaurantApprovalResponseAvroModel.getOrderApprovalStatus()) {
                    log.info("식당승인응답을 처리합니다. order id: {}", restaurantApprovalResponseAvroModel.getOrderId());
                    restaurantApprovalResponseMessageListener.orderApproved(
                            orderMessagingDataMapper.restaurantApprovalResponseAvroModelToRestaurantApprovalResponseEvent(restaurantApprovalResponseAvroModel)
                    );
                } else if (OrderApprovalStatus.REJECTED == restaurantApprovalResponseAvroModel.getOrderApprovalStatus()) {
                    log.info("식당승인응답을 실패했습니다. order id: {} with failureMessage {}", restaurantApprovalResponseAvroModel.getOrderId(),
                            String.join(FAILURE_MESSAGE_DELIMITER, restaurantApprovalResponseAvroModel.getFailureMessages()));
                    restaurantApprovalResponseMessageListener.orderRejected(
                            orderMessagingDataMapper.restaurantApprovalResponseAvroModelToRestaurantApprovalResponseEvent(restaurantApprovalResponseAvroModel)
                    );
                }
            } catch (OptimisticLockingFailureException e) {
                // 낙관적잠금은 예외처리가 필요하지않다. 다른 스레드가 작업을 완료했으니 끝내면 됨.
                log.error("주문결제응답을 처리하는 도중 오류가 발생했습니다. 낙관적잠금. restaurantApprovalResponseAvroModel: {} & error: {}",
                        restaurantApprovalResponseAvroModel, e.getMessage());
            } catch (OrderNotFoundException e){
                // 이 예외도 그냥 끝내면 됨. 주문을 찾을 수 없음. 예외를 잡아줘서 재시도를 막아줘야함.
                log.error("주문결제응답을 처리하는 도중 오류가 발생했습니다. 주문을 찾을 수 없음. restaurantApprovalResponseAvroModel: {} & error: {}",
                        restaurantApprovalResponseAvroModel, e.getMessage());
            }

        });
    }
}

