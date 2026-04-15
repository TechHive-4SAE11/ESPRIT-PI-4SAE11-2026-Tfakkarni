package org.techhive.analyticsservice.client;

import org.springframework.stereotype.Component;
import org.techhive.analyticsservice.dto.PrescriptionResponseDTO;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class TrackingServiceClientFallback implements TrackingServiceClient {

    @Override
    public List<PrescriptionResponseDTO> getPrescriptionsByPatient(String idPatient) {
        return Collections.emptyList();
    }

    @Override
    public Map<String, Object> getMedicationCompliance(String patientId, int days) {
        return Map.of("taken", 0, "missed", 0);
    }

    @Override
    public Map<String, Object> getIncidentTypes(String patientId, int days) {
        return Map.of("labels", Collections.emptyList(), "values", Collections.emptyList());
    }

    @Override
    public Map<String, Object> getStreak(String patientId) {
        return Map.of("currentStreak", 0);
    }

    @Override
    public Map<String, Object> getDailyLog(String patientId, String date) {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Object> getHealthScore(String patientId, String date) {
        return Collections.emptyMap();
    }

    @Override
    public List<Map<String, Object>> getDoctorRatingsRanking() {
        return Collections.emptyList();
    }

    @Override
    public List<Map<String, Object>> getRatingsForDoctor(String doctorKeycloakId) {
        return Collections.emptyList();
    }
}
