package org.techhive.analyticsservice.client;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class MedicalServiceClientFallback implements MedicalServiceClient {

    @Override
    public List<Map<String, Object>> getMedicalFoldersByPatient(String patientId) {
        return Collections.emptyList();
    }

    @Override
    public List<Map<String, Object>> getDiagnosticsByFolder(Long folderId) {
        return Collections.emptyList();
    }

    @Override
    public List<Map<String, Object>> getAppointmentsByPatient(String patientId) {
        return Collections.emptyList();
    }

    @Override
    public List<Map<String, Object>> getCoachingGoals(Long folderId) {
        return Collections.emptyList();
    }

    @Override
    public List<Map<String, Object>> getMedicalFoldersByDoctor(String doctorId) {
        return Collections.emptyList();
    }
}
