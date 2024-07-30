package com.food.ordering.system.kafka.consumer;

import org.apache.avro.specific.SpecificRecordBase;

import java.util.List;

// kafka consumer interface
// keys 값은 주문 ID를 key로 사용하기때문에 String 타입으로 변경
public interface KafkaConsumer <T extends SpecificRecordBase> {
    void receive(List<T> messages, List<String> keys, List<Integer> partitions, List<Long> offsets);
}
