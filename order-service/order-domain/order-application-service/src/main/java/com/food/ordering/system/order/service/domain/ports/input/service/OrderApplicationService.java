package com.food.ordering.system.order.service.domain.ports.input.service;

import com.food.ordering.system.order.service.domain.dto.create.CreateOrderCommand;
import com.food.ordering.system.order.service.domain.dto.create.CreateOrderResponse;
import com.food.ordering.system.order.service.domain.dto.track.TrackOrderQuery;
import com.food.ordering.system.order.service.domain.dto.track.TrackOrderResponse;
import jakarta.validation.Valid;

/**
 * 어플리케이션의 클라이언트가 사용할 인터페이스
 * 내가 주문을 시작하기 위해 우편 배달부처럼 사용
 */
public interface OrderApplicationService {

    // 구현에서 @Valid 를 사용하면 안되고 여기서 사용해야 한다.
    CreateOrderResponse createOrder(@Valid CreateOrderCommand createOrderCommand);

    TrackOrderResponse trackOrder(@Valid TrackOrderQuery orderTrackingId);
}
