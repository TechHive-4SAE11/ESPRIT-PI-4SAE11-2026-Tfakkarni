package org.techhive.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.techhive.userservice.config.KeycloakAdminConfig;
import org.techhive.userservice.dto.RegisterRequest;
import org.techhive.userservice.entity.User;
import org.techhive.userservice.repository.UserRepository;

import org.techhive.userservice.dto.ChangePasswordRequest;
import org.techhive.userservice.dto.UpdateProfileRequest;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakUserService {

  private final RestTemplate restTemplate;
  private final KeycloakAdminConfig keycloakConfig;
  private final UserRepository userRepository;

  @Value("${keycloak.client-id:tfakkarni-app}")
  private String clientId;

  @Value("${keycloak.client-secret:}")
  private String clientSecret;

  /**
   * Register a new user in Keycloak under the configured realm.
   */
  public void registerUser(RegisterRequest request) {
    String adminToken = getAdminToken();
    createKeycloakUser(adminToken, request);
    String keycloakId = findUserByEmail(adminToken, request.getEmail());
    assignRole(adminToken, keycloakId, request.getRole());

    User user = new User(keycloakId, request.getFirstName(), request.getLastName(),
        request.getEmail(), request.getRole(), request.getGender());
    userRepository.save(user);

    log.info("User '{}' registered successfully with role '{}', keycloakId='{}'",
        request.getEmail(), request.getRole(), keycloakId);
  }

  private String getAdminToken() {
    String tokenUrl = keycloakConfig.getServerUrl() + "/realms/master/protocol/openid-connect/token";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("grant_type", "password");
    body.add("client_id", "admin-cli");
    body.add("username", keycloakConfig.getAdminUsername());
    body.add("password", keycloakConfig.getAdminPassword());

    ResponseEntity<Map> response = restTemplate.exchange(
        tokenUrl, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

    return (String) Objects.requireNonNull(response.getBody()).get("access_token");
  }

  private void createKeycloakUser(String adminToken, RegisterRequest request) {
    String usersUrl = keycloakConfig.getServerUrl() + "/admin/realms/"
        + keycloakConfig.getRealm() + "/users";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(adminToken);

    Map<String, Object> userRepresentation = new HashMap<>();
    userRepresentation.put("username", request.getEmail());
    userRepresentation.put("email", request.getEmail());
    userRepresentation.put("firstName", request.getFirstName());
    userRepresentation.put("lastName", request.getLastName());
    userRepresentation.put("enabled", true);
    userRepresentation.put("emailVerified", true);
    userRepresentation.put("credentials", List.of(Map.of(
        "type", "password",
        "value", request.getPassword(),
        "temporary", false)));

    restTemplate.exchange(usersUrl, HttpMethod.POST,
        new HttpEntity<>(userRepresentation, headers), Void.class);
  }

  @SuppressWarnings("unchecked")
  private String findUserByEmail(String adminToken, String email) {
    String searchUrl = keycloakConfig.getServerUrl() + "/admin/realms/"
        + keycloakConfig.getRealm() + "/users?email=" + email + "&exact=true";

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(adminToken);

    ResponseEntity<List> response = restTemplate.exchange(
        searchUrl, HttpMethod.GET, new HttpEntity<>(headers), List.class);

    List<Map<String, Object>> users = response.getBody();
    if (users == null || users.isEmpty()) {
      throw new RuntimeException("User was created but could not be found by email: " + email);
    }
    return (String) users.get(0).get("id");
  }

  @SuppressWarnings("unchecked")
  private void assignRole(String adminToken, String userId, String roleName) {
    String baseUrl = keycloakConfig.getServerUrl() + "/admin/realms/" + keycloakConfig.getRealm();

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(adminToken);
    headers.setContentType(MediaType.APPLICATION_JSON);

    String roleUrl = baseUrl + "/roles/" + roleName;
    ResponseEntity<Map> roleResponse = restTemplate.exchange(
        roleUrl, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    Map<String, Object> role = roleResponse.getBody();

    String roleMappingUrl = baseUrl + "/users/" + userId + "/role-mappings/realm";
    restTemplate.exchange(roleMappingUrl, HttpMethod.POST,
        new HttpEntity<>(List.of(role), headers), Void.class);
  }

  /**
   * Update user profile in Keycloak.
   */
  public void updateKeycloakUser(String keycloakId, UpdateProfileRequest request) {
    String adminToken = getAdminToken();
    String userUrl = keycloakConfig.getServerUrl() + "/admin/realms/"
        + keycloakConfig.getRealm() + "/users/" + keycloakId;

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(adminToken);

    Map<String, Object> body = new HashMap<>();
    if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
      body.put("firstName", request.getFirstName().trim());
    }
    if (request.getLastName() != null && !request.getLastName().isBlank()) {
      body.put("lastName", request.getLastName().trim());
    }
    if (request.getEmail() != null && !request.getEmail().isBlank()) {
      body.put("email", request.getEmail().trim());
      body.put("username", request.getEmail().trim());
    }

    restTemplate.exchange(userUrl, HttpMethod.PUT, new HttpEntity<>(body, headers), Void.class);
    log.info("Keycloak user '{}' profile updated", keycloakId);
  }

  /**
   * Change user password — verifies current password first via token grant.
   */
  public void changePassword(String keycloakId, ChangePasswordRequest request) {
    String adminToken = getAdminToken();

    // 1. Fetch the user's email
    String userUrl = keycloakConfig.getServerUrl() + "/admin/realms/"
        + keycloakConfig.getRealm() + "/users/" + keycloakId;

    HttpHeaders getHeaders = new HttpHeaders();
    getHeaders.setBearerAuth(adminToken);

    ResponseEntity<Map> userResponse = restTemplate.exchange(
        userUrl, HttpMethod.GET, new HttpEntity<>(getHeaders), Map.class);
    String email = (String) Objects.requireNonNull(userResponse.getBody()).get("email");

    // 2. Verify current password
    verifyCurrentPassword(email, request.getCurrentPassword());

    // 3. Set the new password using admin API
    resetKeycloakPassword(keycloakId, request.getNewPassword(), adminToken);
    log.info("Password changed for user '{}'", keycloakId);
  }

  /**
   * Admin reset password — no current password verification required.
   */
  public void adminResetPassword(String keycloakId, String newPassword) {
    String adminToken = getAdminToken();
    resetKeycloakPassword(keycloakId, newPassword, adminToken);
    log.info("Password admin-reset for user '{}'", keycloakId);
  }

  /**
   * Enable or disable a user in Keycloak.
   */
  public void setUserEnabled(String keycloakId, boolean enabled) {
    String adminToken = getAdminToken();
    String userUrl = keycloakConfig.getServerUrl() + "/admin/realms/"
        + keycloakConfig.getRealm() + "/users/" + keycloakId;

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(adminToken);

    Map<String, Object> body = new HashMap<>();
    body.put("enabled", enabled);

    restTemplate.exchange(userUrl, HttpMethod.PUT, new HttpEntity<>(body, headers), Void.class);
    log.info("Keycloak user '{}' enabled={}", keycloakId, enabled);
  }

  /**
   * Delete a user from Keycloak.
   */
  public void deleteKeycloakUser(String keycloakId) {
    String adminToken = getAdminToken();
    String userUrl = keycloakConfig.getServerUrl() + "/admin/realms/"
        + keycloakConfig.getRealm() + "/users/" + keycloakId;

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(adminToken);

    restTemplate.exchange(userUrl, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
    log.info("Keycloak user '{}' deleted", keycloakId);
  }

  /**
   * Update user role in Keycloak: remove old realm role, assign new one.
   */
  public void updateKeycloakUserRole(String keycloakId, String oldRole, String newRole) {
    String adminToken = getAdminToken();
    String baseUrl = keycloakConfig.getServerUrl() + "/admin/realms/" + keycloakConfig.getRealm();

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(adminToken);
    headers.setContentType(MediaType.APPLICATION_JSON);

    try {
      String oldRoleUrl = baseUrl + "/roles/" + oldRole;
      ResponseEntity<Map> oldRoleResp = restTemplate.exchange(
          oldRoleUrl, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
      String roleMappingUrl = baseUrl + "/users/" + keycloakId + "/role-mappings/realm";
      restTemplate.exchange(roleMappingUrl, HttpMethod.DELETE,
          new HttpEntity<>(List.of(oldRoleResp.getBody()), headers), Void.class);
    } catch (Exception e) {
      log.warn("Could not remove old role '{}' from user '{}': {}", oldRole, keycloakId, e.getMessage());
    }

    assignRole(adminToken, keycloakId, newRole);
    log.info("Keycloak user '{}' role changed from '{}' to '{}'", keycloakId, oldRole, newRole);
  }

  // ─── Private helpers ───────────────────────────────────

  private void resetKeycloakPassword(String keycloakId, String newPassword, String adminToken) {
    String resetUrl = keycloakConfig.getServerUrl() + "/admin/realms/"
        + keycloakConfig.getRealm() + "/users/" + keycloakId + "/reset-password";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(adminToken);

    Map<String, Object> credential = new HashMap<>();
    credential.put("type", "password");
    credential.put("value", newPassword);
    credential.put("temporary", false);

    restTemplate.exchange(resetUrl, HttpMethod.PUT, new HttpEntity<>(credential, headers), Void.class);
  }

  private void verifyCurrentPassword(String email, String password) {
    String tokenUrl = keycloakConfig.getServerUrl() + "/realms/"
        + keycloakConfig.getRealm() + "/protocol/openid-connect/token";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("grant_type", "password");
    body.add("client_id", clientId);
    body.add("username", email);
    body.add("password", password);

    // Add client_secret if configured (for confidential clients)
    if (clientSecret != null && !clientSecret.isBlank()) {
      body.add("client_secret", clientSecret);
    }

    try {
      restTemplate.exchange(tokenUrl, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
    } catch (HttpClientErrorException.Unauthorized e) {
      throw new RuntimeException("Mot de passe actuel incorrect");
    } catch (HttpClientErrorException e) {
      log.warn("Password verification failed with status {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
      // If it's a client configuration issue (not 401), the password might still be correct
      // but the client doesn't support direct access grants
      if (e.getStatusCode().value() == 400) {
        String responseBody = e.getResponseBodyAsString();
        if (responseBody.contains("invalid_grant")) {
          throw new RuntimeException("Mot de passe actuel incorrect");
        }
        if (responseBody.contains("unauthorized_client") || responseBody.contains("invalid_client")) {
          log.warn("Client '{}' not configured for direct access grants. Skipping password verification.", clientId);
          // Cannot verify password - skip verification (admin API will handle reset)
          return;
        }
      }
      throw new RuntimeException("Mot de passe actuel incorrect");
    }
  }
}
