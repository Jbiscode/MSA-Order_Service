package com.food.ordering.system.order.service.domain;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    /**
     * OrderDomainService는 domain-core에서 스프링 빈으로 등록되어 있지 않기 때문에<br/>
     * 사용을 하기 위해서는 직접 빈으로 등록해주어야 한다.
     * @return OrderDomainService
     */
    @Bean
    public OrderDomainService orderDomainService() {
        return new OrderDomainServiceImpl();
    }
}
