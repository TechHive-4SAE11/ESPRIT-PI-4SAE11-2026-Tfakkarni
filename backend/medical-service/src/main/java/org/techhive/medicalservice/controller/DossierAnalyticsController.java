package org.techhive.medicalservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.techhive.medicalservice.dto.CrossPatientDiseaseDto;
import org.techhive.medicalservice.dto.DiseaseCountDto;
import org.techhive.medicalservice.dto.DiagnosticsByMonthDto;
import org.techhive.medicalservice.dto.MonthComparisonDto;
import org.techhive.medicalservice.dto.ClinicalSafetyStatsDto;
import org.techhive.medicalservice.dto.FolderSpecificStatsDto;
import org.techhive.medicalservice.service.DossierAnalyticsService;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.time.Year;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/medical-folders/analytics")
@RequiredArgsConstructor
@Slf4j
public class DossierAnalyticsController {

    private final DossierAnalyticsService dossierAnalyticsService;

    @GetMapping("/top-diseases")
    public ResponseEntity<List<DiseaseCountDto>> getTopDiseases(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("GET /api/medical-folders/analytics/top-diseases limit={}", limit);
        return ResponseEntity.ok(dossierAnalyticsService.getTopDiseases(Math.min(limit, 50)));
    }

    @GetMapping("/by-month")
    public ResponseEntity<List<DiagnosticsByMonthDto>> getDiagnosticsByMonth(
            @RequestParam(required = false) Integer year) {
        int y = year != null ? year : Year.now().getValue();
        log.info("GET /api/medical-folders/analytics/by-month year={}", y);
        return ResponseEntity.ok(dossierAnalyticsService.getDiagnosticsByMonth(y));
    }

    @GetMapping("/comparison")
    public ResponseEntity<MonthComparisonDto> getMonthComparison() {
        log.info("GET /api/medical-folders/analytics/comparison");
        return ResponseEntity.ok(dossierAnalyticsService.getMonthComparison());
    }

    @GetMapping("/by-disease")
    public ResponseEntity<List<CrossPatientDiseaseDto>> getByDisease(
            @RequestParam String diseaseName,
            @RequestParam(required = false) String stage) {
        log.info("GET /api/medical-folders/analytics/by-disease diseaseName={} stage={}", diseaseName, stage);
        return ResponseEntity.ok(dossierAnalyticsService.getByDisease(diseaseName, stage));
    }

    @GetMapping("/safety-audit")
    public ResponseEntity<ClinicalSafetyStatsDto> getSafetyAudit() {
        log.info("GET /api/medical-folders/analytics/safety-audit");
        return ResponseEntity.ok(dossierAnalyticsService.getClinicalSafetyStats());
    }

    @GetMapping("/folder/{folderId}")
    public ResponseEntity<FolderSpecificStatsDto> getFolderStats(@PathVariable Long folderId) {
        log.info("GET /api/medical-folders/analytics/folder/{}", folderId);
        return ResponseEntity.ok(dossierAnalyticsService.getFolderStats(folderId));
    }
}
