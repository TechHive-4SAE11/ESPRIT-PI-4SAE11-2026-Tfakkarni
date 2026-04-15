package org.techhive.gameservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.techhive.gameservice.client.AnalyticsServiceClient;
import org.techhive.gameservice.client.UserServiceClient;
import org.techhive.gameservice.dto.FeatureGateResponse;
import org.techhive.gameservice.dto.UserResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientContextService {

    private final AnalyticsServiceClient analyticsServiceClient;
    private final UserServiceClient userServiceClient;

    /**
     * Returns the game complexity level for a patient.
     * STANDARD = full options, SIMPLIFIED = fewer distractors, MINIMAL = 2 choices.
     */
    public String getGameComplexity(String keycloakId) {
        try {
            FeatureGateResponse gates = analyticsServiceClient.getFeatureGates(keycloakId);
            String complexity = gates.getGameComplexity();
            log.debug("Game complexity for {}: {}", keycloakId, complexity);
            return complexity != null ? complexity : "STANDARD";
        } catch (Exception e) {
            log.warn("Failed to fetch game complexity for {}: {}", keycloakId, e.getMessage());
            return "STANDARD";
        }
    }

    /**
     * Returns the patient's user info, or null if user-service is unavailable.
     */
    public UserResponse getPatientInfo(String keycloakId) {
        try {
            return userServiceClient.getUserByKeycloakId(keycloakId);
        } catch (Exception e) {
            log.warn("Failed to fetch patient info for {}: {}", keycloakId, e.getMessage());
            return null;
        }
    }

    /**
     * Returns the number of answer options to present based on game complexity.
     * STANDARD=4, SIMPLIFIED=3, MINIMAL=2
     */
    public int getOptionCount(String keycloakId) {
        String complexity = getGameComplexity(keycloakId);
        return switch (complexity) {
            case "MINIMAL" -> 2;
            case "SIMPLIFIED" -> 3;
            default -> 4;
        };
    }
}
