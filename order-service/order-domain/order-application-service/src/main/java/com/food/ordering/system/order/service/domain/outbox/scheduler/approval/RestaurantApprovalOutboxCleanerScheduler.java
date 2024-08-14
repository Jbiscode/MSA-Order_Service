package com.food.ordering.system.order.service.domain.outbox.scheduler.approval;

import com.food.ordering.system.order.service.domain.outbox.model.approval.OrderApprovalOutboxMessage;
import com.food.ordering.system.outbox.OutboxScheduler;
import com.food.ordering.system.outbox.OutboxStatus;
import com.food.ordering.system.saga.SagaStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class RestaurantApprovalOutboxCleanerScheduler implements OutboxScheduler {

    private final ApprovalOutboxHelper approvalOutboxHelper;

    @Override
    @Scheduled(cron = "@midnight")
    public void processOutboxMessage() {
        Optional<List<OrderApprovalOutboxMessage>> outboxMessagesResponse =
                approvalOutboxHelper.getApprovalOutboxMessageByOutboxStatusAndSagaStatus(
                        OutboxStatus.COMPLETED,
                        SagaStatus.SUCCEEDED,
                        SagaStatus.FAILED,
                        SagaStatus.COMPENSATED);

        outboxMessagesResponse
                .ifPresent(outboxMessages ->{
                    log.info("삭제할 OrderApprovalOutbox 메시지를 {}개 수신했습니다. 페이로드: {}",
                            outboxMessages.size(),
                            outboxMessages.stream().map(OrderApprovalOutboxMessage::getPayload)
                                    .collect(Collectors.joining("\n")));
                    approvalOutboxHelper.deleteApprovalOutboxMessageByOutboxStatusAndSagaStatus(
                            OutboxStatus.COMPLETED,
                            SagaStatus.SUCCEEDED,
                            SagaStatus.FAILED,
                            SagaStatus.COMPENSATED);
                    log.info("OrderApprovalOutbox 메시지를 {}개 삭제했습니다.", outboxMessages.size());
                });
    }
}
