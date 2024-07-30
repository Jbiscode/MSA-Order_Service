package com.food.ordering.system.kafka.producer.service.service;

import com.food.ordering.system.kafka.producer.service.dto.KafkaMessageHelperRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaMessageHelper {

    public KafkaMessageHelperRequest createKafkaMessageHelperRequest(String orderId, String avroModelName) {
        return new KafkaMessageHelperRequest(orderId, avroModelName);
    }
}
