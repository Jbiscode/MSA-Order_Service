package com.food.ordering.system.order.service.domain;

import com.food.ordering.system.order.service.domain.dto.track.TrackOrderQuery;
import com.food.ordering.system.order.service.domain.dto.track.TrackOrderResponse;
import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.exception.OrderNotFoundException;
import com.food.ordering.system.order.service.domain.mapper.OrderDataMapper;
import com.food.ordering.system.order.service.domain.ports.output.repository.OrderRepository;
import com.food.ordering.system.order.service.domain.valueobject.TrackingId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTrackCommandHandler {

    private final OrderDataMapper orderDataMapper;

    private final OrderRepository orderRepository;

    /**
     * 주문 조회 요청을 처리하고 주문 조회 결과를 반환합니다.
     *
     * @param orderTrackingId 주문 조회에 필요한 정보를 담고 있는 쿼리 객체입니다.
     * @return 조회된 주문의 응답 객체입니다.
     */
    @Transactional(readOnly = true)
    public TrackOrderResponse trackOrder(TrackOrderQuery orderTrackingId) {
        Optional<Order> orderResult = orderRepository.findByTrackingId(new TrackingId(orderTrackingId.getOrderTrackingId()));
        if (orderResult.isEmpty()){
            log.info("주문이 존재하지 않습니다. orderTrackingId: {}", orderTrackingId.getOrderTrackingId());
            throw new OrderNotFoundException("주문이 존재하지 않습니다. tracking id: " + orderTrackingId.getOrderTrackingId());
        }
        return orderDataMapper.orderToTrackOrderResponse(orderResult.get());
    }
}
