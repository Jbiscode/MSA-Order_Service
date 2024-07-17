package com.food.ordering.system.domain.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class Money  {
    private final BigDecimal amount;

//    compareTo를 사용해야 BigDecimal 값의 소숫점까지 정확히 비교 가능
    public boolean isGreaterThanZero() {
        return this.amount != null && this.amount.compareTo(BigDecimal.ZERO) > 0;
    }
    public boolean isGreaterThan(Money money) {
        return this.amount != null && this.amount.compareTo(money.getAmount()) > 0;
    }


    public Money add(Money money) {
        return new Money(setScale(this.amount.add(money.getAmount())));
    }
    public Money subtract(Money money) {
        return new Money(setScale(this.amount.subtract(money.getAmount())));
    }
    public Money multiply(int multiplier) {
        return new Money(setScale(this.amount.multiply(new BigDecimal(multiplier))));
    }


//    누적오류를 최소화하기 위해서 RoundMode.HALF_EVEN 사용
    private BigDecimal setScale(BigDecimal input) {
        return input.setScale(2, RoundingMode.HALF_EVEN);
    }
}
