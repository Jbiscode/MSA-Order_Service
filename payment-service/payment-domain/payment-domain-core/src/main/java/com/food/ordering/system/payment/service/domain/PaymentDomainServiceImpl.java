package com.food.ordering.system.payment.service.domain;

import com.food.ordering.system.domain.valueobject.Money;
import com.food.ordering.system.domain.valueobject.PaymentStatus;
import com.food.ordering.system.payment.service.domain.entity.CreditEntry;
import com.food.ordering.system.payment.service.domain.entity.CreditHistory;
import com.food.ordering.system.payment.service.domain.entity.Payment;
import com.food.ordering.system.payment.service.domain.event.PaymentCancelledEvent;
import com.food.ordering.system.payment.service.domain.event.PaymentCompletedEvent;
import com.food.ordering.system.payment.service.domain.event.PaymentEvent;
import com.food.ordering.system.payment.service.domain.event.PaymentFailedEvent;
import com.food.ordering.system.payment.service.domain.valueobject.CreditHistoryId;
import com.food.ordering.system.payment.service.domain.valueobject.TransactionalType;
import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static com.food.ordering.system.domain.DomainConstants.ASIA_SEOUL;

@Slf4j
public class PaymentDomainServiceImpl implements PaymentDomainService {

    /**
     * 결제를 검증하고 결제를 진행합니다. (결제금액 지출처리)
     * @param payment 결제 AggregateRoot
     * @param creditEntry 진행중인 결제금액
     * @param creditHistories 결제이력
     * @param failureMessages 실패메시지
     * @return 결제이벤트
     */
    @Override
    public PaymentEvent validateAndInitiatePayment(Payment payment,
                                                    CreditEntry creditEntry,
                                                    List<CreditHistory> creditHistories,
                                                    List<String> failureMessages) {
        payment.validatePayment(failureMessages);
        payment.initializePayment();
        validateCreditEntry(payment, creditEntry, failureMessages);
        subtractCreditEntry(payment, creditEntry);
        updateCreditHistory(payment, creditHistories,TransactionalType.DEBIT);
        validateCreditHistory(creditEntry, creditHistories, failureMessages);

        if(failureMessages.isEmpty()) {
            log.info("결제가 성공적으로 진행되었습니다. CustomerId: {} OrderId: {}",
                    creditEntry.getCustomerId().getValue(),
                    payment.getOrderId().getValue());
            payment.updatePaymentStatus(PaymentStatus.COMPLETED);

            return new PaymentCancelledEvent(payment, ZonedDateTime.now(ZoneId.of(ASIA_SEOUL)));
        }else {
            log.error("결제가 실패하였습니다. CustomerId: {} OrderId: {}",
                    creditEntry.getCustomerId().getValue(),
                    payment.getOrderId().getValue());
            payment.updatePaymentStatus(PaymentStatus.FAILED);

            return new PaymentFailedEvent(payment, ZonedDateTime.now(ZoneId.of(ASIA_SEOUL)), failureMessages);
        }
    }

    /**
     * 결제를 검증하고 결제를 취소합니다. (결제금액 회수처리)
     * @param payment   결제 AggregateRoot
     * @param creditEntry 진행중인 결제금액
     * @param creditHistories 결제이력
     * @param failureMessages 실패메시지
     * @return 결제이벤트
     */
    @Override
    public PaymentEvent validateAndCancelPayment(Payment payment,
                                                CreditEntry creditEntry,
                                                List<CreditHistory> creditHistories,
                                                List<String> failureMessages) {
        payment.validatePayment(failureMessages);
        addCreditEntry(payment, creditEntry);
        updateCreditHistory(payment, creditHistories,TransactionalType.CREDIT);

        if(failureMessages.isEmpty()) {
            log.info("결제취소가 성공적으로 진행되었습니다. CustomerId: {} OrderId: {}",
                    creditEntry.getCustomerId().getValue(),
                    payment.getOrderId().getValue());
            payment.updatePaymentStatus(PaymentStatus.CANCELLED);

            return new PaymentCompletedEvent(payment, ZonedDateTime.now(ZoneId.of(ASIA_SEOUL)));
        }else {
            log.error("결제가 취소가 실패하였습니다. CustomerId: {} OrderId: {}",
                    creditEntry.getCustomerId().getValue(),
                    payment.getOrderId().getValue());
            payment.updatePaymentStatus(PaymentStatus.FAILED);

            return new PaymentFailedEvent(payment, ZonedDateTime.now(ZoneId.of(ASIA_SEOUL)), failureMessages);
        }
    }


    private void validateCreditHistory(CreditEntry creditEntry,
                                        List<CreditHistory> creditHistories,
                                        List<String> failureMessages) {
        Money totalDebitHistory = getTotalHistoryAmount(creditHistories, TransactionalType.DEBIT);
        Money totalCreditHistory = getTotalHistoryAmount(creditHistories, TransactionalType.CREDIT);

        if (totalDebitHistory.isGreaterThan(totalCreditHistory)) {
            log.error("잔액이 부족합니다. CustomerId: {} 사용가능한도: {} 사용시도: {}",
                    creditEntry.getCustomerId().getValue(),
                    totalCreditHistory,
                    totalDebitHistory);
            failureMessages.add(String.format("CustomerId: %s 의 잔액이 부족합니다.", creditEntry.getCustomerId().getValue()));
        }

        if (!creditEntry.getTotalCreditAmount().equals(totalCreditHistory.subtract(totalDebitHistory))){
            log.error("잔액이 일치하지 않습니다. CustomerId: {} 사용가능한도: {} 사용시도: {}",
                    creditEntry.getCustomerId().getValue(),
                    totalCreditHistory,
                    totalDebitHistory);
            failureMessages.add(String.format("CustomerId: %s 의 잔액이 일치하지 않습니다.", creditEntry.getCustomerId().getValue()));
        }

    }

    private Money getTotalHistoryAmount(List<CreditHistory> creditHistories,
                                        TransactionalType transactionalType) {
        return creditHistories.stream()
                .filter(
                        creditHistory ->
                                creditHistory.getTransactionalType()
                                        .equals(transactionalType)
                )
                .map(CreditHistory::getCreditAmount)
                .reduce(Money.ZERO, Money::add);
    }

    private void updateCreditHistory(Payment payment,
                                    List<CreditHistory> creditHistories,
                                    TransactionalType transactionalType) {
        creditHistories.add(
                CreditHistory.builder()
                        .id(new CreditHistoryId(UUID.randomUUID()))
                        .customerId(payment.getCustomerId())
                        .creditAmount(payment.getPrice())
                        .transactionalType(transactionalType)
                        .build()
        );
    }

    private void subtractCreditEntry(Payment payment,
                                    CreditEntry creditEntry) {
        creditEntry.subtractCreditAmount(payment.getPrice());
    }

    private void validateCreditEntry(Payment payment,
                                    CreditEntry creditEntry,
                                    List<String> failureMessages) {
        if(payment.getPrice().isGreaterThan(creditEntry.getTotalCreditAmount())) {
            log.error("금액이 결제하기에 충분하지 않습니다. CustomerId: {} CreditAmount: {} OrderPrice: {}",
                    creditEntry.getCustomerId(),
                    creditEntry.getTotalCreditAmount(),
                    payment.getPrice());
            failureMessages.add("CustomerId: " + creditEntry.getCustomerId().getValue() +" 의 금액이 결제하기에 충분하지 않습니다.");
        }
    }

    private void addCreditEntry(Payment payment, CreditEntry creditEntry) {
        creditEntry.addCreditAmount(payment.getPrice());
    }
}
