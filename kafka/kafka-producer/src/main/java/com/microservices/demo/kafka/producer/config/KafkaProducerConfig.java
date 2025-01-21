package com.microservices.demo.kafka.producer.config;

import com.microservices.demo.config.KafkaConfigData;
import com.microservices.demo.config.KafkaProducerConfigData;
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
public class KafkaProducerConfig<K extends Serializable, V extends SpecificRecordBase>{
    private final KafkaConfigData kafkaConfigData;
    private final KafkaProducerConfigData kafkaProducerConfigData;

    public KafkaProducerConfig(KafkaConfigData kafkaConfigData, KafkaProducerConfigData kafkaProducerConfigData) {
        this.kafkaConfigData = kafkaConfigData;
        this.kafkaProducerConfigData = kafkaProducerConfigData;
    }

    // We will create a bean to return Map String object, which will include our configuration properties
    @Bean
    public Map<String, Object> producerConfig(){
        Map<String, Object> props = new HashMap<>();
        // Fill the properties in the producer config method.
        // First we need to set the bootstrap server config where we will get the configuration from kafkaConfigData.
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfigData.getBootstrapServers());

        // We will set the Schema registry url key and value by again reading it from kafkaConfigData.
        props.put(kafkaConfigData.getSchemaRegistryUrlKey(), kafkaConfigData.getSchemaRegistryUrl());

        // We will set the producer configurations from kafkaProducerConfigData like key and value serializers.
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, kafkaProducerConfigData.getKeySerializerClass());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, kafkaProducerConfigData.getValueSerializerClass());
        // Batch size
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, kafkaProducerConfigData.getBatchSize() * kafkaProducerConfigData.getBatchSizeBoostFactor());
        // LingerMs
        props.put(ProducerConfig.LINGER_MS_CONFIG, kafkaProducerConfigData.getLingerMs());
        // Compression type
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, kafkaProducerConfigData.getCompressionType());
        // Acknowledgement type
        props.put(ProducerConfig.ACKS_CONFIG, kafkaProducerConfigData.getAcks());
        // Request Timeout ms
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, kafkaProducerConfigData.getRequestTimeoutMs());
        // Retry count
        props.put(ProducerConfig.RETRIES_CONFIG, kafkaProducerConfigData.getRetryCount());

        return props;
    }

    // We will crate a bean to construct a producer factory where we will return a default kafka producer.
    @Bean
    public ProducerFactory<K, V> producerFactory(){
        return new DefaultKafkaProducerFactory<>(producerConfig());
    }

    // We will create a bean to return Kafka Template and pass the producer factory as a parameter
    // KafkaTemplate: A Thread-safe Template for executing high-level Producer operations. KafkaTemplate is actually a wrapper class around kafka producer and it provides some methods to be able to send data to kafka easily.
    @Bean
    public KafkaTemplate<K, V> kafkaTemplate(){
        return new KafkaTemplate<>(producerFactory());
    }
}