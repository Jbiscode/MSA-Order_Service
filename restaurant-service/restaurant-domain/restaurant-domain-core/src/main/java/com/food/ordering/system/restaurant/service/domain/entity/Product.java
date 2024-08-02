package com.food.ordering.system.restaurant.service.domain.entity;

import com.food.ordering.system.domain.entity.BaseEntity;
import com.food.ordering.system.domain.valueobject.Money;
import com.food.ordering.system.domain.valueobject.ProductId;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class Product extends BaseEntity<ProductId> {
    private String name;
    private Money price;
    private final Integer quantity;
    private Boolean available;

    public void updateWithConfirmedNamePriceAndAvailability(String name, Money price, Boolean available) {
        this.name = name;
        this.price = price;
        this.available = available;
    }
}
