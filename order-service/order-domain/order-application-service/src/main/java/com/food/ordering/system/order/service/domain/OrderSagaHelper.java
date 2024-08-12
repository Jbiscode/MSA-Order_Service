package com.food.ordering.system.order.service.domain;

import com.food.ordering.system.domain.valueobject.OrderId;
import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.exception.OrderNotFoundException;
import com.food.ordering.system.order.service.domain.ports.output.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class OrderSagaHelper {

    private final OrderRepository orderRepository;

    public Order findOrder(String orderId) {
        return orderRepository.findById(new OrderId(UUID.fromString(orderId)))
                .orElseThrow(
                        () -> {
                            log.error("주문을 찾을 수 없습니다: {}", orderId);
                            return new OrderNotFoundException(String.format("주문을 찾을 수 없습니다: %s", orderId));
                        });
    }

    void saveOrder(Order order) {
        orderRepository.save(order);
    }
}
