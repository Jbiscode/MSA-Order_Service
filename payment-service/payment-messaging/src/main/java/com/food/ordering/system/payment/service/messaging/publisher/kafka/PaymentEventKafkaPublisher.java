package com.food.ordering.system.payment.service.messaging.publisher.kafka;

import com.food.ordering.system.kafka.order.avro.model.PaymentResponseAvroModel;
import com.food.ordering.system.kafka.producer.service.service.KafkaMessageHelper;
import com.food.ordering.system.kafka.producer.service.service.KafkaProducer;
import com.food.ordering.system.outbox.OutboxStatus;
import com.food.ordering.system.payment.service.domain.config.PaymentServiceConfigData;
import com.food.ordering.system.payment.service.domain.outbox.model.OrderEventPayload;
import com.food.ordering.system.payment.service.domain.outbox.model.OrderOutboxMessage;
import com.food.ordering.system.payment.service.domain.ports.output.message.publisher.PaymentResponseMessagePublisher;
import com.food.ordering.system.payment.service.messaging.mapper.PaymentMessagingDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentEventKafkaPublisher implements PaymentResponseMessagePublisher {

    private final PaymentMessagingDataMapper paymentMessagingDataMapper;
    private final PaymentServiceConfigData paymentServiceConfigData;
    private final KafkaProducer<String, PaymentResponseAvroModel> kafkaProducer;
    private final KafkaMessageHelper kafkaMessageHelper;

    @Override
    public void publish(OrderOutboxMessage orderOutboxMessage,
                        BiConsumer<OrderOutboxMessage, OutboxStatus> outboxCallback) {
        OrderEventPayload orderEventPayload =
                kafkaMessageHelper.getOrderEventPayload(
                        orderOutboxMessage.getPayload(),
                        OrderEventPayload.class
                );

        String SagaId = orderOutboxMessage.getSagaId().toString();

        log.info("OrderOutboxMessage orderId: {} SagaId: {} 를 수신했습니다.",
                orderEventPayload.getOrderId(),
                SagaId);

        try {
            PaymentResponseAvroModel paymentResponseAvroModel =
                    paymentMessagingDataMapper.orderEventPayloadToPaymentResponseAvroModel(
                            SagaId,
                            orderEventPayload
                    );

            kafkaProducer.send(
                    paymentServiceConfigData.getPaymentResponseTopicName(),
                    SagaId,
                    paymentResponseAvroModel,
                    kafkaMessageHelper.createKafkaMessageHelperRequest(
                            orderOutboxMessage,
                            outboxCallback,
                            orderEventPayload.getOrderId(),
                            "PaymentResponseAvroModel"
                    )
            );
            log.info("PaymentResponseAvroModel 이 성공적으로 발행되었습니다. orderId: {} SagaId: {}",
                    orderEventPayload.getOrderId(),
                    SagaId);
        } catch (Exception e) {
            log.error("PaymentResponseAvroModel 발행 중 에러가 발생했습니다. orderId: {} SagaId: {}",
                    orderEventPayload.getOrderId(),
                    SagaId);
        }
    }
}