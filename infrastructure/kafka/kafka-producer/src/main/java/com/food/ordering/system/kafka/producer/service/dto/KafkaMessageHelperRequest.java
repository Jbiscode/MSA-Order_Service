package com.food.ordering.system.kafka.producer.service.dto;

public record KafkaMessageHelperRequest(
        String orderId,
        String avroModelName) {
}
