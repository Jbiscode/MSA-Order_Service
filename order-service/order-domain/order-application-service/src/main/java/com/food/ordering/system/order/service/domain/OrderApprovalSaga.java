package com.food.ordering.system.order.service.domain;

import com.food.ordering.system.order.service.domain.dto.message.RestaurantApprovalResponse;
import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.event.OrderCancelledEvent;
import com.food.ordering.system.saga.SagaStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.food.ordering.system.order.service.domain.entity.Order.FAILURE_MESSAGE_DELIMITER;

@Slf4j
@RequiredArgsConstructor
@Component
public class OrderApprovalSaga implements SagaStep<RestaurantApprovalResponse> {

    private final OrderDomainService orderDomainService;
    private final OrderSagaHelper orderSagaHelper;

    @Override
    @Transactional
    public void process(RestaurantApprovalResponse data) {
        log.info("주문 승인 이벤트 수신중: {}", data.getOrderId());
        Order order = orderSagaHelper.findOrder(data.getOrderId());
        orderDomainService.approveOrder(order);
        orderSagaHelper.saveOrder(order);
        log.info("주문 승인 완료: {}", data.getOrderId());
    }

    @Override
    @Transactional
    public void rollback(RestaurantApprovalResponse data) {
        log.info("주문 승인 롤백 이벤트 수신중: {} with failureMessages {}", data.getOrderId(), String.join(FAILURE_MESSAGE_DELIMITER,data.getFailureMessages()));
        Order order = orderSagaHelper.findOrder(data.getOrderId());
        OrderCancelledEvent domainEvent = orderDomainService.cancelOrderPayment(
                order,
                data.getFailureMessages());
        orderSagaHelper.saveOrder(order);
        log.info("주문 승인 롤백 완료: {}", data.getOrderId());
    }
}
