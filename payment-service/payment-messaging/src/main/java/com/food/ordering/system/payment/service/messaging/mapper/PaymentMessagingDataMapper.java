package com.food.ordering.system.payment.service.messaging.mapper;

import com.food.ordering.system.kafka.order.avro.model.PaymentRequestAvroModel;
import com.food.ordering.system.kafka.order.avro.model.PaymentResponseAvroModel;
import com.food.ordering.system.kafka.order.avro.model.PaymentStatus;
import com.food.ordering.system.payment.service.domain.dto.PaymentRequest;
import com.food.ordering.system.payment.service.domain.outbox.model.OrderEventPayload;
import org.springframework.stereotype.Component;

import java.util.UUID;


@Component
public class PaymentMessagingDataMapper {

    public PaymentResponseAvroModel orderEventPayloadToPaymentResponseAvroModel(String SagaId,
                                                                                OrderEventPayload orderEventPayload) {
        return PaymentResponseAvroModel.newBuilder()
                .setId(UUID.randomUUID())
                .setSagaId(UUID.fromString(SagaId))
                .setPaymentId(UUID.fromString(orderEventPayload.getPaymentId()))
                .setCustomerId(UUID.fromString(orderEventPayload.getCustomerId()))
                .setOrderId(UUID.fromString(orderEventPayload.getOrderId()))
                .setPrice(orderEventPayload.getPrice())
                .setCreatedAt(orderEventPayload.getCreatedAt().toInstant())
                .setPaymentStatus(PaymentStatus.valueOf(orderEventPayload.getPaymentStatus()))
                .setFailureMessages(orderEventPayload.getFailureMessages())
                .build();
    }


    public PaymentRequest paymentRequestAvroModelToPaymentRequest(PaymentRequestAvroModel paymentRequestAvroModel) {
        return PaymentRequest.builder()
                .orderId(paymentRequestAvroModel.getOrderId().toString())
                .customerId(paymentRequestAvroModel.getCustomerId().toString())
                .price(paymentRequestAvroModel.getPrice())
                .build();
    }

}
