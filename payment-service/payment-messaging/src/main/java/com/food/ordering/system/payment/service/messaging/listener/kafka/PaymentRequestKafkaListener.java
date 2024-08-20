package com.food.ordering.system.payment.service.messaging.listener.kafka;

import com.food.ordering.system.kafka.consumer.KafkaConsumer;
import com.food.ordering.system.kafka.order.avro.model.PaymentOrderStatus;
import com.food.ordering.system.kafka.order.avro.model.PaymentRequestAvroModel;
import com.food.ordering.system.payment.service.domain.exception.PaymentApplicationServiceException;
import com.food.ordering.system.payment.service.domain.exception.PaymentNotFoundException;
import com.food.ordering.system.payment.service.domain.ports.input.message.listener.PaymentRequestMessageListener;
import com.food.ordering.system.payment.service.messaging.mapper.PaymentMessagingDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PSQLState;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentRequestKafkaListener implements KafkaConsumer<PaymentRequestAvroModel> {

    private final PaymentRequestMessageListener paymentRequestMessageListener;
    private final PaymentMessagingDataMapper paymentMessagingDataMapper;

    @Override
    @KafkaListener(id = "${kafka-consumer-config.payment-consumer-group-id}",
            topics = "${payment-service.payment-request-topic-name}")
    public void receive(@Payload List<PaymentRequestAvroModel> messages,
                        @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
                        @Header(KafkaHeaders.OFFSET) List<Long> offsets) {
log.info("{}개의 메시지수신 ... messages='{}' key='{}' partitions='{}' and offset='{}'",
                messages.size()
                , messages
                , keys.toString()
                , partitions.toString()
                , offsets.toString());

        messages.forEach(paymentRequestAvroModel -> {
            try {
                if(paymentRequestAvroModel.getPaymentOrderStatus() == PaymentOrderStatus.PENDING) {
                    log.info("주문결제요청을 처리중입니다. order id: {}", paymentRequestAvroModel.getOrderId());
                    paymentRequestMessageListener.completePayment(
                            paymentMessagingDataMapper.paymentRequestAvroModelToPaymentRequest(paymentRequestAvroModel)
                    );
                }else if (paymentRequestAvroModel.getPaymentOrderStatus() == PaymentOrderStatus.CANCELLED) {
                    log.info("주문결제요청을 취소합니다. order id: {}", paymentRequestAvroModel.getOrderId());
                    paymentRequestMessageListener.cancelPayment(
                            paymentMessagingDataMapper.paymentRequestAvroModelToPaymentRequest(paymentRequestAvroModel)
                    );
                }
            } catch (DataAccessException e) {
                SQLException sqlException = (SQLException) e.getRootCause();
                if (sqlException!=null
                        && sqlException.getSQLState() != null
                        && PSQLState.UNIQUE_VIOLATION.getState().equals(sqlException.getSQLState())) {
                    log.error("주문결제요청이 중복되었습니다.UNIQUE_VIOLATION:{} order id: {}",
                            sqlException.getSQLState(),
                            paymentRequestAvroModel.getOrderId());
                }else {
                    throw new PaymentApplicationServiceException("주문결제요청 처리중 예외에러가 발생했습니다." + e.getMessage(),e);
                }
            }catch (PaymentNotFoundException e) {
                log.error("주문결제요청을 찾지 못했습니다. orderId: {}", paymentRequestAvroModel.getOrderId());
            }
        });
    }
}
