package org.techhive.medicalservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.techhive.medicalservice.dto.PatientBadgeDto;
import org.techhive.medicalservice.service.PatientBadgeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/patient-badges")
@RequiredArgsConstructor
@Slf4j
public class PatientBadgeController {

    private final PatientBadgeService patientBadgeService;

    /**
     * Récupère tous les badges d'un patient (par Keycloak ID).
     */
    @GetMapping("/{patientId}")
    public ResponseEntity<List<PatientBadgeDto>> getBadges(@PathVariable String patientId) {
        log.info("GET /api/patient-badges/{} — récupération des badges", patientId);
        return ResponseEntity.ok(patientBadgeService.getBadgesForPatient(patientId));
    }

    /**
     * Déclenche l'évaluation des badges pour un patient.
     * Appelle le game-service, évalue les règles, et stocke les nouveaux badges.
     * Retourne uniquement les NOUVEAUX badges attribués lors de cet appel.
     */
    @PostMapping("/{patientId}/evaluate")
    public ResponseEntity<List<PatientBadgeDto>> evaluateBadges(@PathVariable String patientId) {
        log.info("POST /api/patient-badges/{}/evaluate — évaluation des badges", patientId);
        List<PatientBadgeDto> newBadges = patientBadgeService.evaluateAndAwardBadges(patientId);
        return ResponseEntity.ok(newBadges);
    }
}
