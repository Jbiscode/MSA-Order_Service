package com.food.ordering.system.kafka.producer.service.service.Impl;

import com.food.ordering.system.kafka.producer.service.exception.KafkaProducerException;
import com.food.ordering.system.kafka.producer.service.service.KafkaProducer;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
@Component
public class KafkaProducerImpl<K extends Serializable, V extends SpecificRecordBase> implements KafkaProducer<K, V> {

    private final KafkaTemplate<K, V> kafkaTemplate;

    @Override
    public CompletableFuture<Void> send(String topicName, K key, V message) {
        log.info("Kafka 메시지 전송중 ... message='{}' to topic='{}'", message, topicName);

        return kafkaTemplate.send(topicName, key, message)
                .thenAccept(sendResult -> log.info("Kafka 전송완료 message='{}' to topic='{}' with offset={}",
                        message, topicName, sendResult.getRecordMetadata().offset()))
                .exceptionally(e -> {
                    log.error("KafkaProducerImpl:Kafka 전송실패 with message='{}' to topic='{}' and Exception='{}'", message, topicName,e.getMessage());
                    throw new KafkaProducerException("KafkaProducerImpl:전송실패 key: " + key +" & message: "+ message);
                });
    }

    @PreDestroy
    public void close(){
        if(kafkaTemplate != null){
            log.info("KafkaTemplate 종료중...");
            kafkaTemplate.flush();
            kafkaTemplate.destroy();
            log.info("KafkaTemplate 종료완료");
        }
    }
}
