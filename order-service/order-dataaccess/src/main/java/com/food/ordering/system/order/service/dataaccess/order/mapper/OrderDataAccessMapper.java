package com.food.ordering.system.order.service.dataaccess.order.mapper;

import com.food.ordering.system.domain.valueobject.CustomerId;
import com.food.ordering.system.domain.valueobject.Money;
import com.food.ordering.system.domain.valueobject.OrderId;
import com.food.ordering.system.domain.valueobject.ProductId;
import com.food.ordering.system.domain.valueobject.RestaurantId;
import com.food.ordering.system.order.service.dataaccess.order.entity.OrderAddressEntity;
import com.food.ordering.system.order.service.dataaccess.order.entity.OrderEntity;
import com.food.ordering.system.order.service.dataaccess.order.entity.OrderItemEntity;
import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.entity.OrderItem;
import com.food.ordering.system.order.service.domain.entity.Product;
import com.food.ordering.system.order.service.domain.valueobject.OrderItemId;
import com.food.ordering.system.order.service.domain.valueobject.StreetAddress;
import com.food.ordering.system.order.service.domain.valueobject.TrackingId;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.food.ordering.system.order.service.domain.entity.Order.FAILURE_MESSAGE_DELIMITER;

/**
 * OrderDataAccessMapper 클래스는 도메인 엔티티와 데이터베이스 엔티티 간의 매핑을 담당합니다.
 */
@Component
public class OrderDataAccessMapper {

    /**
     * 도메인 Order 객체를 데이터베이스 OrderEntity 객체로 변환합니다.
     *
     * @param order 도메인 Order 객체
     * @return 데이터베이스 OrderEntity 객체
     */
    public OrderEntity orderToOrderEntity(Order order){
        OrderEntity orderEntity = OrderEntity.builder()
                .id(order.getId().getValue())
                .customerId(order.getCustomerId().getValue())
                .restaurantId(order.getRestaurantId().getValue())
                .trackingId(order.getTrackingId().getValue())
                .address(deliveryAddressToAddressEntity(
                                order.getDeliveryAddress()
                        ))
                .orderStatus(order.getOrderStatus())
                .price(order.getPrice().getAmount())
                .items(orderItemsToOrderItemEntities(order.getItems()))
                .orderStatus(order.getOrderStatus())
                .failureMessages(order.getFailureMessages() != null
                                ? String.join(FAILURE_MESSAGE_DELIMITER, order.getFailureMessages())
                                : "")
                .build();

        orderEntity.getAddress().setOrder(orderEntity);
        orderEntity.getItems().forEach(item -> item.setOrder(orderEntity));

        return orderEntity;
    }

    /**
     * 데이터베이스 OrderEntity 객체를 도메인 Order 객체로 변환합니다.
     *
     * @param orderEntity 데이터베이스 OrderEntity 객체
     * @return 도메인 Order 객체
     */
    public Order orderEntityToOrder(OrderEntity orderEntity){
        return Order.builder()
                .orderId(new OrderId(orderEntity.getId()))
                .customerId(new CustomerId(orderEntity.getCustomerId()))
                .restaurantId(new RestaurantId(orderEntity.getRestaurantId()))
                .deliveryAddress(addressEntityToDeliveryAddress(orderEntity.getAddress()))
                .price(new Money(orderEntity.getPrice()))
                .items(orderItemEntitiesToOrderItems(orderEntity.getItems()))
                .trackingId(new TrackingId(orderEntity.getTrackingId()))
                .orderStatus(orderEntity.getOrderStatus())
                .failureMessages(orderEntity.getFailureMessages() != null
                        ? new ArrayList<>(Arrays.asList(orderEntity.getFailureMessages().split(FAILURE_MESSAGE_DELIMITER)))
                        : new ArrayList<>())
                .build();
    }

    /**
     * OrderItemEntity 객체 목록을 도메인 OrderItem 객체 목록으로 변환합니다.
     *
     * @param items OrderItemEntity 객체 목록
     * @return 도메인 OrderItem 객체 목록
     */
    private List<OrderItem> orderItemEntitiesToOrderItems(List<OrderItemEntity> items) {
        return items.stream()
                .map(item -> OrderItem.builder()
                        .orderItemId(new OrderItemId(item.getId()))
                        .product(new Product(new ProductId(item.getProductId())))
                        .price(new Money(item.getPrice()))
                        .quantity(item.getQuantity())
                        .subTotal(new Money(item.getSubTotal()))
                        .build())
                .toList();
    }

    /**
     * OrderAddressEntity 객체를 도메인 StreetAddress 객체로 변환합니다.
     *
     * @param address OrderAddressEntity 객체
     * @return 도메인 StreetAddress 객체
     */
    private StreetAddress addressEntityToDeliveryAddress(OrderAddressEntity address) {
        return new StreetAddress(
                address.getId(),
                address.getStreet(),
                address.getPostalCode(),
                address.getCity());
    }

    /**
     * 도메인 OrderItem 객체 목록을 OrderItemEntity 객체 목록으로 변환합니다.
     *
     * @param items 도메인 OrderItem 객체 목록
     * @return OrderItemEntity 객체 목록
     */
    private List<OrderItemEntity> orderItemsToOrderItemEntities(List<OrderItem> items) {
        return items.stream()
                .map(item -> OrderItemEntity.builder()
                        .id(item.getId().getValue())
                        .productId(item.getProduct().getId().getValue())
                        .price(item.getPrice().getAmount())
                        .quantity(item.getQuantity())
                        .subTotal(item.getSubTotal().getAmount())
                        .build())
                .toList();

    }

    /**
     * 도메인 StreetAddress 객체를 OrderAddressEntity 객체로 변환합니다.
     *
     * @param deliveryAddress 도메인 StreetAddress 객체
     * @return OrderAddressEntity 객체
     */
    private OrderAddressEntity deliveryAddressToAddressEntity(StreetAddress deliveryAddress){
        return OrderAddressEntity.builder()
                .id(deliveryAddress.getId())
                .street(deliveryAddress.getStreet())
                .postalCode(deliveryAddress.getPostalCode())
                .city(deliveryAddress.getCity())
                .build();
    }
}
