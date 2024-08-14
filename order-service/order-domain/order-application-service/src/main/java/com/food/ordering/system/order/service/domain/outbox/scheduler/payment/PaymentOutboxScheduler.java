package com.food.ordering.system.order.service.domain.outbox.scheduler.payment;

import com.food.ordering.system.order.service.domain.outbox.model.payment.OrderPaymentOutboxMessage;
import com.food.ordering.system.order.service.domain.ports.output.message.publisher.payment.PaymentRequestMessagePublisher;
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
public class PaymentOutboxScheduler implements OutboxScheduler {

    private final PaymentOutboxHelper paymentOutboxHelper;
    private final PaymentRequestMessagePublisher paymentRequestMessagePublisher;

    @Override
    @Transactional
    @Scheduled(fixedDelayString = "${order-service.outbox-scheduler-fixed-rate}",
            initialDelayString = "${order-service.outbox-scheduler-initial-delay}")
    public void processOutboxMessage() {
        // Order에서 Payment로 전달한 SagaStatus는 STARTED, COMPENSATING 뿐이다.
        Optional<List<OrderPaymentOutboxMessage>> outboxMessagesResponse =
                paymentOutboxHelper.getPaymentOutboxMessageByOutboxStatusAndSagaStatus(
                        OutboxStatus.STARTED,
                        SagaStatus.STARTED,
                        SagaStatus.COMPENSATING);

        outboxMessagesResponse
                .filter(outboxMessages -> !outboxMessages.isEmpty())
                .ifPresent(outboxMessages -> {
                    log.info("OrderPaymentOutbox 메시지를 {}개 수신했습니다. OrderPaymentOutboxMessage id{}",
                            outboxMessages.size(),
                            outboxMessages.stream()
                                    .map(outboxMessage ->
                                            outboxMessage.getId().toString())
                                    .collect(Collectors.joining(", ")));
                    outboxMessages.forEach(outboxMessage ->
                            paymentRequestMessagePublisher.publish(outboxMessage, this::updateOutboxStatus));
                    log.info("OrderPaymentOutbox 메시지를 {}개 발행했습니다.", outboxMessages.size());
                });
    }

    private void updateOutboxStatus(OrderPaymentOutboxMessage orderPaymentOutboxMessage, OutboxStatus outboxStatus) {
        orderPaymentOutboxMessage.setOutboxStatus(outboxStatus);
        paymentOutboxHelper.save(orderPaymentOutboxMessage);
        log.info("OrderPaymentOutboxMessage 의 OutboxStatus 를 업데이트 했습니다. OrderPaymentOutboxMessage: {}", orderPaymentOutboxMessage.getId());
    }
}
