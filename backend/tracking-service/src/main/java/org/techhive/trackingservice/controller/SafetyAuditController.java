package org.techhive.trackingservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.techhive.trackingservice.dto.PatientMedicationAuditRequest;
import org.techhive.trackingservice.dto.PatientMedicationAuditResponse;
import org.techhive.trackingservice.service.SafetyAuditService;

@RestController
@RequestMapping("/api/analytics/safety-audit")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SafetyAuditController {

    private final SafetyAuditService safetyAuditService;

    /**
     * Single batch read from tracking DB — avoids N+1 HTTP calls from medical-service.
     */
    @PostMapping("/patient-medications")
    public ResponseEntity<PatientMedicationAuditResponse> patientMedications(
            @RequestBody PatientMedicationAuditRequest request) {
        return ResponseEntity.ok(safetyAuditService.buildPatientMedicationAudit(request.getPatientIds()));
    }
}
