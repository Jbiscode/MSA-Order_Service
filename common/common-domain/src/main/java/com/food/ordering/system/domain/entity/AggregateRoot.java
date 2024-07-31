package com.food.ordering.system.domain.entity;

import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
public abstract class AggregateRoot<ID> extends BaseEntity<ID> {

}
