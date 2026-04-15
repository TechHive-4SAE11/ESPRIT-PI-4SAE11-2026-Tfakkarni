package org.techhive.medicalservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.techhive.medicalservice.config.GeminiSafetyAuditProperties;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(GeminiSafetyAuditProperties.class)
public class MedicalServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(MedicalServiceApplication.class, args);
	}
}
