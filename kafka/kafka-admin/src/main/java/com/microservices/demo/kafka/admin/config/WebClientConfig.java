package com.microservices.demo.kafka.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

// indicates that the class has @Bean methods to create Spring beans
@Configuration
public class WebClientConfig {

    // Spring will create a bean for this method at runtime. And this bean will have the type of the returned object of this method
    @Bean
    WebClient webClient(){
        return WebClient.builder().build();
    }
}
