package com.food.ordering.system.kafka.producer.service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.ordering.system.kafka.producer.service.dto.KafkaMessageHelperRequest;
import com.food.ordering.system.order.service.domain.exception.OrderDomainException;
import com.food.ordering.system.outbox.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;

@Slf4j
@RequiredArgsConstructor
@Component
public class KafkaMessageHelper {

    private final ObjectMapper objectMapper;

    public <U> KafkaMessageHelperRequest<U> createKafkaMessageHelperRequest(U outboxMessage,
                                                                         BiConsumer<U, OutboxStatus> outboxCallback,
                                                                         String orderId,
                                                                         String avroModelName) {
        return new KafkaMessageHelperRequest<>(outboxMessage, outboxCallback, orderId, avroModelName);
    }

    public <T> T getOrderEventPayload(String payload, Class<T> returnType) {
        try {
            return objectMapper.readValue(payload, returnType);
        } catch (JsonProcessingException e) {
            log.error("payload 파싱을 실패했습니다.: {}", e.getMessage());
            throw new OrderDomainException(returnType.getName()+" payload 파싱을 실패했습니다.");
        }
    }
}
