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

/**
 * 주문 도메인 서비스의 구현체입니다.
 * 주문 생성, 결제, 승인 및 취소와 같은 주문 관련 비즈니스 로직을 처리합니다.
 */
@Slf4j
public class OrderDomainServiceImpl implements OrderDomainService{

    private static final String ASIA_SEOUL = "ASIA/SEOUL";

    /**
     * 주문과 레스토랑 정보를 검증하고 주문을 초기화합니다.
     *
     * @param order 주문 엔티티입니다.
     * @param restaurant 레스토랑 엔티티입니다.
     * @return 주문 생성 이벤트를 반환합니다.
     * @throws OrderDomainException 레스토랑이 비활성화 상태일 경우 예외를 발생시킵니다.
     */
    @Override
    public OrderCreatedEvent validateAndInitiateOrder(Order order, Restaurant restaurant) {
        validateRestaurant(restaurant);
        setOrderProductInformation(order, restaurant);
        order.validateOrder();
        order.initializeOrder();
        log.info("Order id: {} created successfully", order.getId().getValue());

        return new OrderCreatedEvent(order, ZonedDateTime.now(ZoneId.of(ASIA_SEOUL)));
    }

    /**
     * 주문 항목에 해당하는 레스토랑의 제품 정보를 설정합니다.
     *
     * @param order 주문 엔티티입니다.
     * @param restaurant 레스토랑 엔티티입니다.
     */
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

    /**
     * 레스토랑이 주문을 받을 수 있는 상태인지 검증합니다.
     *
     * @param restaurant 레스토랑 엔티티입니다.
     * @throws OrderDomainException 레스토랑이 비활성화 상태일 경우 예외를 발생시킵니다.
     */
    private void validateRestaurant(Restaurant restaurant) {
        if(!restaurant.isActive()){
            throw new OrderDomainException("레스토랑: " + restaurant.getId().getValue() + "는 현재 주문을 받지 않습니다.");
        }
    }

    /**
     * 주문을 결제 처리합니다.
     *
     * @param order 주문 엔티티입니다.
     * @param restaurant 레스토랑 엔티티입니다.
     * @return 주문 결제 이벤트를 반환합니다.
     */
    @Override
    public OrderPaidEvent payOrder(Order order, Restaurant restaurant) {
        order.pay();
        log.info("Order id: {} paid successfully", order.getId().getValue());
        return new OrderPaidEvent(order, ZonedDateTime.now(ZoneId.of(ASIA_SEOUL)));
    }

    /**
     * 주문을 승인합니다.
     *
     * @param order 주문 엔티티
     */
    @Override
    public void approveOrder(Order order) {
        order.approved();
        log.info("Order id: {} approved successfully", order.getId().getValue());
    }

    /**
     * 주문 결제를 취소합니다.
     *
     * @param order 주문 엔티티
     * @param failureMessages 실패 메시지 목록
     * @return 주문 결제 취소 이벤트
     */
    @Override
    public OrderCancelledEvent cancelOrderPayment(Order order, List<String> failureMessages) {
        order.initCancel(failureMessages);
        log.info("Order id: {} start cancelling successfully", order.getId().getValue());
        return new OrderCancelledEvent(order, ZonedDateTime.now(ZoneId.of(ASIA_SEOUL)));
    }

    /**
     * 주문을 취소합니다.
     *
     * @param order 주문 엔티티
     * @param failureMessages 실패 메시지 목록
     */
    @Override
    public void cancelOrder(Order order, List<String> failureMessages) {
        order.cancel(failureMessages);
        log.info("Order id: {} cancelled successfully", order.getId().getValue());
    }
}
