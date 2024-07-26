package com.food.ordering.system.kafka.producer.service;

import com.food.ordering.system.kafka.config.data.KafkaConfigData;
import com.food.ordering.system.kafka.config.data.KafkaProducerConfigData;
import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class KafkaProducerConfig <K extends Serializable, V extends SpecificRecordBase>{

    private final KafkaConfigData kafkaConfigData;
    private final KafkaProducerConfigData kafkaProducerConfigData;


    @Bean
    public Map<String, Object> producerConfig(){
        return new HashMap<>() {{
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfigData.getBootstrapServers());
            put(kafkaConfigData.getSchemaRegistryUrlKey(), kafkaConfigData.getSchemaRegistryUrl());

            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, kafkaProducerConfigData.getKeySerializerClass());
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, kafkaProducerConfigData.getValueSerializerClass());
            put(ProducerConfig.BATCH_SIZE_CONFIG, kafkaProducerConfigData.getBatchSize() * kafkaProducerConfigData.getBatchSizeBoostFactor());
            put(ProducerConfig.LINGER_MS_CONFIG, kafkaProducerConfigData.getLingerMs());
            put(ProducerConfig.COMPRESSION_TYPE_CONFIG, kafkaProducerConfigData.getCompressionType());
            put(ProducerConfig.ACKS_CONFIG, kafkaProducerConfigData.getAcks());
            put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, kafkaProducerConfigData.getRequestTimeoutMs());
            put(ProducerConfig.RETRIES_CONFIG, kafkaProducerConfigData.getRetryCount());
        }};
    }

    @Bean
    public ProducerFactory<K, V> producerFactory(){
        return new DefaultKafkaProducerFactory<>(producerConfig());
    }

    // 생성된 템플릿은 KafkaProducerImpl 에서 사용.
    @Bean
    public KafkaTemplate<K, V> kafkaTemplate(){
        return new KafkaTemplate<>(producerFactory());
    }
}
