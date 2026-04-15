package org.techhive.medicalservice.client;

<<<<<<< Updated upstream
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.cloud.openfeign.FeignClient;
import java.util.List;
import org.techhive.medicalservice.dto.tracking.TrackingSummaryDTO;

@FeignClient(name = "tracking-service", path = "/api/tracking")
public interface TrackingServiceClient {

    @GetMapping("/logs/patient/{patientId}/summary")
    TrackingSummaryDTO getPatientTrackingSummary(@PathVariable("patientId") String patientId);
}

=======
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.techhive.medicalservice.dto.audit.PatientMedicationAuditRequest;
import org.techhive.medicalservice.dto.audit.PatientMedicationAuditResponse;

@FeignClient(name = "tracking-service")
public interface TrackingServiceClient {

    @PostMapping("/api/analytics/safety-audit/patient-medications")
    PatientMedicationAuditResponse getPatientMedications(@RequestBody PatientMedicationAuditRequest request);

    @GetMapping("/api/medical-folders/patient/{patientId}")
    JsonNode getMedicalFoldersByPatientId(@PathVariable("patientId") String patientId);

    @GetMapping("/api/sessions/medical-folder/{folderId}")
    JsonNode getSessionsByFolderId(@PathVariable("folderId") Long folderId);

    @GetMapping("/api/prescriptions/session/{sessionId}")
    JsonNode getPrescriptionsBySessionId(@PathVariable("sessionId") Long sessionId);
}
>>>>>>> Stashed changes
