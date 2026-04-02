package org.techhive.alertservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${tracking-service.url}")
    private String trackingServiceUrl;

    @Value("${user-service.url}")
    private String userServiceUrl;

    @Bean
    public WebClient trackingServiceClient() {
        return WebClient.builder()
                .baseUrl(trackingServiceUrl)
                .build();
    }

    @Bean
    public WebClient userServiceClient() {
        return WebClient.builder()
                .baseUrl(userServiceUrl)
                .build();
    }
}
