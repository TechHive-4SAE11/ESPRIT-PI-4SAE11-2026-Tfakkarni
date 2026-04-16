package org.techhive.medicalservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.techhive.medicalservice.dto.audit.PatientMedicationAuditRequest;
import org.techhive.medicalservice.dto.audit.PatientMedicationAuditResponse;
import org.techhive.medicalservice.dto.tracking.MedicationComplianceDTO;
import org.techhive.medicalservice.dto.tracking.IncidentStatsDTO;

import java.util.List;

@FeignClient(name = "tracking-service")
public interface TrackingServiceClient {

    @GetMapping("/api/statistics/{patientId}/medication-compliance?days=30")
    MedicationComplianceDTO getPatientMedicationCompliance(@PathVariable("patientId") String patientId);

    @GetMapping("/api/statistics/{patientId}/medication-compliance-by-drug")
    List<MedicationComplianceDTO> getPatientMedicationComplianceByDrug(@PathVariable("patientId") String patientId);

    @GetMapping("/api/statistics/{patientId}/incident-types?days=30")
    IncidentStatsDTO getPatientIncidentStats(@PathVariable("patientId") String patientId);

    @PostMapping("/api/analytics/safety-audit/patient-medications")
    PatientMedicationAuditResponse getPatientMedications(@RequestBody PatientMedicationAuditRequest request);

    @GetMapping("/api/medical-folders/patient/{patientId}")
    JsonNode getMedicalFoldersByPatientId(@PathVariable("patientId") String patientId);

    @GetMapping("/api/sessions/medical-folder/{folderId}")
    JsonNode getSessionsByFolderId(@PathVariable("folderId") Long folderId);

    @GetMapping("/api/prescriptions/session/{sessionId}")
    JsonNode getPrescriptionsBySessionId(@PathVariable("sessionId") Long sessionId);
}
