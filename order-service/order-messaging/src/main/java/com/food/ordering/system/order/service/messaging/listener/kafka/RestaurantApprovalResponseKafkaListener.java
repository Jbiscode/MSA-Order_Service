package com.food.ordering.system.order.service.messaging.listener.kafka;

import com.food.ordering.system.kafka.consumer.KafkaConsumer;
import com.food.ordering.system.kafka.order.avro.model.OrderApprovalStatus;
import com.food.ordering.system.kafka.order.avro.model.RestaurantApprovalResponseAvroModel;
import com.food.ordering.system.order.service.domain.ports.input.message.listener.restaurantapproval.RestaurantApprovalResponseMessageListener;
import com.food.ordering.system.order.service.messaging.mapper.OrderMessagingDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        });
    }
}

