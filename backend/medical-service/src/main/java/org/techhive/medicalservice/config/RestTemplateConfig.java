package org.techhive.medicalservice.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.annotation.Primary;
@Configuration
public class RestTemplateConfig {


	/** Plain client for external HTTPS (Open-Meteo, etc.) — not service-discovery. */
	@Bean
	public RestTemplate externalRestTemplate() {
		return new RestTemplate();
	}
}
