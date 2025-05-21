package com.erss.order.config;

// src/main/java/com/erss/orderservice/config/RestConfig.java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
@Configuration
public class RestConfig {
    @Bean public RestTemplate restTemplate(){

        return new RestTemplate();
    }
}

