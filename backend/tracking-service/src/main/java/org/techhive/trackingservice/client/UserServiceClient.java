package org.techhive.trackingservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * Feign client for fetching doctor info and signature from user-service.
 * Uses Eureka service discovery.
 */
@FeignClient(
    name = "user-service",
    fallback = UserServiceClientFallback.class
)
public interface UserServiceClient {

    @GetMapping("/api/users/keycloak/{keycloakId}")
    Map<String, Object> getUserByKeycloakId(@PathVariable("keycloakId") String keycloakId);

    @GetMapping(value = "/api/users/signature/{id}",
                consumes = MediaType.IMAGE_PNG_VALUE)
    byte[] getDoctorSignature(@PathVariable("id") Long id);
}
