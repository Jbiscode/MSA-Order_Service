package com.food.ordering.system.restaurant.service.domain;

import com.food.ordering.system.restaurant.service.domain.entity.Restaurant;
import com.food.ordering.system.restaurant.service.domain.event.OrderApprovalEvent;

import java.util.List;

public interface RestaurantDomainService {

    // OrderApprovalEvent는 OrderApprovedEvent와 OrderRejectedEvent를 리턴할수있어서 OrderApprovalEvent를 반환한다.
    OrderApprovalEvent validateOrder(Restaurant restaurant,
                                     List<String> failureMessages);
}
