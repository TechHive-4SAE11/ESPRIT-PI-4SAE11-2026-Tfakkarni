package org.techhive.analyticsservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(name = "medical-service", fallback = MedicalServiceClientFallback.class)
public interface MedicalServiceClient {

    @GetMapping("/api/medical-folders/patient/{patientId}")
    List<Map<String, Object>> getMedicalFoldersByPatient(@PathVariable("patientId") String patientId);

    @GetMapping("/api/diagnostics/folder/{folderId}")
    List<Map<String, Object>> getDiagnosticsByFolder(@PathVariable("folderId") Long folderId);

    @GetMapping("/api/medical/appointments/patient/{patientId}")
    List<Map<String, Object>> getAppointmentsByPatient(@PathVariable("patientId") String patientId);

    @GetMapping("/api/medical-folders/{folderId}/coaching-goals")
    List<Map<String, Object>> getCoachingGoals(@PathVariable("folderId") Long folderId);

    @GetMapping("/api/medical-folders/doctor/{doctorId}")
    List<Map<String, Object>> getMedicalFoldersByDoctor(@PathVariable("doctorId") String doctorId);
}
