package com.food.ordering.system.payment.service.domain.event;

import com.food.ordering.system.domain.event.DomainEvent;
import com.food.ordering.system.payment.service.domain.entity.Payment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
@RequiredArgsConstructor
public abstract class PaymentEvent implements DomainEvent<Payment>{

    private final Payment payment;
    private final ZonedDateTime createdAt;
    private final List<String> failureMessages;
}
