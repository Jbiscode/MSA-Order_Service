package com.food.ordering.system.order.service.messaging.publisher.kafka;

import com.food.ordering.system.kafka.order.avro.model.PaymentRequestAvroModel;
import com.food.ordering.system.kafka.producer.service.service.KafkaMessageHelper;
import com.food.ordering.system.kafka.producer.service.service.KafkaProducer;
import com.food.ordering.system.order.service.domain.config.OrderServiceConfigData;
import com.food.ordering.system.order.service.domain.outbox.model.payment.OrderPaymentEventPayload;
import com.food.ordering.system.order.service.domain.outbox.model.payment.OrderPaymentOutboxMessage;
import com.food.ordering.system.order.service.domain.ports.output.message.publisher.payment.PaymentRequestMessagePublisher;
import com.food.ordering.system.order.service.messaging.mapper.OrderMessagingDataMapper;
import com.food.ordering.system.outbox.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;

@Slf4j
@RequiredArgsConstructor
@Component
public class OrderPaymentEventKafkaPublisher implements PaymentRequestMessagePublisher {

    private final OrderMessagingDataMapper orderMessagingDataMapper;
    private final OrderServiceConfigData orderServiceConfigData;
    private final KafkaProducer<String, PaymentRequestAvroModel> kafkaProducer;
    private final KafkaMessageHelper kafkaMessageHelper;

    @Override
    public void publish(OrderPaymentOutboxMessage orderPaymentOutboxMessage,
                        BiConsumer<OrderPaymentOutboxMessage, OutboxStatus> outboxCallback) {
        OrderPaymentEventPayload orderPaymentEventPayload =
                kafkaMessageHelper.getOrderEventPayload(
                        orderPaymentOutboxMessage.getPayload(),
                        OrderPaymentEventPayload.class
                );

        String SagaId = orderPaymentOutboxMessage.getSagaId().toString();

        log.info("주문 ID: {} 및 saga ID: {}에 대한 주문 결제 아웃박스 메시지 수신중",
                orderPaymentEventPayload.getOrderId(),
                SagaId);

        try {
            PaymentRequestAvroModel paymentRequestAvroModel =
                    orderMessagingDataMapper.orderPaymentEventToPaymentRequestAvroModel(
                            SagaId,
                            orderPaymentEventPayload
                    );

            kafkaProducer.send(
                    orderServiceConfigData.getPaymentRequestTopicName(),
                    SagaId,
                    paymentRequestAvroModel,
                    kafkaMessageHelper.createKafkaMessageHelperRequest(
                            orderPaymentOutboxMessage,
                            outboxCallback,
                            orderPaymentEventPayload.getOrderId(),
                            "PaymentRequestAvroModel"
                    )
            );
            log.info("주문 ID: {} 및 saga ID: {}에 대한 주문 결제 아웃박스 메시지 발행 완료",
                    orderPaymentEventPayload.getOrderId(),
                    SagaId);
        }catch (Exception e){
            log.error("주문 ID: {} 및 saga ID: {}에 대한 주문 결제 아웃박스 메시지 발행 실패",
                    orderPaymentEventPayload.getOrderId(),
                    SagaId);
        }
    }
}
