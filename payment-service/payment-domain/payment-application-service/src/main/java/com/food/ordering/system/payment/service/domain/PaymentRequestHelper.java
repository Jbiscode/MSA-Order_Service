package com.food.ordering.system.payment.service.domain;

import com.food.ordering.system.domain.valueobject.CustomerId;
import com.food.ordering.system.domain.valueobject.OrderId;
import com.food.ordering.system.domain.valueobject.PaymentStatus;
import com.food.ordering.system.outbox.OutboxStatus;
import com.food.ordering.system.payment.service.domain.dto.PaymentRequest;
import com.food.ordering.system.payment.service.domain.entity.CreditEntry;
import com.food.ordering.system.payment.service.domain.entity.CreditHistory;
import com.food.ordering.system.payment.service.domain.entity.Payment;
import com.food.ordering.system.payment.service.domain.event.PaymentEvent;
import com.food.ordering.system.payment.service.domain.exception.PaymentApplicationServiceException;
import com.food.ordering.system.payment.service.domain.exception.PaymentNotFoundException;
import com.food.ordering.system.payment.service.domain.mapper.PaymentDataMapper;
import com.food.ordering.system.payment.service.domain.outbox.model.OrderOutboxMessage;
import com.food.ordering.system.payment.service.domain.outbox.scheduler.OrderOutboxHelper;
import com.food.ordering.system.payment.service.domain.ports.output.message.publisher.PaymentResponseMessagePublisher;
import com.food.ordering.system.payment.service.domain.ports.output.repository.CreditEntryRepository;
import com.food.ordering.system.payment.service.domain.ports.output.repository.CreditHistoryRepository;
import com.food.ordering.system.payment.service.domain.ports.output.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentRequestHelper {

    private final PaymentDomainService paymentDomainService;

    private final PaymentRepository paymentRepository;

    private final PaymentDataMapper paymentDataMapper;

    private final CreditEntryRepository creditEntryRepository;

    private final CreditHistoryRepository creditHistoryRepository;

    private final OrderOutboxHelper orderOutboxHelper;

    private final PaymentResponseMessagePublisher paymentResponseMessagePublisher;

    @Transactional
    public void persistPayment(PaymentRequest paymentRequest) {
        if (publishIfOutboxMessageProcessedForPayment(paymentRequest, PaymentStatus.COMPLETED)) {
            log.info("결제가 이미 완료되었습니다.persistPayment=> orderId: {} sagaId: {}",
                    paymentRequest.getOrderId(),
                    paymentRequest.getSagaId());
            return;
        }
        log.info("결제가 생성되었습니다.persistPayment=> orderId: {}", paymentRequest.getOrderId());
        Payment payment = paymentDataMapper.paymentRequestToPayment(paymentRequest);

        CreditEntry creditEntry = getCreditEntry(payment.getCustomerId());
        List<CreditHistory> creditHistories = getCreditHistory(payment.getCustomerId());
        PaymentEvent paymentEvent =
                paymentDomainService.validateAndInitiatePayment(payment, creditEntry, creditHistories, new ArrayList<>());

        persistDbObjects(payment, paymentEvent, creditEntry, creditHistories);

        orderOutboxHelper.saveOrderOutboxMessage(
                paymentDataMapper.paymentEventToOrderEventPayload(paymentEvent),
                paymentEvent.getPayment().getPaymentStatus(),
                OutboxStatus.STARTED,
                UUID.fromString(paymentRequest.getSagaId()));

    }

    @Transactional
    public void persistCancelPayment(PaymentRequest paymentRequest) {
        if (publishIfOutboxMessageProcessedForPayment(paymentRequest, PaymentStatus.CANCELLED)) {
            log.info("결제가 이미 취소되었습니다.persistCancelPayment=> orderId: {} sagaId: {}",
                    paymentRequest.getOrderId(),
                    paymentRequest.getSagaId());
            return;
        }
        log.info("결제 취소 요청을 받았습니다.persistCancelPayment=> orderId: {}", paymentRequest.getOrderId());
        Payment payment = paymentRepository.findByOrderId(new OrderId(UUID.fromString(paymentRequest.getOrderId())))
                .orElseThrow(() -> {
                    log.error("{} 주문의 결제정보가 존재하지 않습니다.",paymentRequest.getOrderId());
                    return new PaymentNotFoundException(String.format("%s 주문의 결제정보가 존재하지 않습니다.",paymentRequest.getOrderId()));
                });

        CreditEntry creditEntry = getCreditEntry(payment.getCustomerId());
        List<CreditHistory> creditHistories = getCreditHistory(payment.getCustomerId());
        PaymentEvent paymentEvent =
                paymentDomainService.validateAndCancelPayment(payment, creditEntry, creditHistories, new ArrayList<>());

        persistDbObjects(payment, paymentEvent, creditEntry, creditHistories);

        orderOutboxHelper.saveOrderOutboxMessage(
                paymentDataMapper.paymentEventToOrderEventPayload(paymentEvent),
                paymentEvent.getPayment().getPaymentStatus(),
                OutboxStatus.STARTED,
                UUID.fromString(paymentRequest.getSagaId()));

    }

    private void persistDbObjects(Payment payment,
                                    PaymentEvent paymentEvent,
                                    CreditEntry creditEntry,
                                    List<CreditHistory> creditHistories) {
        paymentRepository.save(payment);
        if(paymentEvent.getFailureMessages().isEmpty()){
            creditEntryRepository.save(creditEntry);
            creditHistoryRepository.save(creditHistories.get(creditHistories.size()-1));
        }
    }

    private List<CreditHistory> getCreditHistory(CustomerId customerId) {
        return creditHistoryRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new PaymentApplicationServiceException(String.format("%s 고객의 결제이력이 존재하지 않습니다.",customerId.getValue())));
    }

    private CreditEntry getCreditEntry(CustomerId customerId) {
        return creditEntryRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new PaymentApplicationServiceException(String.format("%s 고객의 결제금액이 존재하지 않습니다.",customerId.getValue())));
    }

    private boolean publishIfOutboxMessageProcessedForPayment(PaymentRequest paymentRequest,
                                                              PaymentStatus paymentStatus) {
        return orderOutboxHelper.getCompletedOrderOutboxMessageBySagaIdAndPaymentStatus(
                        UUID.fromString(paymentRequest.getSagaId()),
                        paymentStatus)
                .map(orderOutboxMessage -> {
                    paymentResponseMessagePublisher.publish(orderOutboxMessage, orderOutboxHelper::updateOutboxStatus);
                    return true;
                }).orElse(false);
    }
}
