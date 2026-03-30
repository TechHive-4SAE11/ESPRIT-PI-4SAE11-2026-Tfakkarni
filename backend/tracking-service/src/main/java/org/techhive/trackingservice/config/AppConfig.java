package org.techhive.trackingservice.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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

    /** Plain RestTemplate for external HTTP calls (Claude AI, Mailtrap, etc.) */
    @Bean
    @Qualifier("plainRestTemplate")
    public RestTemplate plainRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);  // 10s connect timeout
        factory.setReadTimeout(60_000);     // 60s read timeout (Claude can be slow)
        return new RestTemplate(factory);
    }
}
