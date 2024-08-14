package com.food.ordering.system.order.service.domain;

import com.food.ordering.system.order.service.domain.dto.message.PaymentResponse;
import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.event.OrderPaidEvent;
import com.food.ordering.system.saga.SagaStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Component
public class OrderPaymentSaga implements SagaStep<PaymentResponse> {

    private final OrderDomainService orderDomainService;
    private final OrderSagaHelper orderSagaHelper;

    @Override
    @Transactional
    public void process(PaymentResponse data) {
        log.info("결제 완료 이벤트 수신중: {}", data.getOrderId());
        Order order = orderSagaHelper.findOrder(data.getOrderId());
        OrderPaidEvent domainEvent = orderDomainService.payOrder(order);
        orderSagaHelper.saveOrder(order);
        log.info("결제 완료 이벤트 처리 완료 OrderId: {}", order.getId().getValue());
    }

    @Override
    @Transactional
    public void rollback(PaymentResponse data) {
        log.info("결제 롤백 이벤트 수신중: {}", data.getOrderId());
        Order order = orderSagaHelper.findOrder(data.getOrderId());
        orderDomainService.cancelOrder(order, data.getFailureMessages());
        orderSagaHelper.saveOrder(order);
        log.info("결제 롤백 이벤트 처리 완료 OrderId: {}", order.getId().getValue());
    }
}
