package com.food.ordering.system.payment.service.domain.entity;

import com.food.ordering.system.domain.entity.BaseEntity;
import com.food.ordering.system.domain.valueobject.CustomerId;
import com.food.ordering.system.domain.valueobject.Money;
import com.food.ordering.system.payment.service.domain.valueobject.CreditHistoryId;
import com.food.ordering.system.payment.service.domain.valueobject.TransactionalType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class CreditHistory extends BaseEntity<CreditHistoryId> {

    private CustomerId customerId;
    private final Money creditAmount;
    private final TransactionalType transactionalType;
}
