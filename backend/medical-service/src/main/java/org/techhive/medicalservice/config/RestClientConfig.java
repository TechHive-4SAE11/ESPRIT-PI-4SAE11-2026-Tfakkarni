package org.techhive.medicalservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${alert-service.url:http://localhost:18084}")
    private String alertServiceUrl;

    @Value("${user-service.url:http://localhost:18081}")
    private String userServiceUrl;

    @Bean
    public RestClient alertServiceRestClient(RestClient.Builder builder) {
        return builder
                .baseUrl(alertServiceUrl)
                .build();
    }

    @Bean
    public RestClient userServiceRestClient(RestClient.Builder builder) {
        return builder
                .baseUrl(userServiceUrl)
                .build();
    }
}
