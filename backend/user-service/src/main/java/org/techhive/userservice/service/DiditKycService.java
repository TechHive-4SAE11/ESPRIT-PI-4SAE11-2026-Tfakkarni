package org.techhive.userservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.techhive.userservice.entity.User;
import org.techhive.userservice.repository.UserRepository;

import java.util.Map;

@Slf4j
@Service
public class DiditKycService {

  private static final String DIDIT_BASE_URL = "https://verification.didit.me";

  private final WebClient webClient;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;

  @Value("${didit.api-key:}")
  private String apiKey;

  @Value("${didit.workflow-id:}")
  private String workflowId;

  public DiditKycService(UserRepository userRepository, ObjectMapper objectMapper) {
    this.userRepository = userRepository;
    this.objectMapper = objectMapper;
    this.webClient = WebClient.builder()
        .baseUrl(DIDIT_BASE_URL)
        .build();
  }

  /**
   * Creates a Didit KYC session for the given user.
   * Returns a Map with "session_id", "url", and "status".
   */
  public Map<String, String> createSession(String keycloakId) {
    User user = userRepository.findByKeycloakId(keycloakId)
        .orElseThrow(() -> new RuntimeException("User not found with keycloakId: " + keycloakId));

    if (!"doctor".equalsIgnoreCase(user.getRole())) {
      throw new RuntimeException("KYC is only required for doctors");
    }

    log.info("[KYC] Creating Didit session for user: {} ({})", user.getEmail(), keycloakId);

    try {
      String responseBody = webClient.post()
          .uri("/v3/session/")
          .header("x-api-key", apiKey)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(Map.of(
              "workflow_id", workflowId,
              "vendor_data", keycloakId))
          .retrieve()
          .bodyToMono(String.class)
          .block();

      JsonNode json = objectMapper.readTree(responseBody);
      String sessionId = json.get("session_id").asText();
      String verificationUrl = json.get("url").asText();
      String status = json.has("status") ? json.get("status").asText() : "Not Started";

      // Save session ID and update KYC status
      user.setKycSessionId(sessionId);
      user.setKycStatus("pending");
      userRepository.save(user);

      log.info("[KYC] Session created: sessionId={}, url={}", sessionId, verificationUrl);

      return Map.of(
          "session_id", sessionId,
          "url", verificationUrl,
          "status", status);
    } catch (Exception e) {
      log.error("[KYC] Failed to create Didit session for user: {}", keycloakId, e);
      throw new RuntimeException("Failed to create KYC session: " + e.getMessage());
    }
  }

  /**
   * Retrieves the current KYC session status from Didit and updates the user
   * record.
   * Returns a Map with "kyc_status", "session_id", and "didit_status".
   */
  public Map<String, String> getSessionStatus(String keycloakId) {
    User user = userRepository.findByKeycloakId(keycloakId)
        .orElseThrow(() -> new RuntimeException("User not found with keycloakId: " + keycloakId));

    if (user.getKycSessionId() == null || user.getKycSessionId().isBlank()) {
      return Map.of(
          "kyc_status", user.getKycStatus(),
          "session_id", "",
          "didit_status", "No session");
    }

    log.info("[KYC] Checking status for session: {}", user.getKycSessionId());

    try {
      String responseBody = webClient.get()
          .uri("/v3/session/{sessionId}/decision/", user.getKycSessionId())
          .header("x-api-key", apiKey)
          .retrieve()
          .bodyToMono(String.class)
          .block();

      JsonNode json = objectMapper.readTree(responseBody);
      String diditStatus = json.has("status") ? json.get("status").asText() : "unknown";

      // Map Didit status to our internal status
      String internalStatus = mapDiditStatus(diditStatus);
      user.setKycStatus(internalStatus);
      userRepository.save(user);

      log.info("[KYC] Status for {}: didit={}, internal={}", keycloakId, diditStatus, internalStatus);

      return Map.of(
          "kyc_status", internalStatus,
          "session_id", user.getKycSessionId(),
          "didit_status", diditStatus);
    } catch (Exception e) {
      log.error("[KYC] Failed to retrieve session status for user: {}", keycloakId, e);
      return Map.of(
          "kyc_status", user.getKycStatus(),
          "session_id", user.getKycSessionId(),
          "didit_status", "error");
    }
  }

  /**
   * Skip KYC verification (dev/testing only).
   */
  public User skipKyc(String keycloakId) {
    User user = userRepository.findByKeycloakId(keycloakId)
        .orElseThrow(() -> new RuntimeException("User not found with keycloakId: " + keycloakId));

    log.warn("[KYC] Skipping KYC for user: {} (DEV MODE)", keycloakId);
    user.setKycStatus("skipped");
    return userRepository.save(user);
  }

  /**
   * Maps Didit verification statuses to internal status values.
   * Didit statuses: "Not Started", "In Progress", "Approved", "Declined",
   * "In Review", "Resubmitted", "Expired", "Abandoned", "Kyc Expired"
   */
  private String mapDiditStatus(String diditStatus) {
    if (diditStatus == null)
      return "pending";
    return switch (diditStatus) {
      case "Approved" -> "approved";
      case "Declined" -> "declined";
      case "In Progress", "In Review", "Resubmitted", "Not Started" -> "pending";
      case "Expired", "Abandoned", "Kyc Expired" -> "expired";
      default -> "pending";
    };
  }
}
