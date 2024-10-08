package com.food.ordering.system.order.service.domain.outbox.scheduler.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.ordering.system.domain.valueobject.OrderStatus;
import com.food.ordering.system.order.service.domain.exception.OrderDomainException;
import com.food.ordering.system.order.service.domain.outbox.model.approval.OrderApprovalEventPayload;
import com.food.ordering.system.order.service.domain.outbox.model.payment.OrderPaymentEventPayload;
import com.food.ordering.system.order.service.domain.outbox.model.payment.OrderPaymentOutboxMessage;
import com.food.ordering.system.order.service.domain.ports.output.repository.PaymentOutboxRepository;
import com.food.ordering.system.outbox.OutboxStatus;
import com.food.ordering.system.saga.SagaStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.food.ordering.system.saga.order.SagaConstants.ORDER_SAGA_NAME;

/**
 * Helper 에서 Outbox 의 메시지를 조회하는 기능을 구현합니다.
 * 조회만 하기때문에 Transactional 은 readOnly 로 설정.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentOutboxHelper {

    private final PaymentOutboxRepository paymentOutboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Optional<List<OrderPaymentOutboxMessage>> getPaymentOutboxMessageByOutboxStatusAndSagaStatus(OutboxStatus outboxStatus,
                                                                                                        SagaStatus... sagaStatuses) {
        return paymentOutboxRepository.findByTypeAndOutboxStatusAndSagaStatus(
                ORDER_SAGA_NAME,
                outboxStatus,
                sagaStatuses
        );
    }
    @Transactional(readOnly = true)
    public Optional<OrderPaymentOutboxMessage> getPaymentOutboxMessageBySagaIdAndSagaStatus(UUID sagaId,
                                                                                            SagaStatus... sagaStatuses) {
        return paymentOutboxRepository.findByTypeAndSagaIdAndSagaStatus(
                ORDER_SAGA_NAME,
                sagaId,
                sagaStatuses
        );
    }

    @Transactional
    public void save(OrderPaymentOutboxMessage orderPaymentOutboxMessage) {
        OrderPaymentOutboxMessage response = paymentOutboxRepository.save(orderPaymentOutboxMessage)
                .orElseThrow(()->{
                    log.error("OrderPaymentOutboxMessage 를 저장하는데 실패했습니다. OrderPaymentOutboxMessage: {}", orderPaymentOutboxMessage.getId());
                    return new OrderDomainException(String.format("OrderPaymentOutboxMessage 를 저장하는데 실패했습니다. Id=%s", orderPaymentOutboxMessage.getId()));
                });
        log.info("OrderPaymentOutboxMessage 를 저장했습니다. OrderPaymentOutboxMessage: {}", orderPaymentOutboxMessage.getId());
    }


    @Transactional
    public void savePaymentOutboxMessage(OrderPaymentEventPayload paymentEventPayload,
                                         OrderStatus orderStatus,
                                         SagaStatus sagaStatus,
                                         OutboxStatus outboxStatus,
                                         UUID sagaId) {
        save(OrderPaymentOutboxMessage.builder()
                .id(UUID.randomUUID())
                .sagaId(sagaId)
                .createdAt(paymentEventPayload.getCreatedAt())
                .type(ORDER_SAGA_NAME)
                .payload(createPayload(paymentEventPayload))
                .orderStatus(orderStatus)
                .sagaStatus(sagaStatus)
                .outboxStatus(outboxStatus)
                .build());
    }

    @Transactional
    public void deletePaymentOutboxMessageByOutboxStatusAndSagaStatus(OutboxStatus outboxStatus,
                                                                    SagaStatus... sagaStatus) {
        paymentOutboxRepository.deleteByTypeAndOutboxStatusAndSagaStatus(
                ORDER_SAGA_NAME,
                outboxStatus,
                sagaStatus
        );
    }


    private String createPayload(OrderPaymentEventPayload orderPaymentEventPayload) {
        try {
            return objectMapper.writeValueAsString(orderPaymentEventPayload);
        } catch (JsonProcessingException e) {
            log.error("OrderPaymentEventPayload 를 받지 못했습니다. for order id: {}",
                    orderPaymentEventPayload.getOrderId(), e);
            throw new OrderDomainException("OrderPaymentEventPayload 를 받지 못했습니다.for order id: " +
                    orderPaymentEventPayload.getOrderId(), e);
        }
    }
}
