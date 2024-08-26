package com.food.ordering.system.restaurant.service.dataaccess.restaurant.outbox.adapter;

import com.food.ordering.system.outbox.OutboxStatus;
import com.food.ordering.system.restaurant.service.dataaccess.restaurant.outbox.exception.OrderOutboxNotFoundException;
import com.food.ordering.system.restaurant.service.dataaccess.restaurant.outbox.mapper.OrderOutboxDataAccessMapper;
import com.food.ordering.system.restaurant.service.dataaccess.restaurant.outbox.repository.OrderOutboxJpaRepository;
import com.food.ordering.system.restaurant.service.domain.outbox.model.OrderOutboxMessage;
import com.food.ordering.system.restaurant.service.domain.ports.output.repository.OrderOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderOutboxRepositoryImpl implements OrderOutboxRepository {

    private final OrderOutboxDataAccessMapper orderOutboxDataAccessMapper;
    private final OrderOutboxJpaRepository orderOutboxJpaRepository;

    @Override
    public Optional<OrderOutboxMessage> save(OrderOutboxMessage orderOutboxMessage) {
        return Optional.ofNullable(
                orderOutboxDataAccessMapper.orderOutboxEntityToOutboxMessage(
                        orderOutboxJpaRepository.save(
                                orderOutboxDataAccessMapper.orderOutboxMessageToOutboxEntity(orderOutboxMessage)
                        )
                )
        );
    }

    @Override
    public Optional<List<OrderOutboxMessage>> findByTypeAndOutboxStatus(String type, OutboxStatus outboxStatus) {
        return Optional.of(
                orderOutboxJpaRepository.findByTypeAndOutboxStatus(type, outboxStatus)
                        .orElseThrow(()-> new OrderOutboxNotFoundException("Approval Order outbox not found of type:" + type))
                        .stream()
                        .map(orderOutboxDataAccessMapper::orderOutboxEntityToOutboxMessage)
                        .collect(Collectors.toList())
        );
    }

    @Override
    public Optional<OrderOutboxMessage> findByTypeAndSagaIdAndOutboxStatus(String type, UUID sagaId, OutboxStatus outboxStatus) {
        return orderOutboxJpaRepository.findByTypeAndSagaIdAndOutboxStatus(type, sagaId, outboxStatus)
                .map(orderOutboxDataAccessMapper::orderOutboxEntityToOutboxMessage);
    }

    @Override
    public void deleteByTypeAndOutboxStatus(String type, OutboxStatus outboxStatus) {
        orderOutboxJpaRepository.deleteByTypeAndOutboxStatus(type, outboxStatus);
    }
}
