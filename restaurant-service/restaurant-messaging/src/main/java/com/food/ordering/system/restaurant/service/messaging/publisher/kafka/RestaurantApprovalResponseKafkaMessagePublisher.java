package com.food.ordering.system.restaurant.service.messaging.publisher.kafka;

import com.food.ordering.system.kafka.order.avro.model.RestaurantApprovalResponseAvroModel;
import com.food.ordering.system.kafka.producer.service.service.KafkaMessageHelper;
import com.food.ordering.system.kafka.producer.service.service.KafkaProducer;
import com.food.ordering.system.outbox.OutboxStatus;
import com.food.ordering.system.restaurant.service.domain.config.RestaurantServiceConfigData;
import com.food.ordering.system.restaurant.service.domain.outbox.model.OrderEventPayload;
import com.food.ordering.system.restaurant.service.domain.outbox.model.OrderOutboxMessage;
import com.food.ordering.system.restaurant.service.domain.ports.output.message.publisher.RestaurantApprovalResponseMessagePublisher;
import com.food.ordering.system.restaurant.service.messaging.mapper.RestaurantMessagingDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;

@Slf4j
@RequiredArgsConstructor
@Component
public class RestaurantApprovalResponseKafkaMessagePublisher implements RestaurantApprovalResponseMessagePublisher {

    private final RestaurantMessagingDataMapper restaurantMessagingDataMapper;
    private final RestaurantServiceConfigData restaurantServiceConfigData;
    private final KafkaProducer<String, RestaurantApprovalResponseAvroModel> kafkaProducer;
    private final KafkaMessageHelper kafkaMessageHelper;

    @Override
    public void publish(OrderOutboxMessage orderOutboxMessage,
                        BiConsumer<OrderOutboxMessage, OutboxStatus> outboxCallback) {

        OrderEventPayload orderEventPayload =
                kafkaMessageHelper.getOrderEventPayload(
                        orderOutboxMessage.getPayload(),
                        OrderEventPayload.class
                );

        String sagaId = orderOutboxMessage.getSagaId().toString();

        log.info("OrderOutboxMessage orderId: {} SagaId: {} 를 수신했습니다.",
                orderEventPayload.getOrderId(),
                sagaId);

        try {
            RestaurantApprovalResponseAvroModel restaurantApprovalResponseAvroModel =
                    restaurantMessagingDataMapper.orderEventPayloadToRestaurantApprovalResponseAvroModel(
                            sagaId,
                            orderEventPayload
                    );

            kafkaProducer.send(
                    restaurantServiceConfigData.getRestaurantApprovalResponseTopicName(),
                    sagaId,
                    restaurantApprovalResponseAvroModel,
                    kafkaMessageHelper.createKafkaMessageHelperRequest(
                            orderOutboxMessage,
                            outboxCallback,
                            orderEventPayload.getOrderId(),
                            "RestaurantApprovalResponseAvroModel"
                    )
            );
            log.info("RestaurantApprovalResponseAvroModel 이 성공적으로 발행되었습니다. orderId: {} SagaId: {}",
                    orderEventPayload.getOrderId(),
                    sagaId);
        }catch (Exception e){
            log.error("RestaurantApprovalResponseAvroModel 발행 중 에러가 발생했습니다. orderId: {} SagaId: {}",
                    orderEventPayload.getOrderId(),
                    sagaId);
        }

    }
}
