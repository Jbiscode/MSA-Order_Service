package com.food.ordering.system.domain.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public abstract class BaseId<T> {
    private final T value;

    // 하위 클래스에서만 생성자 호출할 수 있으면 되니까. protected
    protected BaseId(T value) {
        this.value = value;
    }
}
