package com.food.ordering.system.order.service.messaging.publisher.kafka;

import com.food.ordering.system.kafka.order.avro.model.RestaurantApprovalRequestAvroModel;
import com.food.ordering.system.kafka.producer.service.service.KafkaMessageHelper;
import com.food.ordering.system.kafka.producer.service.service.KafkaProducer;
import com.food.ordering.system.order.service.domain.config.OrderServiceConfigData;
import com.food.ordering.system.order.service.domain.outbox.model.approval.OrderApprovalEventPayload;
import com.food.ordering.system.order.service.domain.outbox.model.approval.OrderApprovalOutboxMessage;
import com.food.ordering.system.order.service.domain.ports.output.message.publisher.restaurantapproval.RestaurantApprovalRequestMessagePublisher;
import com.food.ordering.system.order.service.messaging.mapper.OrderMessagingDataMapper;
import com.food.ordering.system.outbox.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;

@Slf4j
@RequiredArgsConstructor
@Component
public class OrderApprovalEventKafkaPublisher implements RestaurantApprovalRequestMessagePublisher {

    private final OrderMessagingDataMapper orderMessagingDataMapper;
    private final OrderServiceConfigData orderServiceConfigData;
    private final KafkaProducer<String, RestaurantApprovalRequestAvroModel> kafkaProducer;
    private final KafkaMessageHelper kafkaMessageHelper;

    @Override
    public void publish(OrderApprovalOutboxMessage orderApprovalOutboxMessage,
                        BiConsumer<OrderApprovalOutboxMessage, OutboxStatus> outboxCallback) {
        OrderApprovalEventPayload orderApprovalEventPayload =
                kafkaMessageHelper.getOrderEventPayload(
                        orderApprovalOutboxMessage.getPayload(),
                        OrderApprovalEventPayload.class
                );

        String SagaId = orderApprovalOutboxMessage.getSagaId().toString();

        log.info("주문 ID: {} 및 saga ID: {}에 대한 주문 승인 아웃박스 메시지 수신중",
                orderApprovalEventPayload.getOrderId(),
                SagaId);

        try {
            RestaurantApprovalRequestAvroModel restaurantApprovalRequestAvroModel =
                    orderMessagingDataMapper.orderApprovalEventToRestaurantApprovalRequestAvroModel(
                            SagaId,
                            orderApprovalEventPayload
                    );

            kafkaProducer.send(
                    orderServiceConfigData.getRestaurantApprovalRequestTopicName(),
                    SagaId,
                    restaurantApprovalRequestAvroModel,
                    kafkaMessageHelper.createKafkaMessageHelperRequest(
                            orderApprovalOutboxMessage,
                            outboxCallback,
                            orderApprovalEventPayload.getOrderId(),
                            "RestaurantApprovalRequestAvroModel"
                    )
            );

            log.info("주문 ID: {} 및 saga ID: {}에 대한 주문 승인 아웃박스 메시지 발행 완료",
                    orderApprovalEventPayload.getOrderId(),
                    SagaId);

        }catch (Exception e){
            log.error("주문 ID: {} 및 saga ID: {}에 대한 주문 승인 아웃박스 메시지 발행 실패",
                    orderApprovalEventPayload.getOrderId(),
                    SagaId);
            outboxCallback.accept(orderApprovalOutboxMessage, OutboxStatus.FAILED);
        }
    }
}
