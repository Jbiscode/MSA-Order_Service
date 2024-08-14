package com.food.ordering.system.order.service.domain.outbox.scheduler.approval;

import com.food.ordering.system.order.service.domain.outbox.model.approval.OrderApprovalOutboxMessage;
import com.food.ordering.system.order.service.domain.ports.output.message.publisher.restaurantapproval.RestaurantApprovalRequestMessagePublisher;
import com.food.ordering.system.outbox.OutboxScheduler;
import com.food.ordering.system.outbox.OutboxStatus;
import com.food.ordering.system.saga.SagaStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class RestaurantApprovalOutboxScheduler implements OutboxScheduler {

    private final ApprovalOutboxHelper approvalOutboxHelper;
    private final RestaurantApprovalRequestMessagePublisher restaurantApprovalRequestMessagePublisher;

    @Override
    @Transactional
    @Scheduled(fixedDelayString = "${order-service.outbox-scheduler-fixed-rate}",
            initialDelayString = "${order-service.outbox-scheduler-initial-delay}")
    public void processOutboxMessage() {
        Optional<List<OrderApprovalOutboxMessage>> outboxMessagesResponse =
                approvalOutboxHelper.getApprovalOutboxMessageByOutboxStatusAndSagaStatus(
                        OutboxStatus.STARTED,
                        SagaStatus.PROCESSING);

        outboxMessagesResponse
                .filter(outboxMessages -> !outboxMessages.isEmpty())
                .ifPresent(outboxMessages -> {
                    log.info("OrderApprovalOutbox 메시지를 {}개 수신했습니다. OrderApprovalOutboxMessage id{}",
                            outboxMessages.size(),
                            outboxMessages.stream()
                                    .map(outboxMessage ->
                                            outboxMessage.getId().toString())
                                    .collect(Collectors.joining(", ")));
                    outboxMessages.forEach(outboxMessage ->
                            restaurantApprovalRequestMessagePublisher.publish(outboxMessage, this::updateOutboxStatus));
                    log.info("OrderApprovalOutbox 메시지를 {}개 발행했습니다.", outboxMessages.size());
                });
    }

    private void updateOutboxStatus(OrderApprovalOutboxMessage orderApprovalOutboxMessage, OutboxStatus outboxStatus) {
        orderApprovalOutboxMessage.setOutboxStatus(outboxStatus);
        approvalOutboxHelper.save(orderApprovalOutboxMessage);
        log.info("OrderApprovalOutboxMessage 의 OutboxStatus 를 업데이트 했습니다. OrderApprovalOutboxMessage: {}", orderApprovalOutboxMessage.getId());
    }
}
