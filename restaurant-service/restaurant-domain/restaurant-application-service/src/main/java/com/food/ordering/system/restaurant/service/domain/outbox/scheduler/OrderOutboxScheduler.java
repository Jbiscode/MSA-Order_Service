package com.food.ordering.system.restaurant.service.domain.outbox.scheduler;

import com.food.ordering.system.outbox.OutboxScheduler;
import com.food.ordering.system.outbox.OutboxStatus;
import com.food.ordering.system.restaurant.service.domain.ports.output.message.publisher.RestaurantApprovalResponseMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class OrderOutboxScheduler implements OutboxScheduler {

    private final OrderOutboxHelper orderOutboxHelper;
    private final RestaurantApprovalResponseMessagePublisher restaurantApprovalResponseMessagePublisher;

    @Transactional
    @Scheduled(fixedRateString = "${restaurant-service.outbox-scheduler-fixed-rate}",
            initialDelayString = "${restaurant-service.outbox-scheduler-initial-delay}")
    @Override
    public void processOutboxMessage() {
        orderOutboxHelper.getOrderOutboxMessageByOutboxStatus(OutboxStatus.STARTED)
                .filter(outboxMessages -> !outboxMessages.isEmpty())
                .ifPresent(outboxMessages -> {
                    log.info("OrderOutbox 메시지를 {}개 수신했습니다. OrderOutboxMessage id{}",
                            outboxMessages.size(),
                            outboxMessages.stream()
                                    .map(outboxMessage ->
                                            outboxMessage.getId().toString())
                                    .collect(Collectors.joining(", ")));
                    outboxMessages.forEach(outboxMessage ->
                            restaurantApprovalResponseMessagePublisher.publish(
                                    outboxMessage,
                                    orderOutboxHelper::updateOutboxStatus));
                    log.info("OrderOutbox 메시지를 {}개 발행했습니다.", outboxMessages.size());
                });
    }
}
