package com.food.ordering.system.order.service.domain;

import com.food.ordering.system.domain.valueobject.OrderStatus;
import com.food.ordering.system.order.service.domain.dto.message.PaymentResponse;
import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.event.OrderPaidEvent;
import com.food.ordering.system.order.service.domain.mapper.OrderDataMapper;
import com.food.ordering.system.order.service.domain.outbox.model.payment.OrderPaymentOutboxMessage;
import com.food.ordering.system.order.service.domain.outbox.scheduler.approval.ApprovalOutboxHelper;
import com.food.ordering.system.order.service.domain.outbox.scheduler.payment.PaymentOutboxHelper;
import com.food.ordering.system.outbox.OutboxStatus;
import com.food.ordering.system.saga.SagaStatus;
import com.food.ordering.system.saga.SagaStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static com.food.ordering.system.domain.DomainConstants.ASIA_SEOUL;

@Slf4j
@RequiredArgsConstructor
@Component
public class OrderPaymentSaga implements SagaStep<PaymentResponse> {

    private final OrderDomainService orderDomainService;
    private final OrderSagaHelper orderSagaHelper;
    private final PaymentOutboxHelper paymentOutboxHelper;
    private final ApprovalOutboxHelper approvalOutboxHelper;
    private final OrderDataMapper orderDataMapper;

    @Override
    @Transactional
    public void process(PaymentResponse data) {
        log.info("결제 완료 이벤트 수신중: {}", data.getOrderId());
        Optional<OrderPaymentOutboxMessage> orderPaymentOutboxMessageResponse =
                paymentOutboxHelper.getPaymentOutboxMessageBySagaIdAndSagaStatus(
                    UUID.fromString(data.getSagaId()),
                    SagaStatus.STARTED);
        if(orderPaymentOutboxMessageResponse.isEmpty()) {
            log.error("결제 완료 이벤트 처리중 오류 발생: OrderPaymentOutboxMessage 가 이미 처리중입니다. SagaId: {}", data.getSagaId());
            return;
        }

        OrderPaymentOutboxMessage orderPaymentOutboxMessage = orderPaymentOutboxMessageResponse.get();

        Order order = orderSagaHelper.findOrder(data.getOrderId());
        OrderPaidEvent domainEvent = orderDomainService.payOrder(order);
        orderSagaHelper.saveOrder(order);
        SagaStatus sagaStatus = orderSagaHelper.orderStatusToSagaStatus(domainEvent.getOrder().getOrderStatus());

        paymentOutboxHelper.save(
                getUpdatedPaymentOutboxMessage(
                        orderPaymentOutboxMessage,
                        domainEvent.getOrder().getOrderStatus(),
                        sagaStatus)
        );

        approvalOutboxHelper
                .saveApprovalOutboxMessage(
                        orderDataMapper.orderPaidEventToOrderApprovalEventPayload(domainEvent),
                        domainEvent.getOrder().getOrderStatus(),
                        sagaStatus,
                        OutboxStatus.STARTED,
                        UUID.fromString(data.getSagaId())
                );
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

    private OrderPaymentOutboxMessage getUpdatedPaymentOutboxMessage(OrderPaymentOutboxMessage
                                                                             orderPaymentOutboxMessage,
                                                                     OrderStatus orderStatus,
                                                                     SagaStatus sagaStatus) {
        orderPaymentOutboxMessage.setProcessedAt(ZonedDateTime.now(ZoneId.of(ASIA_SEOUL)));
        orderPaymentOutboxMessage.setOrderStatus(orderStatus);
        orderPaymentOutboxMessage.setSagaStatus(sagaStatus);

        return orderPaymentOutboxMessage;
    }

}
