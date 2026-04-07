package org.techhive.medicalservice.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.techhive.medicalservice.dto.CrossPatientDiseaseDto;
import org.techhive.medicalservice.dto.DiseaseCountDto;
import org.techhive.medicalservice.dto.DiagnosticsByMonthDto;
import org.techhive.medicalservice.dto.FolderSpecificStatsDto;
import org.techhive.medicalservice.dto.MonthComparisonDto;
import org.techhive.medicalservice.entity.Diagnostics;
import org.techhive.medicalservice.repository.DiagnosticsRepository;
import org.techhive.medicalservice.repository.MedicalFolderRepository;
import org.techhive.medicalservice.repository.MedicalHistoryRepository;
import org.techhive.medicalservice.service.DossierAnalyticsService;

import org.springframework.web.client.RestTemplate;
import org.techhive.medicalservice.dto.ClinicalSafetyStatsDto;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DossierAnalyticsServiceImpl implements DossierAnalyticsService {

        private static final String TRACKING_BASE = "http://tracking-service";

        /**
         * Demo conflict rows (presentation-demo): Jack Sparrow, Nessim Baraket, monta kaabi — UUIDs match app patients.
         */
        private static final PresentationConflictSeed[] PRESENTATION_CONFLICT_SEEDS = {
                        new PresentationConflictSeed("85f70fbb-89c1-4156-b49c-5597b74f91f6", "Jack Sparrow", "Ibuprofen",
                                        "NSAID vs acid-related disease", "MEDIUM"),
                        new PresentationConflictSeed("85f70fbb-89c1-4156-b49c-5597b74f91f6", "Jack Sparrow", "Naproxen",
                                        "NSAID vs GERD / reflux", "MEDIUM"),
                        new PresentationConflictSeed("85f70fbb-89c1-4156-b49c-5597b74f91f6", "Jack Sparrow", "Ketoprofen",
                                        "GI bleed risk with NSAID course", "HIGH"),
                        new PresentationConflictSeed("72e0e30c-4eeb-47ec-9317-900bc2de1c12", "Nessim Baraket", "Warfarin",
                                        "Anticoagulant / antiplatelet stacking", "HIGH"),
                        new PresentationConflictSeed("72e0e30c-4eeb-47ec-9317-900bc2de1c12", "Nessim Baraket", "Aspirin",
                                        "Bleeding risk (multi-agent antithrombotic)", "HIGH"),
                        new PresentationConflictSeed("72e0e30c-4eeb-47ec-9317-900bc2de1c12", "Nessim Baraket", "Clopidogrel",
                                        "Dual antiplatelet — hemorrhagic risk", "HIGH"),
                        new PresentationConflictSeed("44b7b0de-dd77-437f-bd06-855988ac2dba", "monta kaabi", "Aspirin",
                                        "NSAID vs peptic ulcer disease", "HIGH"),
                        new PresentationConflictSeed("44b7b0de-dd77-437f-bd06-855988ac2dba", "monta kaabi", "Ibuprofen",
                                        "Cardiovascular risk (long-term NSAID)", "MEDIUM"),
                        new PresentationConflictSeed("44b7b0de-dd77-437f-bd06-855988ac2dba", "monta kaabi", "Metformin",
                                        "Renal function caution (contrast / dehydration)", "MEDIUM"),
        };

        private record PresentationConflictSeed(String patientId, String patientDisplayName, String medicationName,
                        String conflictingCondition, String severity) {
        }

        private final DiagnosticsRepository diagnosticsRepository;
        private final MedicalFolderRepository medicalFolderRepository;
        private final MedicalHistoryRepository medicalHistoryRepository;
        private final RestTemplate restTemplate;

        /**
         * When true and tracking returns no medications, KPIs are augmented with plausible sample values
         * tied to real patient IDs from diagnostics (jury / local demo only).
         */
        @Value("${medical.analytics.safety-audit.presentation-demo:false}")
        private boolean safetyAuditPresentationDemo;

        public DossierAnalyticsServiceImpl(DiagnosticsRepository diagnosticsRepository,
                        MedicalFolderRepository medicalFolderRepository,
                        MedicalHistoryRepository medicalHistoryRepository,
                        @org.springframework.beans.factory.annotation.Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate) {
                this.diagnosticsRepository = diagnosticsRepository;
                this.medicalFolderRepository = medicalFolderRepository;
                this.medicalHistoryRepository = medicalHistoryRepository;
                this.restTemplate = restTemplate;
        }

        @Override
        public List<DiseaseCountDto> getTopDiseases(int limit) {
                return diagnosticsRepository.findDiseaseCounts(PageRequest.of(0, limit)).stream()
                                .map(row -> DiseaseCountDto.builder()
                                                .diseaseName((String) row[0])
                                                .count(((Number) row[1]).longValue())
                                                .build())
                                .collect(Collectors.toList());
        }

        @Override
        public List<DiagnosticsByMonthDto> getDiagnosticsByMonth(int year) {
                return diagnosticsRepository.findDiagnosticsCountByMonthAndDisease().stream()
                                .filter(row -> ((Number) row[0]).intValue() == year)
                                .map(row -> DiagnosticsByMonthDto.builder()
                                                .year(((Number) row[0]).intValue())
                                                .month(((Number) row[1]).intValue())
                                                .diseaseName((String) row[2])
                                                .count(((Number) row[3]).longValue())
                                                .build())
                                .collect(Collectors.toList());
        }

        @Override
        public MonthComparisonDto getMonthComparison() {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime startOfThisMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                                .withNano(0);
                LocalDateTime startOfLastMonth = startOfThisMonth.minusMonths(1);

                long thisMonthDiagnostics = diagnosticsRepository.countByDiagnosisDateAfter(startOfThisMonth);
                long lastMonthDiagnostics = diagnosticsRepository.countByDiagnosisDateBetween(startOfLastMonth,
                                startOfThisMonth);
                long thisMonthFolders = medicalFolderRepository.countByCreatedAtAfter(startOfThisMonth);
                long lastMonthFolders = medicalFolderRepository.countByCreatedAtBetween(startOfLastMonth,
                                startOfThisMonth);

                return MonthComparisonDto.builder()
                                .thisMonthDiagnostics(thisMonthDiagnostics)
                                .lastMonthDiagnostics(lastMonthDiagnostics)
                                .thisMonthFolders(thisMonthFolders)
                                .lastMonthFolders(lastMonthFolders)
                                .build();
        }

        @Override
        public List<CrossPatientDiseaseDto> getByDisease(String diseaseName, String stage) {
                if (diseaseName == null || diseaseName.isBlank()) {
                        return Collections.emptyList();
                }
                String stageParam = (stage != null && !stage.isBlank()) ? stage.trim() : null;
                List<Diagnostics> list = diagnosticsRepository
                                .findByDiseaseNameContainingIgnoreCaseAndOptionalStage(diseaseName.trim(), stageParam);
                return list.stream()
                                .map(d -> CrossPatientDiseaseDto.builder()
                                                .diagnosticsId(d.getId())
                                                .medicalFolderId(d.getMedicalFolder().getId())
                                                .patientId(d.getMedicalFolder().getPatientId())
                                                .doctorId(d.getMedicalFolder().getDoctorId())
                                                .diseaseName(d.getDiseaseName())
                                                .stage(d.getStage())
                                                .diagnosisDate(d.getDiagnosisDate())
                                                .build())
                                .collect(Collectors.toList());
        }

        @Override
        public ClinicalSafetyStatsDto getClinicalSafetyStats() {
                List<Diagnostics> diagnostics = diagnosticsRepository.findAll();
                try {
                        Map<String, List<String>> patientMeds = loadPatientMedicationsFromTracking();

                        long coveredCount = 0;
                        Set<String> polyPatients = new HashSet<>();
                        Set<String> chronicPatients = new HashSet<>();
                        Set<String> conflictKeys = new HashSet<>();
                        List<ClinicalSafetyStatsDto.MedicationConflictDto> conflicts = new ArrayList<>();

                        for (Diagnostics diag : diagnostics) {
                                String pId = diag.getMedicalFolder().getPatientId();
                                List<String> meds = patientMeds.getOrDefault(pId, Collections.emptyList());
                                String diseaseLower = diag.getDiseaseName() != null
                                                ? diag.getDiseaseName().toLowerCase()
                                                : "";
                                Set<String> medSet = new HashSet<>(meds);

                                if (!meds.isEmpty())
                                        coveredCount++;

                                if (medSet.size() > 5)
                                        polyPatients.add(pId);

                                if ((diseaseLower.contains("alzheimer") || diseaseLower.contains("diabetes"))
                                                && meds.isEmpty()) {
                                        chronicPatients.add(pId);
                                }

                                addConflictIfMatch(conflicts, conflictKeys, pId, diseaseLower, medSet, "reflux",
                                                Set.of("ibuprofen", "naproxen", "ketoprofen"),
                                                "NSAID vs acid-related disease", "MEDIUM");
                                addConflictIfMatch(conflicts, conflictKeys, pId, diseaseLower, medSet, "ulcer",
                                                Set.of("ibuprofen", "aspirin", "naproxen"),
                                                "NSAID vs peptic ulcer", "HIGH");
                                addConflictIfMatch(conflicts, conflictKeys, pId, diseaseLower, medSet, "bleeding",
                                                Set.of("warfarin", "aspirin", "clopidogrel"),
                                                "Anticoagulant / antiplatelet stacking", "HIGH");
                        }

                        ClinicalSafetyStatsDto built = ClinicalSafetyStatsDto.builder()
                                        .treatmentCoverageRate(diagnostics.isEmpty() ? 0
                                                        : (double) coveredCount / diagnostics.size() * 100)
                                        .polypharmacyRiskCount(polyPatients.size())
                                        .chronicMonitoringAlerts(chronicPatients.size())
                                        .potentialConflicts(conflicts)
                                        .illustrationData(false)
                                        .build();

                        return maybeAugmentSafetyAuditForPresentation(built, patientMeds, diagnostics);

                } catch (Exception e) {
                        log.warn("Clinical safety audit: tracking unavailable or parse error: {}", e.getMessage());
                        if (safetyAuditPresentationDemo) {
                                return augmentSafetyAuditPresentationSample(
                                                ClinicalSafetyStatsDto.builder()
                                                                .treatmentCoverageRate(0)
                                                                .polypharmacyRiskCount(0)
                                                                .chronicMonitoringAlerts(0)
                                                                .potentialConflicts(Collections.emptyList())
                                                                .illustrationData(false)
                                                                .build(),
                                                diagnostics);
                        }
                        return ClinicalSafetyStatsDto.builder()
                                        .treatmentCoverageRate(0).polypharmacyRiskCount(0).chronicMonitoringAlerts(0)
                                        .potentialConflicts(Collections.emptyList())
                                        .illustrationData(false)
                                        .build();
                }
        }

        private boolean isTrackingMedicationDataEmpty(Map<String, List<String>> patientMeds) {
                if (patientMeds == null || patientMeds.isEmpty()) {
                        return true;
                }
                return patientMeds.values().stream().mapToInt(List::size).sum() == 0;
        }

        private ClinicalSafetyStatsDto maybeAugmentSafetyAuditForPresentation(ClinicalSafetyStatsDto computed,
                        Map<String, List<String>> patientMeds, List<Diagnostics> diagnostics) {
                if (!safetyAuditPresentationDemo) {
                        return computed;
                }
                boolean trackingEmpty = isTrackingMedicationDataEmpty(patientMeds);
                log.info("Safety audit: presentation-demo active (tracking meds empty={}).", trackingEmpty);
                return augmentSafetyAuditPresentationSample(computed, diagnostics);
        }

        /**
         * Ensures non-zero KPIs and sample conflict rows using real patient IDs from diagnostics when possible.
         */
        private ClinicalSafetyStatsDto augmentSafetyAuditPresentationSample(ClinicalSafetyStatsDto base,
                        List<Diagnostics> diagnostics) {
                double coverage = base.getTreatmentCoverageRate();
                if (diagnostics.isEmpty()) {
                        coverage = 0;
                } else if (coverage < 1.0) {
                        coverage = 68.0;
                }

                List<ClinicalSafetyStatsDto.MedicationConflictDto> merged = new ArrayList<>(
                                base.getPotentialConflicts() != null ? base.getPotentialConflicts()
                                                : Collections.emptyList());
                Set<String> seen = new HashSet<>();
                for (ClinicalSafetyStatsDto.MedicationConflictDto c : merged) {
                        seen.add(c.getPatientId() + "|" + c.getMedicationName() + "|" + c.getConflictingCondition());
                }
                for (PresentationConflictSeed row : PRESENTATION_CONFLICT_SEEDS) {
                        addPresentationConflictIfMissing(merged, seen, row.patientId(), row.patientDisplayName(),
                                        row.medicationName(), row.conflictingCondition(), row.severity());
                }

                return ClinicalSafetyStatsDto.builder()
                                .treatmentCoverageRate(coverage)
                                .polypharmacyRiskCount(Math.max(base.getPolypharmacyRiskCount(), 3))
                                .chronicMonitoringAlerts(Math.max(base.getChronicMonitoringAlerts(), 2))
                                .potentialConflicts(merged)
                                .illustrationData(true)
                                .build();
        }

        private static void addPresentationConflictIfMissing(
                        List<ClinicalSafetyStatsDto.MedicationConflictDto> merged, Set<String> seen,
                        String patientId, String patientDisplayName, String med, String condition, String severity) {
                String key = patientId + "|" + med + "|" + condition;
                if (!seen.add(key)) {
                        return;
                }
                merged.add(ClinicalSafetyStatsDto.MedicationConflictDto.builder()
                                .patientId(patientId)
                                .patientDisplayName(patientDisplayName)
                                .medicationName(med)
                                .conflictingCondition(condition)
                                .severity(severity)
                                .build());
        }

        /**
         * Tracking exposes flat folder DTOs (no nested sessions). We resolve prescriptions by
         * walking sessions and session-scoped prescription lists — same data the UI uses.
         */
        private Map<String, List<String>> loadPatientMedicationsFromTracking() {
                Map<String, List<String>> patientMeds = new HashMap<>();
                JsonNode folders = restTemplate.getForObject(TRACKING_BASE + "/api/medical-folders",
                                JsonNode.class);
                if (folders == null || !folders.isArray()) {
                        return patientMeds;
                }
                for (JsonNode folder : folders) {
                        String patientId = extractTrackingPatientId(folder);
                        if (patientId == null || patientId.isBlank()) {
                                continue;
                        }
                        long folderId = folder.path("id").asLong(0);
                        if (folderId == 0L) {
                                continue;
                        }
                        JsonNode sessions = restTemplate.getForObject(
                                        TRACKING_BASE + "/api/sessions/medical-folder/" + folderId,
                                        JsonNode.class);
                        if (sessions == null || !sessions.isArray()) {
                                continue;
                        }
                        for (JsonNode session : sessions) {
                                long sessionId = session.path("id").asLong(0);
                                if (sessionId == 0L) {
                                        continue;
                                }
                                JsonNode prescriptions = restTemplate.getForObject(
                                                TRACKING_BASE + "/api/prescriptions/session/" + sessionId,
                                                JsonNode.class);
                                mergeMedicationNames(patientMeds, patientId, prescriptions);
                        }
                }
                return patientMeds;
        }

        private static String extractTrackingPatientId(JsonNode folder) {
                String p = folder.path("patientId").asText("");
                if (!p.isBlank()) {
                        return p;
                }
                p = folder.path("idPatient").asText("");
                return p.isBlank() ? null : p;
        }

        private static void mergeMedicationNames(Map<String, List<String>> patientMeds, String patientId,
                        JsonNode prescriptions) {
                if (prescriptions == null || !prescriptions.isArray()) {
                        return;
                }
                for (JsonNode prescription : prescriptions) {
                        prescription.path("medications").forEach(med -> {
                                String name = med.path("medicationName").asText("").trim();
                                if (!name.isBlank()) {
                                        patientMeds.computeIfAbsent(patientId, k -> new ArrayList<>())
                                                        .add(name.toLowerCase());
                                }
                        });
                }
        }

        private static void addConflictIfMatch(List<ClinicalSafetyStatsDto.MedicationConflictDto> conflicts,
                        Set<String> conflictKeys, String patientId, String diseaseLower, Set<String> medSet,
                        String diseaseKeyword, Set<String> riskyMedsLower, String label, String severity) {
                if (!diseaseLower.contains(diseaseKeyword)) {
                        return;
                }
                for (String rm : riskyMedsLower) {
                        if (medSet.contains(rm)) {
                                String key = patientId + "|" + rm + "|" + label;
                                if (!conflictKeys.add(key)) {
                                        return;
                                }
                                String displayName = rm.length() > 1
                                                ? rm.substring(0, 1).toUpperCase() + rm.substring(1)
                                                : rm.toUpperCase();
                                conflicts.add(ClinicalSafetyStatsDto.MedicationConflictDto.builder()
                                                .patientId(patientId)
                                                .medicationName(displayName)
                                                .conflictingCondition(label)
                                                .severity(severity)
                                                .build());
                                return;
                        }
                }
        }

        @Override
        public FolderSpecificStatsDto getFolderStats(Long folderId) {
                var folder = medicalFolderRepository.findById(folderId).orElse(null);
                if (folder == null)
                        return null;

                List<Diagnostics> diagnostics = diagnosticsRepository.findByMedicalFolderId(folderId);
                long historyCount = medicalHistoryRepository.findByMedicalFolderId(folderId).size();

                List<FolderSpecificStatsDto.MedicationSummary> prescriptions = new ArrayList<>();
                Set<String> prescribedMeds = new HashSet<>();

                // 1. Fetch prescriptions from tracking (folder DTOs are flat — no nested sessions)
                try {
                        String patientId = folder.getPatientId();
                        JsonNode trackingFolders = restTemplate.getForObject(
                                        TRACKING_BASE + "/api/medical-folders/patient/" + patientId,
                                        JsonNode.class);
                        if (trackingFolders != null && trackingFolders.isArray()) {
                                for (JsonNode tf : trackingFolders) {
                                        long tid = tf.path("id").asLong(0);
                                        if (tid == 0L) {
                                                continue;
                                        }
                                        JsonNode sessions = restTemplate.getForObject(
                                                        TRACKING_BASE + "/api/sessions/medical-folder/" + tid,
                                                        JsonNode.class);
                                        if (sessions == null || !sessions.isArray()) {
                                                continue;
                                        }
                                        for (JsonNode session : sessions) {
                                                long sessionId = session.path("id").asLong(0);
                                                if (sessionId == 0L) {
                                                        continue;
                                                }
                                                String date = session.path("createdAt").asText("");
                                                JsonNode prescList = restTemplate.getForObject(
                                                                TRACKING_BASE + "/api/prescriptions/session/"
                                                                                + sessionId,
                                                                JsonNode.class);
                                                if (prescList == null || !prescList.isArray()) {
                                                        continue;
                                                }
                                                for (JsonNode p : prescList) {
                                                        p.path("medications").forEach(m -> {
                                                                String name = m.path("medicationName").asText("");
                                                                if (!name.isBlank()) {
                                                                        prescriptions.add(
                                                                                        FolderSpecificStatsDto.MedicationSummary
                                                                                                        .builder()
                                                                                                        .medicationName(name)
                                                                                                        .prescribedAt(date)
                                                                                                        .build());
                                                                        prescribedMeds.add(name.toLowerCase());
                                                                }
                                                        });
                                                }
                                        }
                                }
                        }
                } catch (Exception e) {
                        log.debug("Tracking service unavailable during folder analytics: {}", e.getMessage());
                }

                // 2. Coverage calculation
                long coveredDiagnostics = diagnostics.stream()
                                .filter(d -> prescribedMeds.stream()
                                                .anyMatch(med -> d.getDiseaseName().toLowerCase().contains(med)
                                                                ||
                                                                med.contains(d.getDiseaseName().toLowerCase())))
                                .count();

                // 3. Stage Distribution (as Severity)
                Map<String, Long> severityDist = diagnostics.stream()
                                .filter(d -> d.getStage() != null)
                                .collect(Collectors.groupingBy(Diagnostics::getStage,
                                                Collectors.counting()));

                // 4. Timeline
                List<FolderSpecificStatsDto.DiagnosticTimelineEntry> timeline = diagnostics.stream()
                                .sorted(Comparator.comparing(Diagnostics::getDiagnosisDate))
                                .map(d -> FolderSpecificStatsDto.DiagnosticTimelineEntry.builder()
                                                .date(d.getDiagnosisDate().toString())
                                                .diseaseName(d.getDiseaseName())
                                                .stage(d.getStage())
                                                .build())
                                .collect(Collectors.toList());

                return FolderSpecificStatsDto.builder()
                                .totalDiagnostics(diagnostics.size())
                                .totalMedicalHistory(historyCount)
                                .severityDistribution(severityDist)
                                .treatmentCoverageRate(diagnostics.isEmpty() ? 0
                                                : (double) coveredDiagnostics / diagnostics.size() * 100)
                                .prescriptions(prescriptions)
                                .timeline(timeline)
                                .build();
        }
}
