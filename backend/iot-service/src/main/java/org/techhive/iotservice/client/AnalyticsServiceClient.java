package org.techhive.iotservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.techhive.iotservice.dto.FeatureGateResponse;

@FeignClient(name = "analytics-service", fallback = AnalyticsServiceClientFallback.class)
public interface AnalyticsServiceClient {

    @GetMapping("/api/analytics/patient/{patientKeycloakId}/feature-gates")
    FeatureGateResponse getFeatureGates(@PathVariable("patientKeycloakId") String patientKeycloakId);
}
