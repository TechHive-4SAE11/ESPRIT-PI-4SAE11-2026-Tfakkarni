package org.techhive.medicalservice.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.annotation.Primary;
@Configuration
public class RestTemplateConfig {

	@Bean
	@Primary
	@LoadBalanced
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}
