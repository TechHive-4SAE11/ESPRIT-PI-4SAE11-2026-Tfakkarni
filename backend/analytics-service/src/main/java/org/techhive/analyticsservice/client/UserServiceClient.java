package org.techhive.analyticsservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(name = "user-service", fallback = UserServiceClientFallback.class)
public interface UserServiceClient {

    @GetMapping("/api/users/keycloak/{keycloakId}")
    Map<String, Object> getUserByKeycloakId(@PathVariable("keycloakId") String keycloakId);

    @GetMapping("/api/users/role/{role}")
    List<Map<String, Object>> getUsersByRole(@PathVariable("role") String role);
}
