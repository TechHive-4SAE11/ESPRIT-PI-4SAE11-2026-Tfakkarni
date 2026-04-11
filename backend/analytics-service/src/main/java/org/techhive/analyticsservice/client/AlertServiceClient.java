package org.techhive.analyticsservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(name = "alert-service", fallback = AlertServiceClientFallback.class)
public interface AlertServiceClient {

    @GetMapping("/api/alerts/geofence-violations/{patientId}")
    List<Map<String, Object>> getGeofenceViolations(@PathVariable("patientId") String patientId);

    @GetMapping("/api/alerts/geofence-violations/{patientId}/unacknowledged")
    List<Map<String, Object>> getUnacknowledgedViolations(@PathVariable("patientId") String patientId);
}
