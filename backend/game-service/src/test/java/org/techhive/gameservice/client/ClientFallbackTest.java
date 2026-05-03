package org.techhive.gameservice.client;

import org.junit.jupiter.api.Test;
import org.techhive.gameservice.dto.FeatureGateResponse;

import static org.junit.jupiter.api.Assertions.*;

class ClientFallbackTest {

    @Test
    void userServiceFallbackReturnsNullWhenUserServiceIsUnavailable() {
        UserServiceClientFallback fallback = new UserServiceClientFallback();

        assertNull(fallback.getUserByKeycloakId("patient-1"));
    }

    @Test
    void analyticsFallbackReturnsStandardUnknownGateForPatient() {
        AnalyticsServiceClientFallback fallback = new AnalyticsServiceClientFallback();

        FeatureGateResponse response = fallback.getFeatureGates("patient-1");

        assertEquals("patient-1", response.getPatientKeycloakId());
        assertEquals("STANDARD", response.getGameComplexity());
        assertEquals("UNKNOWN", response.getStage());
    }
}
