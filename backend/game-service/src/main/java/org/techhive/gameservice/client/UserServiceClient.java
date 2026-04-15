package org.techhive.gameservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.techhive.gameservice.dto.UserResponse;

@FeignClient(name = "user-service", fallback = UserServiceClientFallback.class)
public interface UserServiceClient {

    @GetMapping("/api/users/keycloak/{keycloakId}")
    UserResponse getUserByKeycloakId(@PathVariable("keycloakId") String keycloakId);
}
