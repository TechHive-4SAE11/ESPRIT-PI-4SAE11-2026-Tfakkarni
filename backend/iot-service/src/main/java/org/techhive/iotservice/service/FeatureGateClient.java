package org.techhive.iotservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.techhive.iotservice.client.AnalyticsServiceClient;
import org.techhive.iotservice.dto.FeatureGateResponse;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Checks whether IoT features are enabled for a patient by calling
 * the analytics-service feature-gates endpoint via OpenFeign.
 * Results are cached for 5 minutes to avoid excessive inter-service calls.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureGateClient {

    private final AnalyticsServiceClient analyticsServiceClient;

    private static final long CACHE_TTL_MINUTES = 5;
    private final ConcurrentHashMap<String, CachedResult> cache = new ConcurrentHashMap<>();

    /**
     * Returns true if IoT is enabled for the given patient.
     * Falls back to true on errors (fail-open) so the service degrades gracefully.
     */
    public boolean isIotEnabled(String patientKeycloakId) {
        CachedResult cached = cache.get(patientKeycloakId);
        if (cached != null && cached.expiresAt.isAfter(LocalDateTime.now())) {
            return cached.iotEnabled;
        }

        try {
            FeatureGateResponse response = analyticsServiceClient.getFeatureGates(patientKeycloakId);
            boolean enabled = response.isIotEnabled();
            cache.put(patientKeycloakId, new CachedResult(enabled, LocalDateTime.now().plusMinutes(CACHE_TTL_MINUTES)));
            log.debug("Feature gate for {}: iotEnabled={}", patientKeycloakId, enabled);
            return enabled;
        } catch (Exception e) {
            log.warn("Failed to check feature gate for patient {}: {}", patientKeycloakId, e.getMessage());
            return true; // fail-open: don't block IoT if analytics-service is down
        }
    }

    private record CachedResult(boolean iotEnabled, LocalDateTime expiresAt) {}
}
