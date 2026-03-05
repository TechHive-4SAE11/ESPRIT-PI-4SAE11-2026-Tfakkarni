package org.techhive.trackingservice.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    /** Load-balanced RestTemplate for inter-service calls (lb://user-service, etc.) */
    @Bean
    @Qualifier("lbRestTemplate")
    @LoadBalanced
    public RestTemplate lbRestTemplate() {
        return new RestTemplate();
    }

    /** Plain RestTemplate for external HTTP calls (Mailtrap, etc.) */
    @Bean
    @Qualifier("plainRestTemplate")
    public RestTemplate plainRestTemplate() {
        return new RestTemplate();
    }
}
