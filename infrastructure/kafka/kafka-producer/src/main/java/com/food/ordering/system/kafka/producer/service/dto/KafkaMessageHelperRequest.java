package com.food.ordering.system.kafka.producer.service.dto;

import com.food.ordering.system.outbox.OutboxStatus;

import java.util.function.BiConsumer;

public record KafkaMessageHelperRequest<U>(
        U outboxMessage,
        BiConsumer<U, OutboxStatus> outboxCallback,
        String orderId,
        String avroModelName) {
}
