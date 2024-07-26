package com.food.ordering.system.order.service.dataaccess.order.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * OrderItemEntity.java 의 복합키 클래스
 * JPA 에서는 복합키를 사용할 때 별도의 클래스를 생성해야 한다.
 * 추가로 Serializable 인터페이스를 구현해야 한다.
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class OrderItemEntityId implements Serializable {
    private Long id;
    private OrderEntity order;
}
