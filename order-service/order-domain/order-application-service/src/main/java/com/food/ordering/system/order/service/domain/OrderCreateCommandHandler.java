package com.food.ordering.system.order.service.domain;

import com.food.ordering.system.order.service.domain.dto.create.CreateOrderCommand;
import com.food.ordering.system.order.service.domain.dto.create.CreateOrderResponse;
import com.food.ordering.system.order.service.domain.entity.Customer;
import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.entity.Restaurant;
import com.food.ordering.system.order.service.domain.event.OrderCreatedEvent;
import com.food.ordering.system.order.service.domain.exception.OrderDomainException;
import com.food.ordering.system.order.service.domain.mapper.OrderDataMapper;
import com.food.ordering.system.order.service.domain.ports.output.repository.CustomerRepository;
import com.food.ordering.system.order.service.domain.ports.output.repository.OrderRepository;
import com.food.ordering.system.order.service.domain.ports.output.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class OrderCreateCommandHandler {

    private final OrderDomainService orderDomainService;

    private final OrderRepository orderRepository;

    private final CustomerRepository customerRepository;

    private final RestaurantRepository restaurantRepository;

    private final OrderDataMapper orderDataMapper;

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderCommand createOrderCommand) {
        checkCustomer(createOrderCommand.getCustomerId());
        Restaurant restaurant = checkRestaurant(createOrderCommand);
        Order order = orderDataMapper.createOrderCommandToOrder(createOrderCommand);
        OrderCreatedEvent orderCreatedEvent = orderDomainService.validateAndInitiateOrder(order, restaurant);
        Order orderResult = saveOrder(order);
        log.info("주문이 생성되었습니다. orderId: {}", orderResult.getId().getValue());

        return orderDataMapper.orderToCreateOrderResponse(orderResult);
    }

    private Restaurant checkRestaurant(CreateOrderCommand createOrderCommand) {
        Restaurant restaurant = orderDataMapper.createOrderCommandToRestaurant(createOrderCommand);
        Optional<Restaurant> optionalRestaurant = restaurantRepository.findRestaurantInformation(restaurant);
        if(optionalRestaurant.isEmpty()) {
            log.warn("식당을 찾을 수 없습니다. restaurantId: {}", createOrderCommand.getRestaurantId());
            throw new OrderDomainException("식당을 찾을 수 없습니다. restaurantId: " + createOrderCommand.getRestaurantId());
        }

        return optionalRestaurant.get();
    }

    private void checkCustomer(UUID customerId) {
        Optional<Customer> customer = customerRepository.findCustomer(customerId);
        if (customer.isEmpty()) {
            log.warn("고객을 찾을 수 없습니다. customerId: {}", customerId);
            throw new OrderDomainException("고객을 찾을 수 없습니다. customerId: " + customerId);
        }
    }

    private Order saveOrder(Order order) {
        Order orderResult =  orderRepository.save(order);
        if(orderResult == null){
            log.error("주문을 저장할 수 없습니다.");
            throw new OrderDomainException("주문을 저장할 수 없습니다.");
        }

        log.info("주문이 저장되었습니다. orderId: {}", orderResult.getId().getValue());
        return orderResult;
    }
}
