package com.microservices.demo.kafka.producer.config.service.impl;

import com.microservices.demo.kafka.avro.model.TwitterAvroModel;
import com.microservices.demo.kafka.producer.config.service.KafkaProducer;
import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class TwitterKafkaProducer implements KafkaProducer<Long, TwitterAvroModel> {

    private static final Logger LOG = LoggerFactory.getLogger(TwitterKafkaProducer.class);

    private KafkaTemplate<Long, TwitterAvroModel> kafkaTemplate;

    public TwitterKafkaProducer(KafkaTemplate<Long, TwitterAvroModel> kafkaTemplate){
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void send(String topicName, Long key, TwitterAvroModel message){
        LOG.info("Sending messages='{}' to topic='{}'", message, topicName);

        // It returns a CompletableFuture, which allows you to handle the result or failure without blocking the calling thread.
        // CompletableFuture is used for its flexibility, non-blocking nature, and support for chaining callbacks, making it ideal for handling asynchronous operations in modern Java.
        CompletableFuture<SendResult<Long, TwitterAvroModel>> kafkaResultFuture =
                kafkaTemplate.send(topicName, key, message); // kafkaTemplate.send is an asynchronous method used to send a message to a specified Kafka topic.

        addCallback(topicName, message, kafkaResultFuture);
    }

    @PreDestroy
    public void close(){
        if(kafkaTemplate != null){
            LOG.info("Closing kafka producer");
            kafkaTemplate.destroy();
        }
    }

    private static void addCallback(String topicName, TwitterAvroModel message, CompletableFuture<SendResult<Long, TwitterAvroModel>> kafkaResultFuture) {
        kafkaResultFuture.whenComplete((result, throwable) -> {
            if(throwable != null) {
                // Handle failure
                LOG.error("Error while sending messsage {} to kafka topic {}", message.toString(), topicName, throwable);
            } else {
                // Handle success
                RecordMetadata metadata = result.getRecordMetadata();
                LOG.debug("Received new metadata. Topic: {}; Partition: {}; Offset: {}; Timestamp: {}; at time: {}",
                        metadata.topic(),
                        metadata.partition(),
                        metadata.offset(),
                        metadata.timestamp(),
                        System.nanoTime());
            }

        });
    }
}
