package com.food.ordering.system.domain.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@EqualsAndHashCode
@SuperBuilder
@NoArgsConstructor
public abstract class BaseEntity<ID> {
    private ID id;

}
