package com.food.ordering.system.order.service.domain;

import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.entity.Product;
import com.food.ordering.system.order.service.domain.entity.Restaurant;
import com.food.ordering.system.order.service.domain.event.OrderCancelledEvent;
import com.food.ordering.system.order.service.domain.event.OrderCreatedEvent;
import com.food.ordering.system.order.service.domain.event.OrderPaidEvent;
import com.food.ordering.system.order.service.domain.exception.OrderDomainException;
import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Slf4j
public class OrderDomainServiceImpl implements OrderDomainService{

    private static final String UTC = "UTC";

    @Override
    public OrderCreatedEvent validateAndInitiateOrder(Order order, Restaurant restaurant) {
        validateRestaurant(restaurant);
        setOrderProductInformation(order, restaurant);
        order.validateOrder();
        order.initializeOrder();
        log.info("Order id: {} created successfully", order.getId().getValue());

        return new OrderCreatedEvent(order, ZonedDateTime.now(ZoneId.of(UTC)));
    }

    private void setOrderProductInformation(Order order, Restaurant restaurant) {
        // 시간복잡도 n * m (n: 주문 항목 수, m: 레스토랑 제품 수)
//        order.getItems().forEach(orderItem-> restaurant.getProducts().forEach(restaurantProduct -> {
//            Product currentProduct = orderItem.getProduct();
//            if(currentProduct.equals(restaurantProduct)){
//                currentProduct.updateWithConfirmedNameAndPrice(
//                        restaurantProduct.getName(),
//                        restaurantProduct.getPrice()
//                );
//            }
//        }));

        // 시간복잡도 n + m (n: 주문 항목 수, m: 레스토랑 제품 수) 개선
        // 레스토랑 제품을 해시 맵에 저장
        HashMap<UUID, Product> productMap = new HashMap<>();
        for (Product product : restaurant.getProducts()) {
            productMap.put(product.getId().getValue(), product);
        }

        // 주문 항목을 순회하며 제품 정보 업데이트
        order.getItems().forEach(orderItem -> {
            Product currentProduct = orderItem.getProduct();
            UUID productId = currentProduct.getId().getValue();
            Product restaurantProduct = productMap.get(productId);

            if (restaurantProduct != null) {
                currentProduct.updateWithConfirmedNameAndPrice(
                        restaurantProduct.getName(),
                        restaurantProduct.getPrice()
                );
            }
        });
    }

    private void validateRestaurant(Restaurant restaurant) {
        if(!restaurant.isActive()){
            throw new OrderDomainException("레스토랑: " + restaurant.getId().getValue() + "는 현재 주문을 받지 않습니다.");
        }
    }

    @Override
    public OrderPaidEvent payOrder(Order order, Restaurant restaurant) {
        order.pay();
        log.info("Order id: {} paid successfully", order.getId().getValue());
        return new OrderPaidEvent(order, ZonedDateTime.now(ZoneId.of(UTC)));
    }

    @Override
    public void approveOrder(Order order) {
        order.approved();
        log.info("Order id: {} approved successfully", order.getId().getValue());
    }

    @Override
    public OrderCancelledEvent cancelOrderPayment(Order order, List<String> failureMessages) {
        order.initCancel(failureMessages);
        log.info("Order id: {} start cancelling successfully", order.getId().getValue());
        return new OrderCancelledEvent(order, ZonedDateTime.now(ZoneId.of(UTC)));
    }

    @Override
    public void cancelOrder(Order order, List<String> failureMessages) {
        order.cancel(failureMessages);
        log.info("Order id: {} cancelled successfully", order.getId().getValue());
    }
}
