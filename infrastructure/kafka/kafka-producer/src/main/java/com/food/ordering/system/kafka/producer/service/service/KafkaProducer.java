package com.food.ordering.system.kafka.producer.service.service;

import com.food.ordering.system.kafka.producer.service.dto.KafkaMessageHelperRequest;
import org.apache.avro.specific.SpecificRecordBase;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;

public interface KafkaProducer <K extends Serializable, V extends SpecificRecordBase> {

    /**
     * 카프카에 메시지 보내기
     * @param topic  전송할 토픽
     * @param key   메시지 키
     * @param message 전송할 메시지
     * @return CompletableFuture<Void> 를 사용해서 비동기 처리를 한다.
     *
     */
    CompletableFuture<Void> send(String topic, K key, V message, KafkaMessageHelperRequest kafkaMessageHelperRequests);
}
