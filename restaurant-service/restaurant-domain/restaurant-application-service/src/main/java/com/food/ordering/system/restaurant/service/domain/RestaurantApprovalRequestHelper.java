package com.food.ordering.system.restaurant.service.domain;

import com.food.ordering.system.domain.valueobject.OrderId;
import com.food.ordering.system.outbox.OutboxStatus;
import com.food.ordering.system.restaurant.service.domain.dto.RestaurantApprovalRequest;
import com.food.ordering.system.restaurant.service.domain.entity.Restaurant;
import com.food.ordering.system.restaurant.service.domain.event.OrderApprovalEvent;
import com.food.ordering.system.restaurant.service.domain.exception.RestaurantNotFoundException;
import com.food.ordering.system.restaurant.service.domain.mapper.RestaurantDataMapper;
import com.food.ordering.system.restaurant.service.domain.outbox.scheduler.OrderOutboxHelper;
import com.food.ordering.system.restaurant.service.domain.ports.output.message.publisher.OrderApprovedMessagePublisher;
import com.food.ordering.system.restaurant.service.domain.ports.output.message.publisher.OrderRejectedMessagePublisher;
import com.food.ordering.system.restaurant.service.domain.ports.output.message.publisher.RestaurantApprovalResponseMessagePublisher;
import com.food.ordering.system.restaurant.service.domain.ports.output.repository.OrderApprovalRepository;
import com.food.ordering.system.restaurant.service.domain.ports.output.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class RestaurantApprovalRequestHelper {

    private final RestaurantDomainService restaurantDomainService;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantDataMapper restaurantDataMapper;
    private final OrderApprovalRepository orderApprovalRepository;

    private final OrderOutboxHelper orderOutboxHelper;
    private final RestaurantApprovalResponseMessagePublisher restaurantApprovalResponseMessagePublisher;

    @Transactional
    public void persistOrderApproval(RestaurantApprovalRequest restaurantApprovalRequest) {
        if (publishIfOutboxMessageProcessed(restaurantApprovalRequest)) {
            log.info("주문 승인이 이미 처리되었습니다. persistOrderApproval=> sagaId: {}", restaurantApprovalRequest.getSagaId());
            return;
        }
        log.info("주문 승인이 생성되었습니다.persistOrderApproval=> orderId: {}", restaurantApprovalRequest.getOrderId());
        Restaurant restaurant = findRestaurant(restaurantApprovalRequest);
        OrderApprovalEvent orderApprovalEvent =
                restaurantDomainService.validateOrder(
                        restaurant,
                        new ArrayList<>());

        orderApprovalRepository.save(orderApprovalEvent.getOrderApproval());

        orderOutboxHelper
                .saveOrderOutboxMessage(
                        restaurantDataMapper.orderApprovalEventToOrderOutboxMessage(orderApprovalEvent),
                        orderApprovalEvent.getOrderApproval().getApprovalStatus(),
                        OutboxStatus.STARTED,
                        UUID.fromString(restaurantApprovalRequest.getSagaId()));
    }

    private Restaurant findRestaurant(RestaurantApprovalRequest restaurantApprovalRequest) {
        Restaurant restaurant = restaurantDataMapper.restaurantApprovalRequestToRestaurant(restaurantApprovalRequest);

        Restaurant restaurantResult = restaurantRepository.findRestaurantInformation(restaurant)
                .orElseThrow(() -> {
                    log.error("식당 정보를 찾을 수 없습니다. restaurantId: {}", restaurant.getId().getValue());
                    return new RestaurantNotFoundException(String.format("Id: %s 식당 정보를 찾을 수 없습니다.", restaurant.getId().getValue()));
                });

        restaurant.setActive(restaurantResult.getActive());
        restaurant.getOrderDetail().getProducts().forEach(
                product -> restaurantResult.getOrderDetail().getProducts().forEach(
                        p -> {
                            if (p.getId().equals(product.getId())) {
                                product.updateWithConfirmedNamePriceAndAvailability(p.getName(), p.getPrice(), p.getAvailable());
                            }
                    })
        );
        restaurant.getOrderDetail().setId(new OrderId(UUID.fromString(restaurantApprovalRequest.getOrderId())));
        return restaurant;
    }

    private boolean publishIfOutboxMessageProcessed(RestaurantApprovalRequest restaurantApprovalRequest){
        return orderOutboxHelper.getCompletedOrderOutboxMessageBySagaIdAndOutboxStatus(
                UUID.fromString(restaurantApprovalRequest.getSagaId()),
                OutboxStatus.COMPLETED)
                .map(orderOutboxMessage -> {
                    restaurantApprovalResponseMessagePublisher.publish(orderOutboxMessage, orderOutboxHelper::updateOutboxStatus);
                    return true;
                }).orElse(false);
    }
}
