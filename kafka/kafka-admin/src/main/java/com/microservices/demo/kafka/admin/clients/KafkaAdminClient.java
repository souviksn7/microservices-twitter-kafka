package com.microservices.demo.kafka.admin.clients;

import com.microservices.demo.config.KafkaConfigData;
import com.microservices.demo.config.RetryConfigData;
import com.microservices.demo.kafka.admin.exception.KafkaClientException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicListing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Component
public class KafkaAdminClient {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaAdminClient.class);

    private final KafkaConfigData kafkaConfigData;

    private final RetryConfigData retryConfigData;

    // AdminClient is a class provided by Apache Kafka within the Kafka Clients library that allows administrative operations on Kafka brokers. It is used to manage and inspect various Kafka cluster resources, such as topics, brokers, and configurations
    private final AdminClient adminClient;

    // A RetryTemplate in Spring Framework is a utility that provides retry operations for executing code that may fail. It allows configuring retry policies and handling failure scenarios with retries, making it ideal for managing transient errors, such as network failures or temporary unavailability of external systems.
    private final RetryTemplate retryTemplate;

    // WebClient is a non-blocking, reactive client for making HTTP requests in Spring WebFlux, part of the Spring Framework. It provides a modern alternative to RestTemplate for interacting with REST APIs and web services. Unlike RestTemplate, which is synchronous, WebClient is asynchronous and reactive, making it suitable for scalable and efficient applications, especially in microservices and cloud-based environments.
    private final WebClient webClient;


    public KafkaAdminClient(KafkaConfigData kafkaConfigData, RetryConfigData retryConfigData, AdminClient adminClient, RetryTemplate retryTemplate, WebClient webClient) {
        this.kafkaConfigData = kafkaConfigData;
        this.retryConfigData = retryConfigData;
        this.adminClient = adminClient;
        this.retryTemplate = retryTemplate;
        this.webClient = webClient;
    }

    public void createTopics(){
        // A Kafka client class representing the result of a topic creation request. It is used to handle the response when attempting to create topics in Kafka asynchronously.
        CreateTopicsResult createTopicsResult;
        try{
            // call a method with retry logic configured in the retry config
            createTopicsResult = retryTemplate.execute(this::doCreateTopics);
        }catch(Throwable t){
            throw new KafkaClientException("Reached maximum number of retry for creating kafka topics", t);
        }
        checkTopicsCreated();
    }

    public void checkTopicsCreated(){
        Collection<TopicListing> topics = getTopics();
        int retryCount = 1;
        Integer maxRetry = retryConfigData.getMaxAttempts();
        Integer multiplier = retryConfigData.getMultiplier().intValue();
        Long sleepTimeMs = retryConfigData.getSleepTimeMs();
        for(String topic: kafkaConfigData.getTopicNamesToCreate()){
            // Wait until topic created or max retry reached, increasing waiting time exponentially
            while(!isTopicCreated(topics, topic)){
                checkMaxRetry(retryCount++, maxRetry);
                sleep(sleepTimeMs);
                sleepTimeMs *= multiplier;
                topics = getTopics();
            }
        }
    }
    // to be able to check if schema registry is up and running
    public void checkSchemaRegistry(){
        int retryCount = 1;
        Integer maxRetry = retryConfigData.getMaxAttempts();
        Integer multiplier = retryConfigData.getMultiplier().intValue();
        Long sleepTimeMs = retryConfigData.getSleepTimeMs();
        while(!getSchemaRegistryStatus().is2xxSuccessful()){
            checkMaxRetry(retryCount, maxRetry);
            sleep(sleepTimeMs);
            sleepTimeMs *= multiplier;
        }
    }

    // We will make a rest call here and return the HTTP status to check the status of the schema registry.
    private HttpStatus getSchemaRegistryStatus(){
        try {
            return (HttpStatus) webClient
                    .method(HttpMethod.GET)
                    .uri(kafkaConfigData.getSchemaRegistryUrl())
                    .exchange()
                    .map(ClientResponse::statusCode)
                    .block();
        } catch (Exception e) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
    }

    private void sleep(Long sleepTimeMs){
        try {
            Thread.sleep(sleepTimeMs);
        } catch (InterruptedException e) {
            throw new KafkaClientException("Error while sleeping for waiting new created topics");
        }
    }

    private void checkMaxRetry(int retry, Integer maxRetry) {
        if(retry>maxRetry)
            throw new KafkaClientException("Reached maximum number of retry for reading kafka topics");
    }

    private boolean isTopicCreated(Collection<TopicListing> topics, String topicName) {
        if(topics == null)
            return false;
        return topics.stream().anyMatch(topic -> topic.name().equals(topicName));
    }

    // RetryContext is an interface in Spring Retry that provides contextual information about the current retry operation. It is used within the retry logic to access details such as the number of retries attempted, the exception that caused the retry, and other metadata. This context is available during each attempt of the retry and helps in making decisions or logging relevant information.
    private CreateTopicsResult doCreateTopics(RetryContext retryContext) {
        List<String> topicNames = kafkaConfigData.getTopicNamesToCreate();
        LOG.info("Creating {} topics(s), attempt {}", topicNames.size(), retryContext.getRetryCount());
        List<NewTopic> kafkaTopics =  topicNames.stream().map(topic -> new NewTopic(
                topic.trim(),
                kafkaConfigData.getNumOfPartitions(),
                kafkaConfigData.getReplicationFactor()
        )).collect(Collectors.toList());
        return adminClient.createTopics(kafkaTopics);
    }

    private Collection<TopicListing> getTopics(){
        Collection<TopicListing> topics;
        try {
            topics = retryTemplate.execute(this::doGetTopics);
        } catch (Throwable t) {
            throw new KafkaClientException("Reached max number of retry for reading kafka topic(s)!", t);
        }
        return topics;
    }

    private Collection<TopicListing> doGetTopics(RetryContext retryContext) {
        LOG.info("Reading kafka topics {}, attempt {}",
                kafkaConfigData.getTopicNamesToCreate().toArray(), retryContext.getRetryCount());
        Collection<TopicListing> topics = null;
        try {
            topics = adminClient.listTopics().listings().get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        if(topics != null){
            topics.forEach(topic -> LOG.debug("Topic with name {}", topic.name()));
        }
        return topics;
    }


}
