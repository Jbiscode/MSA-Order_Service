package com.food.ordering.system.restaurant.service.domain.entity;

import com.food.ordering.system.domain.entity.AggregateRoot;
import com.food.ordering.system.domain.valueobject.Money;
import com.food.ordering.system.domain.valueobject.OrderApprovalStatus;
import com.food.ordering.system.domain.valueobject.OrderStatus;
import com.food.ordering.system.domain.valueobject.RestaurantId;
import com.food.ordering.system.restaurant.service.domain.valueobject.OrderApprovalId;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.UUID;

@Getter
@SuperBuilder
public class Restaurant extends AggregateRoot<RestaurantId> {
    private OrderApproval orderApproval;
    @Setter
    private Boolean active;
    private final OrderDetail orderDetail;

    public void validateOrder(List<String> failureMessages){
        if(orderDetail.getOrderStatus() != OrderStatus.PAID){
            failureMessages.add(String.format("orderId: %s 결제가 완료되지 않았습니다.", orderDetail.getId().getValue()));
        }

        Money totalAmount = orderDetail.getProducts().stream().map(product -> {
            if(!product.getAvailable()){
                failureMessages.add(String.format("productId: %s 상품이 현재 구매불가입니다.", product.getId().getValue()));
            }
            return product.getPrice().multiply(product.getQuantity()); // 가격과 수량을 곱한 값을 반환
        }).reduce(Money.ZERO, Money::add);

        if(!totalAmount.equals(orderDetail.getTotalAmount())){
            failureMessages.add(String.format("orderId: %s 결제금액이 일치하지 않습니다.", orderDetail.getId().getValue()));
        }
    }

    public void constructOrderApproval(OrderApprovalStatus orderApprovalStatus){
        this.orderApproval = OrderApproval.builder()
                .id(new OrderApprovalId(UUID.randomUUID()))
                .restaurantId(this.getId())
                .orderId(orderDetail.getId())
                .approvalStatus(orderApprovalStatus)
                .build();
    }

}
