package com.food.ordering.system.payment.service.dataaccess.outbox.mapper;

import com.food.ordering.system.payment.service.dataaccess.outbox.entity.OrderOutboxEntity;
import com.food.ordering.system.payment.service.domain.outbox.model.OrderOutboxMessage;
import org.springframework.stereotype.Component;

@Component
public class OrderOutboxDataAccessMapper {
    public OrderOutboxEntity orderOutboxMessageToOrderOutboxEntity(OrderOutboxMessage orderOutboxMessage) {
        return OrderOutboxEntity.builder()
                .id(orderOutboxMessage.getId())
                .sagaId(orderOutboxMessage.getSagaId())
                .type(orderOutboxMessage.getType())
                .payload(orderOutboxMessage.getPayload())
                .paymentStatus(orderOutboxMessage.getPaymentStatus())
                .outboxStatus(orderOutboxMessage.getOutboxStatus())
                .createdAt(orderOutboxMessage.getCreatedAt())
                .version(orderOutboxMessage.getVersion())
                .build();
    }

    public OrderOutboxMessage orderOutboxEntityToOrderOutboxMessage(OrderOutboxEntity entity) {
        return OrderOutboxMessage.builder()
                .id(entity.getId())
                .sagaId(entity.getSagaId())
                .type(entity.getType())
                .payload(entity.getPayload())
                .paymentStatus(entity.getPaymentStatus())
                .outboxStatus(entity.getOutboxStatus())
                .createdAt(entity.getCreatedAt())
                .version(entity.getVersion())
                .build();
    }
}
