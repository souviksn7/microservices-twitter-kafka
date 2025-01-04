package com.microservices.demo.twitter.to.kafka.service.init.impl;

import com.microservices.demo.config.KafkaConfigData;
import com.microservices.demo.kafka.admin.clients.KafkaAdminClient;
import com.microservices.demo.twitter.to.kafka.service.init.StreamInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class KafkaStreamInitializer implements StreamInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaStreamInitializer.class);

    private final KafkaConfigData kafkaConfigData;

    private final KafkaAdminClient kafkaAdminClient;

    public KafkaStreamInitializer(KafkaConfigData kafkaConfigData, KafkaAdminClient kafkaAdminClient) {
        this.kafkaConfigData = kafkaConfigData;
        this.kafkaAdminClient = kafkaAdminClient;
    }

    @Override
    public void init() {
        // Call kafkaAdminClient's createTopics() method in the init method to create the required topics
        kafkaAdminClient.createTopics();

        // Call kafkaAdminClient's checkSchemaRegistry() method to be sure that schema registry is up and running prior to starting of our service
        kafkaAdminClient.checkSchemaRegistry();

        LOG.info("Topics with the name {} are ready for operation", kafkaConfigData.getTopicNamesToCreate().toArray());
    }
}
