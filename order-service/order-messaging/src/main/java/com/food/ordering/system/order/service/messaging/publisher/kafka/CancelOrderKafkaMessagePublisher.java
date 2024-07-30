package com.food.ordering.system.order.service.messaging.publisher.kafka;

import com.food.ordering.system.kafka.order.avro.model.PaymentRequestAvroModel;
import com.food.ordering.system.kafka.producer.service.service.KafkaProducer;
import com.food.ordering.system.kafka.producer.service.service.KafkaMessageHelper;
import com.food.ordering.system.order.service.domain.config.OrderServiceConfigData;
import com.food.ordering.system.order.service.domain.event.OrderCancelledEvent;
import com.food.ordering.system.order.service.domain.ports.output.message.publisher.payment.OrderCancelledPaymentRequestMessagePublisher;
import com.food.ordering.system.order.service.messaging.mapper.OrderMessagingDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class CancelOrderKafkaMessagePublisher implements OrderCancelledPaymentRequestMessagePublisher {

    private final OrderMessagingDataMapper orderMessagingDataMapper;
    private final OrderServiceConfigData orderServiceConfigData;
    private final KafkaProducer<String, PaymentRequestAvroModel> kafkaProducer;
    private final KafkaMessageHelper kafkaMessageHelper;

    @Override
    public void publish(OrderCancelledEvent domainEvent) {
        String orderId = domainEvent.getOrder().getId().getValue().toString();
        try {
            log.info("주문취소이벤트를 카프카로 발행합니다. orderId: {}", orderId);

            // 주문생성 이벤트를 PaymentRequestAvroModel로 변환 (Kafka 메시지)
            PaymentRequestAvroModel paymentRequestAvroModel =
                    orderMessagingDataMapper.orderCancelledEventToPaymentRequestAvroModel(domainEvent);

            kafkaProducer.send(
                    orderServiceConfigData.getPaymentRequestTopicName(),
                    orderId,
                    paymentRequestAvroModel,
                    kafkaMessageHelper.createKafkaMessageHelperRequest(
                            orderId,
                            "PaymentRequestAvroModel"
                    )
            );

            log.info("주문취소이벤트를 카프카로 발행완료했습니다. orderId: {}", paymentRequestAvroModel.getOrderId());
        } catch (Exception e) {
            log.error("주문취소이벤트를 카프카로 발행실패했습니다. orderId: {} & error: {}",
                    orderId, e.getMessage());
        }

    }
}
