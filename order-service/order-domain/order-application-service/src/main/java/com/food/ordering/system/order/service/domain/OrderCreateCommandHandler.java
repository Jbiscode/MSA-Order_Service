package com.food.ordering.system.order.service.domain;

import com.food.ordering.system.order.service.domain.dto.create.CreateOrderCommand;
import com.food.ordering.system.order.service.domain.dto.create.CreateOrderResponse;
import com.food.ordering.system.order.service.domain.event.OrderCreatedEvent;
import com.food.ordering.system.order.service.domain.exception.OrderDomainException;
import com.food.ordering.system.order.service.domain.mapper.OrderDataMapper;
import com.food.ordering.system.order.service.domain.ports.output.message.publisher.payment.OrderCreatedPaymentRequestMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 주문 생성 과정을 담당하는 핸들러 클래스입니다.
 * 주문 생성 요청을 받아 도메인 로직을 실행하고, 결과를 반환합니다.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class OrderCreateCommandHandler {

    private final OrderCreateHelper orderCreateHelper;

    private final OrderDataMapper orderDataMapper;

    private final OrderCreatedPaymentRequestMessagePublisher orderCreatedPaymentRequestMessagePublisher;

    /**
     * 주문 생성 요청을 처리하고 주문 생성 결과를 반환합니다.
     *
     * @param createOrderCommand 주문 생성에 필요한 정보를 담고 있는 커맨드 객체입니다.
     * @return 생성된 주문의 응답 객체입니다.
     * @throws OrderDomainException 고객이나 식당 정보를 찾을 수 없는 경우 예외를 발생시킵니다.
     */

    public CreateOrderResponse createOrder(CreateOrderCommand createOrderCommand) {
        OrderCreatedEvent orderCreatedEvent = orderCreateHelper.persistOrder(createOrderCommand);
        log.info("주문이 생성되었습니다. orderId: {}", orderCreatedEvent.getOrder().getId().getValue());
        // OrderCreatedEvent 가 발생되면 이후에 메시지를 퍼블리싱한다.(persistOrder 메서드는 @Transactional 이어서 실패시 롤백)
        orderCreatedPaymentRequestMessagePublisher.publish(orderCreatedEvent);

        return orderDataMapper.orderToCreateOrderResponse(orderCreatedEvent.getOrder(), "주문이 성공적으로 생성되었습니다.");
    }

}
