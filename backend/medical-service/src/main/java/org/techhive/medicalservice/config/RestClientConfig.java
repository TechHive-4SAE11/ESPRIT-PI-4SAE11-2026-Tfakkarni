package org.techhive.medicalservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${alert-service.url:http://localhost:18084}")
    private String alertServiceUrl;

    @Bean
    public RestClient alertServiceRestClient(RestClient.Builder builder) {
        return builder
                .baseUrl(alertServiceUrl)
                .build();
    }
}
