package com.food.ordering.system.order.service.domain.mapper;

import com.food.ordering.system.domain.valueobject.CustomerId;
import com.food.ordering.system.domain.valueobject.Money;
import com.food.ordering.system.domain.valueobject.ProductId;
import com.food.ordering.system.domain.valueobject.RestaurantId;
import com.food.ordering.system.order.service.domain.dto.create.CreateOrderCommand;
import com.food.ordering.system.order.service.domain.dto.create.CreateOrderResponse;
import com.food.ordering.system.order.service.domain.dto.create.OrderAddress;
import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.entity.OrderItem;
import com.food.ordering.system.order.service.domain.entity.Product;
import com.food.ordering.system.order.service.domain.entity.Restaurant;
import com.food.ordering.system.order.service.domain.valueobject.StreetAddress;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 주문 관련 도메인 엔티티와 DTO 간의 변환을 담당하는 매퍼 클래스입니다.
 */
@Component
public class OrderDataMapper {

    /**
     * CreateOrderCommand 객체를 Restaurant 엔티티로 변환합니다.
     *
     * @param createOrderCommand 주문 생성 요청 정보를 담고 있는 커맨드 객체입니다.
     * @return 지정된 식당 ID와 제품 목록을 포함하는 Restaurant 엔티티를 반환합니다.
     */
    public Restaurant createOrderCommandToRestaurant(CreateOrderCommand createOrderCommand) {
        return Restaurant.builder()
                .restaurantId(new RestaurantId(createOrderCommand.getRestaurantId()))
                .products(createOrderCommand.getItems().stream().map( orderItem ->
                        new Product(new ProductId(orderItem.getProductId())))
                        .collect(Collectors.toList())
                )
                .build();
    }

    /**
     * CreateOrderCommand 객체를 Order 엔티티로 변환합니다.
     *
     * @param createOrderCommand 주문 생성 요청 정보를 담고 있는 커맨드 객체입니다.
     * @return 고객 ID, 식당 ID, 배송 주소, 가격, 항목을 포함하는 Order 엔티티를 반환합니다.
     */
    public Order createOrderCommandToOrder(CreateOrderCommand createOrderCommand) {
        return Order.builder()
                .customerId(new CustomerId(createOrderCommand.getCustomerId()))
                .restaurantId(new RestaurantId(createOrderCommand.getRestaurantId()))
                .deliveryAddress(orderAddressToStreetAddress(createOrderCommand.getAddress()))
                .price(new Money(createOrderCommand.getPrice()))
                .items(orderItemToOrderEntities(createOrderCommand.getItems()))
                .build();
    }

    /**
     * Order 엔티티를 CreateOrderResponse DTO로 변환합니다.
     *
     * @param order 변환할 Order 엔티티입니다.
     * @return 주문 추적 ID와 주문 상태를 포함하는 CreateOrderResponse DTO를 반환합니다.
     */
    public CreateOrderResponse orderToCreateOrderResponse(Order order) {
        return CreateOrderResponse.builder()
                .orderTrackingId(order.getTrackingId().getValue())
                .orderStatus(order.getOrderStatus())
                .build();
    }

    /**
     * OrderItem DTO 목록을 OrderItem 엔티티 목록으로 변환합니다.
     *
     * @param orderItems 변환할 OrderItem DTO 목록입니다.
     * @return OrderItem 엔티티 목록을 반환합니다.
     */
    private List<OrderItem> orderItemToOrderEntities(List<com.food.ordering.system.order.service.domain.dto.create.OrderItem> orderItems) {
        return orderItems.stream()
                .map(orderItem ->
                        OrderItem.builder()
                                .product(new Product(new ProductId(orderItem.getProductId())))
                                .price(new Money(orderItem.getPrice()))
                                .quantity(orderItem.getQuantity())
                                .subTotal(new Money(orderItem.getSubTotal()))
                                .build()
                )
                .collect(Collectors.toList());
    }

    /**
     * OrderAddress DTO를 StreetAddress 값 객체로 변환합니다.
     *
     * @param orderAddress 변환할 OrderAddress DTO입니다.
     * @return StreetAddress 값 객체를 반환합니다.
     */
    private StreetAddress orderAddressToStreetAddress(OrderAddress orderAddress) {
        return  new StreetAddress(
                UUID.randomUUID(),
                orderAddress.getStreet(),
                orderAddress.getPostalCode(),
                orderAddress.getCity()
        );
    }
}