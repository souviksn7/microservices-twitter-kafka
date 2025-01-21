package com.microservices.demo.config.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

 // It is an annotation in spring cloud that marks a Spring Boot application as a config server. Config server is a central place for managing all configuration properties for application across all environments
@EnableConfigServer
@SpringBootApplication
public class ConfigServer {
    // If we run config server after that twitter-to-kafka service, we can check that the configuration is really fetched remotely from config-server
    public static void main(String[] args){
        SpringApplication.run(ConfigServer.class, args);
    }
}
