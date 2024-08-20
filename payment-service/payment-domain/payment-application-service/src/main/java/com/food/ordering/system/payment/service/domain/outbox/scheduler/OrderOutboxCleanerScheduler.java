package com.food.ordering.system.payment.service.domain.outbox.scheduler;

import com.food.ordering.system.outbox.OutboxScheduler;
import com.food.ordering.system.outbox.OutboxStatus;
import com.food.ordering.system.payment.service.domain.outbox.model.OrderOutboxMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class OrderOutboxCleanerScheduler implements OutboxScheduler {

    private  final OrderOutboxHelper orderOutboxHelper;

    @Override
    @Transactional
    @Scheduled(cron = "midnight", zone = "Asia/Seoul")
    public void processOutboxMessage() {
        orderOutboxHelper.getOrderOutboxMessageByOutboxStatus(OutboxStatus.COMPLETED)
                .ifPresent(outboxMessages -> {
                    log.info("삭제할 OrderOutbox 메시지를 {}개 수신했습니다. OrderOutboxMessage 페이로드{}",
                            outboxMessages.size(),
                            outboxMessages.stream()
                                    .map(OrderOutboxMessage::getPayload)
                                    .collect(Collectors.joining(", ")));
                    orderOutboxHelper.deleteOrderOutboxMessageByOutboxStatus(OutboxStatus.COMPLETED);
                    log.info("OrderOutbox 메시지를 {}개 삭제했습니다.", outboxMessages.size());
                });
    }
}
