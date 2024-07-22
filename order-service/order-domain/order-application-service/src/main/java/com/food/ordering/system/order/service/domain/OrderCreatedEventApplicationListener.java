package com.food.ordering.system.order.service.domain;

import com.food.ordering.system.order.service.domain.event.OrderCreatedEvent;
import com.food.ordering.system.order.service.domain.ports.output.message.publisher.payment.OrderCreatedPaymentRequestMessagePublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 주문 생성 이벤트를 처리하는 애플리케이션 리스너입니다.
 * 주문 생성 이벤트가 발생하면 결제 요청 메시지를 발행합니다.
 */
@Slf4j
@Component
public class OrderCreatedEventApplicationListener {

    private final OrderCreatedPaymentRequestMessagePublisher orderCreatedPaymentRequestMessagePublisher;

    public OrderCreatedEventApplicationListener(OrderCreatedPaymentRequestMessagePublisher orderCreatedPaymentRequestMessagePublisher) {
        this.orderCreatedPaymentRequestMessagePublisher = orderCreatedPaymentRequestMessagePublisher;
    }

    /**
     * * @Transactional 이 성공적으로 완료된 후에 이벤트를 처리합니다.
     * @param orderCreatedEvent
     */
    @TransactionalEventListener
    public void process(OrderCreatedEvent orderCreatedEvent) {
        log.info("OrderCreatedEventApplicationListener.process() - 주문이 생성되었습니다(리스너). orderId: {}", orderCreatedEvent.getOrder().getId().getValue());
        orderCreatedPaymentRequestMessagePublisher.publish(orderCreatedEvent);
    }
}
