package com.food.ordering.system.order.service.domain;

import com.food.ordering.system.domain.valueobject.OrderStatus;
import com.food.ordering.system.domain.valueobject.PaymentStatus;
import com.food.ordering.system.order.service.domain.dto.message.PaymentResponse;
import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.event.OrderPaidEvent;
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

        OrderPaidEvent domainEvent = completePaymentForOrder(data);

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
        log.info("결제 완료 이벤트 처리 완료 OrderId: {}", domainEvent.getOrder().getId().getValue());
    }

    @Override
    @Transactional
    public void rollback(PaymentResponse data) {
        log.info("결제 롤백 이벤트 수신: {}", data.getOrderId());
        Optional<OrderPaymentOutboxMessage> orderPaymentOutboxMessageResponse =
                paymentOutboxHelper.getPaymentOutboxMessageBySagaIdAndSagaStatus(
                        UUID.fromString(data.getSagaId()),
                        getCurrentSagaStatus(data.getPaymentStatus()));
        if(orderPaymentOutboxMessageResponse.isEmpty()) {
            log.error("결제 롤백 이벤트 처리중 오류 발생: OrderPaymentOutboxMessage 가 이미 처리중입니다. SagaId: {}", data.getSagaId());
            return;
        }
        Order order = rollbackPaymentForOrder(data);

        SagaStatus sagaStatus = orderSagaHelper.orderStatusToSagaStatus(order.getOrderStatus());

        paymentOutboxHelper.save(
                getUpdatedPaymentOutboxMessage(
                        orderPaymentOutboxMessageResponse.get(),
                        order.getOrderStatus(),
                        sagaStatus)
        );
        if(data.getPaymentStatus() == PaymentStatus.CANCELLED) {
            approvalOutboxHelper
                    .save(
                            getUpdatedApprovalOutboxMessage(
                                    data.getSagaId(),
                                    order.getOrderStatus(),
                                    sagaStatus
                            )
                    );
        }

        log.info("결제 롤백 이벤트 처리 완료 OrderId: {}", order.getId().getValue());
    }

    private SagaStatus[] getCurrentSagaStatus(PaymentStatus paymentStatus) {
        return switch (paymentStatus) {
            case COMPLETED -> new SagaStatus[]{SagaStatus.STARTED};
            case CANCELLED -> new SagaStatus[]{SagaStatus.PROCESSING};
            case FAILED -> new SagaStatus[]{SagaStatus.STARTED, SagaStatus.PROCESSING};
        };
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

    private OrderApprovalOutboxMessage getUpdatedApprovalOutboxMessage(String sagaId,
                                                                        OrderStatus orderStatus,
                                                                        SagaStatus sagaStatus){
        Optional<OrderApprovalOutboxMessage> orderApprovalOutboxMessageResponse =
                approvalOutboxHelper.getApprovalOutboxMessageBySagaIdAndSagaStatus(
                        UUID.fromString(sagaId),
                        SagaStatus.COMPENSATING
                );
        if(orderApprovalOutboxMessageResponse.isEmpty()) {
            throw new OrderDomainException("Approval outbox 메시지가 "+SagaStatus.COMPENSATING+" 상태가 아닙니다. SagaId: "+sagaId);
        }

        OrderApprovalOutboxMessage orderApprovalOutboxMessage = orderApprovalOutboxMessageResponse.get();
        orderApprovalOutboxMessage.setProcessedAt(ZonedDateTime.now(ZoneId.of(ASIA_SEOUL)));
        orderApprovalOutboxMessage.setOrderStatus(orderStatus);
        orderApprovalOutboxMessage.setSagaStatus(sagaStatus);
        return orderApprovalOutboxMessage;
    }


    private OrderPaidEvent completePaymentForOrder(PaymentResponse paymentResponse){
        log.info("결제 완료 처리중 .. 주문Id:{}", paymentResponse.getOrderId());
        Order order = orderSagaHelper.findOrder(paymentResponse.getOrderId());
        OrderPaidEvent domainEvent = orderDomainService.payOrder(order);
        orderSagaHelper.saveOrder(order);
        return domainEvent;
    }
    private Order rollbackPaymentForOrder(PaymentResponse paymentResponse){
        log.info("결제 롤백 처리중 .. 주문Id:{}", paymentResponse.getOrderId());
        Order order = orderSagaHelper.findOrder(paymentResponse.getOrderId());
        orderDomainService.cancelOrder(order, paymentResponse.getFailureMessages());
        orderSagaHelper.saveOrder(order);
        return order;
    }
}
