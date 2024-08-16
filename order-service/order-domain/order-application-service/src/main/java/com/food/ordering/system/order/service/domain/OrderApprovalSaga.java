package com.food.ordering.system.order.service.domain;

import com.food.ordering.system.domain.valueobject.OrderApprovalStatus;
import com.food.ordering.system.domain.valueobject.OrderStatus;
import com.food.ordering.system.order.service.domain.dto.message.RestaurantApprovalResponse;
import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.event.OrderCancelledEvent;
import com.food.ordering.system.order.service.domain.exception.OrderDomainException;
import com.food.ordering.system.order.service.domain.mapper.OrderDataMapper;
import com.food.ordering.system.order.service.domain.outbox.model.approval.OrderApprovalOutboxMessage;
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
import static com.food.ordering.system.order.service.domain.entity.Order.FAILURE_MESSAGE_DELIMITER;

@Slf4j
@RequiredArgsConstructor
@Component
public class OrderApprovalSaga implements SagaStep<RestaurantApprovalResponse> {

    private final OrderDomainService orderDomainService;
    private final OrderSagaHelper orderSagaHelper;
    private final PaymentOutboxHelper paymentOutboxHelper;
    private final ApprovalOutboxHelper approvalOutboxHelper;
    private final OrderDataMapper orderDataMapper;

    @Override
    @Transactional
    public void process(RestaurantApprovalResponse data) {
        Optional<OrderApprovalOutboxMessage> orderApprovalOutboxMessageResponse =
                approvalOutboxHelper.getApprovalOutboxMessageBySagaIdAndSagaStatus(
                        UUID.fromString(data.getSagaId()),
                        SagaStatus.PROCESSING);
        if (orderApprovalOutboxMessageResponse.isEmpty()) {
            log.info("주문 승인 아웃박스 메시지 처리중 오류 발생: OrderApprovalOutboxMessage 가 이미 처리중입니다. SagaId: {}", data.getSagaId());
            return;
        }
        log.info("주문 승인 이벤트 수신중: {}", data.getOrderId());

        OrderApprovalOutboxMessage orderApprovalOutboxMessage = orderApprovalOutboxMessageResponse.get();

        Order order = approveOrder(data);

        SagaStatus sagaStatus = orderSagaHelper.orderStatusToSagaStatus(order.getOrderStatus());

        approvalOutboxHelper.save(
                getUpdatedApprovalOutboxMessage(
                        orderApprovalOutboxMessage,
                        order.getOrderStatus(),
                        sagaStatus)
        );

        paymentOutboxHelper.save(
                getUpdatedPaymentOutboxMessage(
                        data.getSagaId(),
                        order.getOrderStatus(),
                        sagaStatus
                ));
    }


    @Override
    @Transactional
    public void rollback(RestaurantApprovalResponse data) {
        Optional<OrderApprovalOutboxMessage> orderApprovalOutboxMessageResponse =
                approvalOutboxHelper.getApprovalOutboxMessageBySagaIdAndSagaStatus(
                        UUID.fromString(data.getSagaId()),
                        SagaStatus.PROCESSING);
        if (orderApprovalOutboxMessageResponse.isEmpty()) {
            log.error("주문 승인 롤백 이벤트 처리중 오류 발생: OrderApprovalOutboxMessage 가 이미 처리중입니다. SagaId: {}", data.getSagaId());
            return;
        }

        OrderCancelledEvent domainEvent = rollbackOrder(data);

        SagaStatus sagaStatus = orderSagaHelper.orderStatusToSagaStatus(domainEvent.getOrder().getOrderStatus());

        approvalOutboxHelper.save(
                getUpdatedApprovalOutboxMessage(
                        orderApprovalOutboxMessageResponse.get(),
                        domainEvent.getOrder().getOrderStatus(),
                        sagaStatus)
        );
        paymentOutboxHelper.savePaymentOutboxMessage(
                orderDataMapper.orderCancelledEventToOrderPaymentEventPayload(domainEvent),
                domainEvent.getOrder().getOrderStatus(),
                sagaStatus,
                OutboxStatus.STARTED,
                UUID.fromString(data.getSagaId())
        );

        log.info("주문 승인 CANCELLING ~ : {}", data.getOrderId());
    }

    private Order approveOrder(RestaurantApprovalResponse data) {
        Order order = orderSagaHelper.findOrder(data.getOrderId());
        orderDomainService.approveOrder(order);
        orderSagaHelper.saveOrder(order);
        log.info("주문 승인 완료: {}", data.getOrderId());
        return order;
    }

    private OrderCancelledEvent rollbackOrder(RestaurantApprovalResponse data) {
        log.info("주문 승인 롤백 이벤트 수신중: {} with failureMessages {}", data.getOrderId(), String.join(FAILURE_MESSAGE_DELIMITER,data.getFailureMessages()));
        Order order = orderSagaHelper.findOrder(data.getOrderId());
        OrderCancelledEvent domainEvent = orderDomainService.cancelOrderPayment(
                order,
                data.getFailureMessages());
        orderSagaHelper.saveOrder(order);
        return domainEvent;
    }

    private OrderApprovalOutboxMessage getUpdatedApprovalOutboxMessage(OrderApprovalOutboxMessage
                                                                             orderApprovalOutboxMessage,
                                                                     OrderStatus orderStatus,
                                                                     SagaStatus sagaStatus) {
        orderApprovalOutboxMessage.setProcessedAt(ZonedDateTime.now(ZoneId.of(ASIA_SEOUL)));
        orderApprovalOutboxMessage.setOrderStatus(orderStatus);
        orderApprovalOutboxMessage.setSagaStatus(sagaStatus);

        return orderApprovalOutboxMessage;
    }

    private OrderPaymentOutboxMessage getUpdatedPaymentOutboxMessage(String sagaId,
                                                                    OrderStatus orderStatus,
                                                                    SagaStatus sagaStatus) {
        Optional<OrderPaymentOutboxMessage> orderPaymentOutboxMessageResponse =
                paymentOutboxHelper.getPaymentOutboxMessageBySagaIdAndSagaStatus(
                        UUID.fromString(sagaId),
                        SagaStatus.PROCESSING);
        if (orderPaymentOutboxMessageResponse.isEmpty()) {
            throw new OrderDomainException("Payment outbox 메시지가" + SagaStatus.PROCESSING.name() + "상태로 존재하지 않습니다.");
        }

        OrderPaymentOutboxMessage orderPaymentOutboxMessage = orderPaymentOutboxMessageResponse.get();
        orderPaymentOutboxMessage.setProcessedAt(ZonedDateTime.now(ZoneId.of(ASIA_SEOUL)));
        orderPaymentOutboxMessage.setOrderStatus(orderStatus);
        orderPaymentOutboxMessage.setSagaStatus(sagaStatus);
        return orderPaymentOutboxMessage;
    }
}
