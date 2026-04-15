package org.techhive.iotservice.client;

import org.springframework.stereotype.Component;
import org.techhive.iotservice.dto.FeatureGateResponse;

@Component
public class AnalyticsServiceClientFallback implements AnalyticsServiceClient {

    @Override
    public FeatureGateResponse getFeatureGates(String patientKeycloakId) {
        // Fail-open: if analytics-service is down, allow IoT access
        return FeatureGateResponse.builder()
                .patientKeycloakId(patientKeycloakId)
                .iotEnabled(true)
                .stage("UNKNOWN")
                .iotLevel("FULL")
                .build();
    }
}
