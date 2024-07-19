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

/**
 * 주문 생성 과정을 담당하는 핸들러 클래스입니다.
 * 주문 생성 요청을 받아 도메인 로직을 실행하고, 결과를 반환합니다.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class OrderCreateCommandHandler {

    private final OrderDomainService orderDomainService;

    private final OrderRepository orderRepository;

    private final CustomerRepository customerRepository;

    private final RestaurantRepository restaurantRepository;

    private final OrderDataMapper orderDataMapper;

    /**
     * 주문 생성 요청을 처리하고 주문 생성 결과를 반환합니다.
     *
     * @param createOrderCommand 주문 생성에 필요한 정보를 담고 있는 커맨드 객체입니다.
     * @return 생성된 주문의 응답 객체입니다.
     * @throws OrderDomainException 고객이나 식당 정보를 찾을 수 없는 경우 예외를 발생시킵니다.
     */
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

    /**
     * 주문 생성 시 제공된 식당 ID를 기반으로 식당 정보를 검증합니다.
     *
     * @param createOrderCommand 주문 생성에 필요한 정보를 담고 있는 커맨드 객체입니다.
     * @return 검증된 식당 엔티티입니다.
     * @throws OrderDomainException 식당 정보를 찾을 수 없는 경우 예외를 발생시킵니다.
     */
    private Restaurant checkRestaurant(CreateOrderCommand createOrderCommand) {
        Restaurant restaurant = orderDataMapper.createOrderCommandToRestaurant(createOrderCommand);
        Optional<Restaurant> optionalRestaurant = restaurantRepository.findRestaurantInformation(restaurant);
        if(optionalRestaurant.isEmpty()) {
            log.warn("식당을 찾을 수 없습니다. restaurantId: {}", createOrderCommand.getRestaurantId());
            throw new OrderDomainException("식당을 찾을 수 없습니다. restaurantId: " + createOrderCommand.getRestaurantId());
        }

        return optionalRestaurant.get();
    }

    /**
     * 주문 생성 시 제공된 고객 ID를 기반으로 고객 정보를 검증합니다.
     *
     * @param customerId 주문을 생성하는 고객의 ID입니다.
     * @throws OrderDomainException 고객 정보를 찾을 수 없는 경우 예외를 발생시킵니다.
     */
    private void checkCustomer(UUID customerId) {
        Optional<Customer> customer = customerRepository.findCustomer(customerId);
        if (customer.isEmpty()) {
            log.warn("고객을 찾을 수 없습니다. customerId: {}", customerId);
            throw new OrderDomainException("고객을 찾을 수 없습니다. customerId: " + customerId);
        }
    }

    /**
     * 주문 엔티티를 저장하고 저장된 주문 엔티티를 반환합니다.
     *
     * @param order 저장할 주문 엔티티입니다.
     * @return 저장된 주문 엔티티입니다.
     * @throws OrderDomainException 주문을 저장할 수 없는 경우 예외를 발생시킵니다.
     */
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
