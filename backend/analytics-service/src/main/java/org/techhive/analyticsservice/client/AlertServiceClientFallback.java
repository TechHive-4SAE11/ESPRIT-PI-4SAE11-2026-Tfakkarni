package org.techhive.analyticsservice.client;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class AlertServiceClientFallback implements AlertServiceClient {

    @Override
    public List<Map<String, Object>> getGeofenceViolations(String patientId) {
        return Collections.emptyList();
    }

    @Override
    public List<Map<String, Object>> getUnacknowledgedViolations(String patientId) {
        return Collections.emptyList();
    }
}
