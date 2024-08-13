package com.food.ordering.system.payment.service.domain.event;

import com.food.ordering.system.domain.event.publisher.DomainEventPublisher;
import com.food.ordering.system.payment.service.domain.entity.Payment;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * PaymentCompletedEvent 에서 PaymentCompletedMessagePublisher 를 사용해야하는데,<br>
 * 직접적으로 사용하지 못하고 DomainEventPublisher<PaymentCompletedEvent>를 확장한 PaymentCompletedMessagePublisher를 사용해야한다.(의존성 역전 원칙)<br>
 * 의존성 주입은 객체가 다른 객체를 사용할 때, 객체를 생성하는 쪽에서 주입하는 것이다.<br>
 * 역전은 객체가 다른 객체를 사용할 때, 객체를 생성하는 쪽이 아닌 객체를 사용하는 쪽에서 주입하는 것이다.<br>
 * <br>
 * 직접 사용하지 못하는이유는 pom에 직접적으로 의존성을 추가하면 순환참조 문제가 발생할 수 있기 때문.<br>
 * PaymentCompletedEvent는 core에 있고, PaymentCompletedMessagePublisher는 application-service에 있다.<br>
 */
public class PaymentCompletedEvent extends PaymentEvent {

    private final DomainEventPublisher<PaymentCompletedEvent> paymentCompletedEventDomainEventPublisher;

    public PaymentCompletedEvent(Payment payment,
                                ZonedDateTime createdAt,
                                DomainEventPublisher<PaymentCompletedEvent> paymentCompletedEventDomainEventPublisher) {
        super(payment, createdAt, new ArrayList<>());
        this.paymentCompletedEventDomainEventPublisher = paymentCompletedEventDomainEventPublisher;
    }

    // 여기서 this는 PaymentCompletedEvent를 의미한다.
    @Override
    public void fire() {
        paymentCompletedEventDomainEventPublisher.publish(this);
    }
}
