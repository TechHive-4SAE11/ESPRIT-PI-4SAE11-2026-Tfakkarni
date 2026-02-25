package org.techhive.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakUserService {

  private final RestTemplate restTemplate;
  private final KeycloakAdminConfig keycloakConfig;
  private final UserRepository userRepository;

  /**
   * Register a new user in Keycloak under the configured realm.
   * All Keycloak Admin REST API calls happen server-side — no CORS issues.
   */
  public void registerUser(RegisterRequest request) {
    String adminToken = getAdminToken();

    // 1. Create the user in Keycloak
    createKeycloakUser(adminToken, request);

    // 2. Find the created user by email to get the Keycloak ID
    String keycloakId = findUserByEmail(adminToken, request.getEmail());

    // 3. Assign the chosen role in Keycloak
    assignRole(adminToken, keycloakId, request.getRole());

    // 4. Save user to local PostgreSQL database
    User user = new User(keycloakId, request.getFirstName(), request.getLastName(),
        request.getEmail(), request.getRole(), request.getGender());
    userRepository.save(user);

    log.info("User '{}' registered successfully with role '{}', keycloakId='{}'",
        request.getEmail(), request.getRole(), keycloakId);
  }

  private String getAdminToken() {
    String tokenUrl = keycloakConfig.getServerUrl() + "/realms/master/protocol/openid-connect/token";
    log.debug("Getting admin token from: {}", tokenUrl);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("grant_type", "password");
    body.add("client_id", "admin-cli");
    body.add("username", keycloakConfig.getAdminUsername());
    body.add("password", keycloakConfig.getAdminPassword());

    ResponseEntity<Map> response = restTemplate.exchange(
        tokenUrl, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

    String token = (String) Objects.requireNonNull(response.getBody()).get("access_token");
    log.debug("Admin token obtained successfully");
    return token;
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

    log.debug("Creating user '{}' at: {}", request.getEmail(), usersUrl);
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

    // Get the role representation
    String roleUrl = baseUrl + "/roles/" + roleName;
    ResponseEntity<Map> roleResponse = restTemplate.exchange(
        roleUrl, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    Map<String, Object> role = roleResponse.getBody();

    // Assign the role to the user
    String roleMappingUrl = baseUrl + "/users/" + userId + "/role-mappings/realm";
    restTemplate.exchange(roleMappingUrl, HttpMethod.POST,
        new HttpEntity<>(List.of(role), headers), Void.class);

    log.debug("Role '{}' assigned to user '{}'", roleName, userId);
  }
}
