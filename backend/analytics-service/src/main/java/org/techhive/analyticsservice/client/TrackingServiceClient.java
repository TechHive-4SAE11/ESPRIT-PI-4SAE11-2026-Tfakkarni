package org.techhive.analyticsservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "tracking-service", fallback = TrackingServiceClientFallback.class)
public interface TrackingServiceClient {

    @GetMapping("/api/statistics/{patientId}/medication-compliance")
    Map<String, Object> getMedicationCompliance(
            @PathVariable("patientId") String patientId,
            @RequestParam("days") int days);

    @GetMapping("/api/statistics/{patientId}/incident-types")
    Map<String, Object> getIncidentTypes(
            @PathVariable("patientId") String patientId,
            @RequestParam("days") int days);

    @GetMapping("/api/statistics/{patientId}/streak")
    Map<String, Object> getStreak(@PathVariable("patientId") String patientId);

    @GetMapping("/api/daily-monitoring/{patientId}")
    Map<String, Object> getDailyLog(
            @PathVariable("patientId") String patientId,
            @RequestParam("date") String date);

    @GetMapping("/api/health-score/{patientId}")
    Map<String, Object> getHealthScore(
            @PathVariable("patientId") String patientId,
            @RequestParam("date") String date);
}
