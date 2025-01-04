package com.microservices.demo.kafka.producer.config.service;

import org.apache.avro.specific.SpecificRecordBase;

import java.io.Serializable;

// Specifying two generic variables
public interface KafkaProducer<K extends Serializable, V extends SpecificRecordBase>{
    void send(String topicName, K key, V message);
}
