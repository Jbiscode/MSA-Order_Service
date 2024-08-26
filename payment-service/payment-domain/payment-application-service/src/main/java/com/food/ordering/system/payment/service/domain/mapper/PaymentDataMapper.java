package com.food.ordering.system.payment.service.domain.mapper;

import com.food.ordering.system.domain.valueobject.CustomerId;
import com.food.ordering.system.domain.valueobject.Money;
import com.food.ordering.system.domain.valueobject.OrderId;
import com.food.ordering.system.payment.service.domain.dto.PaymentRequest;
import com.food.ordering.system.payment.service.domain.entity.Payment;
import com.food.ordering.system.payment.service.domain.event.PaymentEvent;
import com.food.ordering.system.payment.service.domain.outbox.model.OrderEventPayload;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaymentDataMapper {

    public Payment paymentRequestToPayment(PaymentRequest paymentRequest) {
        return Payment.builder()
                .orderId(new OrderId(UUID.fromString(paymentRequest.getOrderId())))
                .customerId(new CustomerId(UUID.fromString(paymentRequest.getCustomerId())))
                .price(new Money(paymentRequest.getPrice()))
                .build();
    }

    public OrderEventPayload paymentEventToOrderEventPayload(PaymentEvent paymentEvent){
        return OrderEventPayload.builder()
                .paymentId(paymentEvent.getPayment().getId().toString())
                .orderId(paymentEvent.getPayment().getOrderId().toString())
                .customerId(paymentEvent.getPayment().getCustomerId().toString())
                .price(paymentEvent.getPayment().getPrice().getAmount())
                .paymentStatus(paymentEvent.getPayment().getPaymentStatus().name())
                .createdAt(paymentEvent.getPayment().getCreatedAt())
                .failureMessages(paymentEvent.getFailureMessages())
                .build();
    }
}
