package com.food.ordering.system.order.service.messaging.listener.kafka;

import com.food.ordering.system.kafka.consumer.KafkaConsumer;
import com.food.ordering.system.kafka.order.avro.model.PaymentResponseAvroModel;
import com.food.ordering.system.kafka.order.avro.model.PaymentStatus;
import com.food.ordering.system.order.service.domain.ports.input.message.listener.payment.PaymentResponseMessageListener;
import com.food.ordering.system.order.service.messaging.mapper.OrderMessagingDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentResponseKafkaListener implements KafkaConsumer<PaymentResponseAvroModel> {

    private final PaymentResponseMessageListener paymentResponseMessageListener;
    private final OrderMessagingDataMapper orderMessagingDataMapper;

    @Override
    @KafkaListener(id = "${kafka-consumer-config.payment.consumer.group-id}",
            topics = "${order-service.payment-response-topic-name}")
    public void receive(@Payload List<PaymentResponseAvroModel> messages,
                        @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
                        @Header(KafkaHeaders.OFFSET) List<Long> offsets) {

        log.info("{}개의 메시지수신 ... messages='{}' key='{}' partitions='{}' and offset='{}'",
                messages.size()
                , messages
                , keys.toString()
                , partitions.toString()
                , offsets.toString());

        messages.forEach(paymentResponseAvroModel -> {
            try {
                if (PaymentStatus.COMPLETED == paymentResponseAvroModel.getPaymentStatus()) {
                    log.info("주문결제응답을 성공적으로 처리합니다. order id: {}", paymentResponseAvroModel.getOrderId());
                    paymentResponseMessageListener.paymentCompleted(
                            orderMessagingDataMapper.paymentResponseAvroModelToPaymentResponseEvent(paymentResponseAvroModel)
                    );
                } else if (PaymentStatus.FAILED == paymentResponseAvroModel.getPaymentStatus() ||
                        PaymentStatus.CANCELLED == paymentResponseAvroModel.getPaymentStatus()) {
                    log.info("주문결제응답을 실패했습니다. order id: {}", paymentResponseAvroModel.getOrderId());
                    paymentResponseMessageListener.paymentCancelled(
                            orderMessagingDataMapper.paymentResponseAvroModelToPaymentResponseEvent(paymentResponseAvroModel)
                    );
                }
            } catch (Exception e) {
                log.error("주문결제응답을 처리하는 도중 오류가 발생했습니다. paymentResponseAvroModel: {} & error: {}",
                        paymentResponseAvroModel, e.getMessage());
            }
        });
    }
}

