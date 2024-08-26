package com.food.ordering.system.restaurant.service.dataaccess.restaurant.outbox.mapper;

import com.food.ordering.system.restaurant.service.dataaccess.restaurant.outbox.entity.OrderOutboxEntity;
import com.food.ordering.system.restaurant.service.domain.outbox.model.OrderOutboxMessage;
import org.springframework.stereotype.Component;

@Component
public class OrderOutboxDataAccessMapper {

    public OrderOutboxEntity orderOutboxMessageToOutboxEntity(OrderOutboxMessage orderOutboxMessage) {
        return OrderOutboxEntity.builder()
                .id(orderOutboxMessage.getId())
                .sagaId(orderOutboxMessage.getSagaId())
                .createdAt(orderOutboxMessage.getCreatedAt())
                .type(orderOutboxMessage.getType())
                .payload(orderOutboxMessage.getPayload())
                .outboxStatus(orderOutboxMessage.getOutboxStatus())
                .approvalStatus(orderOutboxMessage.getApprovalStatus())
                .version(orderOutboxMessage.getVersion())
                .build();
    }

    public OrderOutboxMessage orderOutboxEntityToOutboxMessage(OrderOutboxEntity orderOutboxEntity) {
        return OrderOutboxMessage.builder()
                .id(orderOutboxEntity.getId())
                .sagaId(orderOutboxEntity.getSagaId())
                .createdAt(orderOutboxEntity.getCreatedAt())
                .type(orderOutboxEntity.getType())
                .payload(orderOutboxEntity.getPayload())
                .outboxStatus(orderOutboxEntity.getOutboxStatus())
                .approvalStatus(orderOutboxEntity.getApprovalStatus())
                .version(orderOutboxEntity.getVersion())
                .build();
    }
}
