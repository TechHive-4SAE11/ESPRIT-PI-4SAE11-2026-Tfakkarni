package org.techhive.userservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.techhive.userservice.dto.RegisterRequest;
import org.techhive.userservice.service.KeycloakUserService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final KeycloakUserService keycloakUserService;

  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
    log.info("Registration request received for email: '{}', role: '{}'",
        request.getEmail(), request.getRole());

    try {
      keycloakUserService.registerUser(request);
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(Map.of("message", "User registered successfully"));
    } catch (HttpClientErrorException.Conflict e) {
      log.warn("User already exists: {}", request.getEmail());
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(Map.of("error", "User already exists with this username or email"));
    } catch (HttpClientErrorException e) {
      log.error("Keycloak error during registration: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
      return ResponseEntity.status(e.getStatusCode())
          .body(Map.of("error", "Registration failed: " + e.getResponseBodyAsString()));
    } catch (Exception e) {
      log.error("Unexpected error during registration", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Registration failed: " + e.getMessage()));
    }
  }
}
