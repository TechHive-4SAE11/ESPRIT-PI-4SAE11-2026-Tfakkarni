package org.techhive.userservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.techhive.userservice.dto.RegisterRequest;
import org.techhive.userservice.entity.User;
import org.techhive.userservice.service.KeycloakUserService;
import org.techhive.userservice.service.UserService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final KeycloakUserService keycloakUserService;
  private final UserService userService;

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

  /**
   * List all users (admin).
   */
  @GetMapping
  public ResponseEntity<?> getAllUsers() {
    try {
      return ResponseEntity.ok(userService.getAllUsers());
    } catch (Exception e) {
      log.error("Error fetching all users", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Failed to fetch users: " + e.getMessage()));
    }
  }

  /**
   * List users by role (e.g., /api/users/role/patient).
   */
  @GetMapping("/role/{role}")
  public ResponseEntity<?> getUsersByRole(@PathVariable String role) {
    try {
      log.info("Fetching users with role: {}", role);
      List<User> users = userService.getUsersByRole(role);
      log.info("Found {} users with role: {}", users.size(), role);
      return ResponseEntity.ok(users);
    } catch (Exception e) {
      log.error("Error fetching users by role: {}", role, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Failed to fetch users: " + e.getMessage()));
    }
  }

  /**
   * Get a user by their Keycloak ID.
   */
  @GetMapping("/keycloak/{keycloakId}")
  public ResponseEntity<?> getUserByKeycloakId(@PathVariable String keycloakId) {
    try {
      return userService.getUserByKeycloakId(keycloakId)
          .<ResponseEntity<?>>map(ResponseEntity::ok)
          .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
              .body(Map.of("error", "User not found")));
    } catch (Exception e) {
      log.error("Error fetching user by keycloakId: {}", keycloakId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Failed to fetch user: " + e.getMessage()));
    }
  }
}
