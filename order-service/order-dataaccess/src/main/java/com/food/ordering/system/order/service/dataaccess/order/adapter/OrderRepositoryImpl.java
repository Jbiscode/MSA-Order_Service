package com.food.ordering.system.order.service.dataaccess.order.adapter;

import com.food.ordering.system.order.service.dataaccess.order.mapper.OrderDataAccessMapper;
import com.food.ordering.system.order.service.dataaccess.order.repository.OrderJpaRepository;
import com.food.ordering.system.order.service.domain.entity.Order;
import com.food.ordering.system.order.service.domain.ports.output.repository.OrderRepository;
import com.food.ordering.system.order.service.domain.valueobject.TrackingId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// 강의는 Component를 사용했지만, Repository를 사용하는 것이 더 적합하다고 판단하여 변경
@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;
    private final OrderDataAccessMapper orderDataAccessMapper;

    /**
     * 주어진 Order 엔티티를 데이터베이스에 저장합니다.
     *
     * 이 메서드는 Order 엔티티를 해당하는 OrderEntity로 변환하고,
     * OrderJpaRepository를 사용하여 저장한 후, 저장된 OrderEntity를 다시 Order 엔티티로 변환합니다.
     *
     * @param order 저장할 Order 엔티티
     * @return 저장된 Order 엔티티
     */
    @Override
    public Order save(Order order) {
        return orderDataAccessMapper.orderEntityToOrder(
                orderJpaRepository.save(
                        orderDataAccessMapper.orderToOrderEntity(order)
                )
        );
    }

    /**
     * 주어진 TrackingId를 사용하여 Order를 조회합니다.<br/><br/>
     *
     * 이 메서드는 TrackingId를 사용하여 OrderJpaRepository에서 OrderEntity를 조회하고,<br/>
     * 조회된 OrderEntity를 Order로 변환하여 반환합니다.
     *
     * @param trackingId 조회할 Order의 TrackingId
     * @return 조회된 Order를 Optional로 감싸서 반환, Order가 없으면 Optional.empty() 반환
     */
    @Override
    public Optional<Order> findByTrackingId(TrackingId trackingId) {
        return orderJpaRepository.findByTrackingId(trackingId.getValue())
                .map(orderDataAccessMapper::orderEntityToOrder);
    }
}
