package com.food.ordering.system.payment.service.domain.entity;

import com.food.ordering.system.domain.valueobject.BaseId;
import com.food.ordering.system.domain.valueobject.CustomerId;
import com.food.ordering.system.domain.valueobject.Money;
import com.food.ordering.system.payment.service.domain.valueobject.CreditEntryId;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Payment 에 포함되지 않는 이유는
 * 금액을 충전하는것과 지불하는것을 독립적으로 관리하기 위함
 */
@Getter
@SuperBuilder
public class CreditEntry extends BaseId<CreditEntryId> {

    private final CustomerId customerId;

    private Money totalCreditAmount;


    public void addCreditAmount(Money amount) {
        totalCreditAmount = totalCreditAmount.add(amount);
    }

    public void subtractCreditAmount(Money amount) {
        totalCreditAmount = totalCreditAmount.subtract(amount);
    }
}
