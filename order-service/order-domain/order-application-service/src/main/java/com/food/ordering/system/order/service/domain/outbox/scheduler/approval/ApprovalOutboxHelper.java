package com.food.ordering.system.order.service.domain.outbox.scheduler.approval;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.ordering.system.domain.valueobject.OrderStatus;
import com.food.ordering.system.order.service.domain.exception.OrderDomainException;
import com.food.ordering.system.order.service.domain.outbox.model.approval.OrderApprovalEventPayload;
import com.food.ordering.system.order.service.domain.outbox.model.approval.OrderApprovalOutboxMessage;
import com.food.ordering.system.order.service.domain.ports.output.repository.ApprovalOutboxRepository;
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

@Slf4j
@RequiredArgsConstructor
@Component
public class ApprovalOutboxHelper {


    private final ApprovalOutboxRepository approvalOutboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Optional<List<OrderApprovalOutboxMessage>> getApprovalOutboxMessageByOutboxStatusAndSagaStatus(OutboxStatus outboxStatus,
                                                                                                          SagaStatus... sagaStatuses) {
        return approvalOutboxRepository.findByTypeAndOutboxStatusAndSagaStatus(
                ORDER_SAGA_NAME,
                outboxStatus,
                sagaStatuses
        );
    }

    @Transactional(readOnly = true)
    public Optional<OrderApprovalOutboxMessage> getApprovalOutboxMessageBySagaIdAndSagaStatus(UUID sagaId,
                                                                                              SagaStatus... sagaStatuses) {
        return approvalOutboxRepository.findByTypeAndSagaIdAndSagaStatus(
                ORDER_SAGA_NAME,
                sagaId,
                sagaStatuses
        );
    }

    @Transactional
    public void save(OrderApprovalOutboxMessage orderApprovalOutboxMessage) {
        OrderApprovalOutboxMessage response = approvalOutboxRepository.save(orderApprovalOutboxMessage)
                .orElseThrow(()->{
                    log.error("OrderApprovalOutboxMessage 를 저장하는데 실패했습니다. OrderApprovalOutboxMessage: {}", orderApprovalOutboxMessage.getId());
                    return new OrderDomainException(String.format("OrderApprovalOutboxMessage 를 저장하는데 실패했습니다. Id=%s", orderApprovalOutboxMessage.getId()));
                });
        log.info("OrderApprovalOutboxMessage 를 저장했습니다. OrderApprovalOutboxMessage: {}", orderApprovalOutboxMessage.getId());
    }

    @Transactional
    public void deleteApprovalOutboxMessageByOutboxStatusAndSagaStatus(OutboxStatus outboxStatus,
                                                                       SagaStatus... sagaStatus) {
        approvalOutboxRepository.deleteByTypeAndOutboxStatusAndSagaStatus(
                ORDER_SAGA_NAME,
                outboxStatus,
                sagaStatus
        );
    }

}
