package org.techhive.trackingservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import org.techhive.trackingservice.dto.HealthScoreResponse;
import org.techhive.trackingservice.service.HealthScoreService;

/**
 * API REST dédiée au Score Santé (Daily Health Score).
 * Aucune logique métier : délégation totale au HealthScoreService.
 */
@RestController
@RequestMapping("/api/health-score")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HealthScoreController {

    private final HealthScoreService healthScoreService;

    /**
     * Récupère le score santé quotidien pour un patient à une date donnée.
     *
     * @param patientId identifiant Keycloak du patient
     * @param date      date au format YYYY-MM-DD (par défaut : aujourd'hui)
     * @return HealthScoreResponse (totalScore, adjustedMaxScore, riskLevel, colorCode, breakdown, missingCategories)
     */
    @GetMapping("/{patientId}")
    public ResponseEntity<HealthScoreResponse> getDailyScore(
            @PathVariable String patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        HealthScoreResponse response = healthScoreService.computeDailyScore(patientId, targetDate);
        return ResponseEntity.ok(response);
    }
}
