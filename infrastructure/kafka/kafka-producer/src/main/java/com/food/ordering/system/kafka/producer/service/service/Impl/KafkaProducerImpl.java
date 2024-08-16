package com.food.ordering.system.kafka.producer.service.service.Impl;

import com.food.ordering.system.kafka.producer.service.dto.KafkaMessageHelperRequest;
import com.food.ordering.system.kafka.producer.service.exception.KafkaProducerException;
import com.food.ordering.system.kafka.producer.service.service.KafkaProducer;
import com.food.ordering.system.outbox.OutboxStatus;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.RecordMetadata;
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
    public <U> CompletableFuture<Void> send(String topicName, K key, V message,
                                            KafkaMessageHelperRequest<U> kafkaMessageHelperRequests) {
        log.info("Kafka 메시지 전송중 ... message='{}' to topic='{}'", message, topicName);

        return kafkaTemplate.send(topicName, key, message)
                .thenAccept(sendResult -> {
                    RecordMetadata metadata = sendResult.getRecordMetadata();

                    log.info("Kafka 전송완료 orderId='{}' to topic='{}' partition='{}' offset='{}' timestamp='{}'",
                            kafkaMessageHelperRequests.orderId(),
                            metadata.topic(),
                            metadata.partition(),
                            metadata.offset(),
                            metadata.timestamp());

                    kafkaMessageHelperRequests.outboxCallback()
                            .accept(kafkaMessageHelperRequests.outboxMessage(), OutboxStatus.COMPLETED);

                })
                .exceptionally(e -> {
                    log.error("전송실패 KafkaProducerImpl({}):Kafka with message='{}' outboxType='{}' to topic='{}' and Exception='{}'",
                            kafkaMessageHelperRequests.avroModelName(),
                            message.toString(),
                            kafkaMessageHelperRequests.outboxMessage().getClass().getName(),
                            topicName,
                            e.getMessage());

                    kafkaMessageHelperRequests.outboxCallback()
                            .accept(kafkaMessageHelperRequests.outboxMessage(), OutboxStatus.FAILED);

                    throw new KafkaProducerException("전송실패 KafkaProducerImpl("+kafkaMessageHelperRequests.avroModelName()+"): key: " + key +" & message: "+ message);
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
