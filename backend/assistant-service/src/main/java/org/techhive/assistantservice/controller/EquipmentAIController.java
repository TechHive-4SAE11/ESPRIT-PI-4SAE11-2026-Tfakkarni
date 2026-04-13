package org.techhive.assistantservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.assistantservice.dto.EquipmentRecommendRequest;
import org.techhive.assistantservice.dto.EquipmentRecommendResponse;
import org.techhive.assistantservice.service.EquipmentAIService;

import org.springframework.http.HttpStatus;
import org.techhive.assistantservice.service.PatientLookupService;
import org.techhive.assistantservice.client.MedicalServiceClient;
import org.techhive.assistantservice.service.ReportAnalysisService;
import org.techhive.assistantservice.dto.MedicalFolderDTO;
import org.techhive.assistantservice.dto.PatientDTO;
import org.techhive.assistantservice.dto.ReportAnalysisResult;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai/equipment")
@RequiredArgsConstructor
public class EquipmentAIController {

    private final EquipmentAIService equipmentAIService;
    private final PatientLookupService patientLookupService;
    private final MedicalServiceClient medicalServiceClient;
    private final ReportAnalysisService reportAnalysisService;

    /**
     * POST /api/ai/equipment/recommend
     * Get AI-powered equipment recommendations based on patient condition.
     */
    @PostMapping("/recommend")
    public ResponseEntity<?> recommendEquipment(@Valid @RequestBody EquipmentRecommendRequest request) {
        log.info("Equipment recommendation request: patient={}, condition={}, severity={}",
                request.getPatientId(), request.getCondition(), request.getSeverity());

        try {
            EquipmentRecommendResponse response = equipmentAIService.recommendEquipment(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Equipment recommendation failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Equipment recommendation failed",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * POST /api/ai/equipment/recommend-from-patient-name
     * Get AI-powered equipment recommendations automatically based on patient medical folder.
     */
    @PostMapping("/recommend-from-patient-name")
    public ResponseEntity<?> recommendEquipmentFromPatientName(@RequestBody Map<String, String> payload) {
        String patientName = payload.get("patientName");
        log.info("Equipment recommendation request from patient name: {}", patientName);

        if (patientName == null || patientName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Patient name is required"));
        }

        try {
            // 1. Fetch patient
            PatientDTO patient = patientLookupService.findPatientByName(patientName);
            String patientIdentifier = patient.getKeycloakId() != null ? patient.getKeycloakId() : String.valueOf(patient.getId());

            // 2. Fetch medical folder
            List<MedicalFolderDTO> medicalFolders = medicalServiceClient.getMedicalFolderByPatient(patientIdentifier);
            if (medicalFolders == null || medicalFolders.isEmpty()) {
                throw new RuntimeException("No medical folder found for patient ID " + patient.getId());
            }
            MedicalFolderDTO medicalFolder = medicalFolders.get(0);
            medicalFolder.setDiagnosis(patient.getDiagnosis());

            // 3. Analyze medical folder
            ReportAnalysisResult analysis = reportAnalysisService.analyzeMedicalFolder(medicalFolder);
            
            // 4. Determine condition and severity
            String condition = (analysis.getDiagnosis() != null && !analysis.getDiagnosis().isEmpty()) ? analysis.getDiagnosis() : "UNKNOWN";
            String severity = (analysis.getCognitiveLevel() != null && !analysis.getCognitiveLevel().isEmpty()) ? analysis.getCognitiveLevel() : "MODERATE";

            String customContext = String.format(
                "Patient Profile: %s %s (age %d). " +
                "Detailed Medical Condition: %s. " +
                "Cognitive Severity Level: %s. " +
                "Specific Needs & Weak Topics: %s. " +
                "Additional Notes & Recommended Topics: %s. " +
                "Please ensure standard equipmentCategory matching this physical and medical state.",
                patient.getFirstName(), patient.getLastName(),
                patient.getAge() != null ? patient.getAge() : 0,
                condition,
                severity,
                analysis.getWeakTopics() != null ? String.join(", ", analysis.getWeakTopics()) : "None",
                analysis.getRecommendedTopics() != null ? String.join(", ", analysis.getRecommendedTopics()) : "None"
            );

            // 5. Generate equipment recommendations
            EquipmentRecommendRequest request = new EquipmentRecommendRequest();
            request.setPatientId(patient.getId());
            request.setCondition(condition);
            request.setSeverity(severity);
            request.setCustomContext(customContext);

            EquipmentRecommendResponse response = equipmentAIService.recommendEquipment(request);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Equipment recommendation from patient name failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Custom equipment recommendation failed",
                            "message", e.getMessage()
                    ));
        }
    }
}
