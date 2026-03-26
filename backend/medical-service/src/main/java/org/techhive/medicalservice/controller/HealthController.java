package org.techhive.medicalservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/health")
@Slf4j
public class HealthController {

	@GetMapping
	public ResponseEntity<HealthStatus> health() {
		log.info("Health check requested");
		HealthStatus status = HealthStatus.builder()
				.status("UP")
				.service("medical-service")
				.message("Medical service is running")
				.build();
		return ResponseEntity.ok(status);
	}

	public static class HealthStatus {
		public String status;
		public String service;
		public String message;

		public static HealthStatusBuilder builder() {
			return new HealthStatusBuilder();
		}

		public static class HealthStatusBuilder {
			private String status;
			private String service;
			private String message;

			public HealthStatusBuilder status(String status) {
				this.status = status;
				return this;
			}

			public HealthStatusBuilder service(String service) {
				this.service = service;
				return this;
			}

			public HealthStatusBuilder message(String message) {
				this.message = message;
				return this;
			}

			public HealthStatus build() {
				HealthStatus instance = new HealthStatus();
				instance.status = this.status;
				instance.service = this.service;
				instance.message = this.message;
				return instance;
			}
		}
	}
}
