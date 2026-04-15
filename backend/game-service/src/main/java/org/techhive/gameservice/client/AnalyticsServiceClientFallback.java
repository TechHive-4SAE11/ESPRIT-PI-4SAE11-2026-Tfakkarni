package org.techhive.gameservice.client;

import org.springframework.stereotype.Component;
import org.techhive.gameservice.dto.FeatureGateResponse;

@Component
public class AnalyticsServiceClientFallback implements AnalyticsServiceClient {

    @Override
    public FeatureGateResponse getFeatureGates(String patientKeycloakId) {
        return FeatureGateResponse.builder()
                .patientKeycloakId(patientKeycloakId)
                .gameComplexity("STANDARD")
                .stage("UNKNOWN")
                .build();
    }
}
