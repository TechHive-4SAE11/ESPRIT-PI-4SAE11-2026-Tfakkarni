package org.techhive.medicalservice.service.impl;

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

@Service
public class DossierAnalyticsServiceImpl implements DossierAnalyticsService {

        private final DiagnosticsRepository diagnosticsRepository;
        private final MedicalFolderRepository medicalFolderRepository;
        private final MedicalHistoryRepository medicalHistoryRepository;
        private final RestTemplate restTemplate;

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
                try {
                        // 1. Fetch Local Data (Diagnostics)
                        List<Diagnostics> diagnostics = diagnosticsRepository.findAll();

                        // 2. Fetch Remote Data (Prescriptions from tracking-service)
                        // We use the Eureka service name thanks to @LoadBalanced RestTemplate
                        String trackingUrl = "http://tracking-service/api/medical-folders";
                        JsonNode remoteFolders = restTemplate.getForObject(trackingUrl, JsonNode.class);

                        // 3. Process & Correlate
                        Map<String, List<String>> patientMeds = new HashMap<>();
                        if (remoteFolders != null && remoteFolders.isArray()) {
                                for (JsonNode folder : remoteFolders) {
                                        String patientId = folder.path("idPatient").asText();
                                        List<String> meds = new ArrayList<>();
                                        folder.path("sessions").forEach(session -> session.path("prescriptions")
                                                        .forEach(prescription -> prescription.path("medications")
                                                                        .forEach(med -> meds.add(med
                                                                                        .path("medicationName").asText()
                                                                                        .toLowerCase()))));
                                        patientMeds.put(patientId, meds);
                                }
                        }

                        // Calculations
                        long coveredCount = 0;
                        long polypharmacyCount = 0;
                        long chronicAlerts = 0;
                        List<ClinicalSafetyStatsDto.MedicationConflictDto> conflicts = new ArrayList<>();

                        for (Diagnostics diag : diagnostics) {
                                String pId = diag.getMedicalFolder().getPatientId();
                                List<String> meds = patientMeds.getOrDefault(pId, Collections.emptyList());

                                // Coverage check
                                if (!meds.isEmpty())
                                        coveredCount++;

                                // Polypharmacy Check (> 5 unique meds)
                                if (new HashSet<>(meds).size() > 5)
                                        polypharmacyCount++;

                                // Chronic Monitoring (e.g., Alzheimer, Hypertension mentioned)
                                if ((diag.getDiseaseName().toLowerCase().contains("alzheimer") ||
                                                diag.getDiseaseName().toLowerCase().contains("diabetes"))
                                                && meds.isEmpty()) {
                                        chronicAlerts++;
                                }

                                // Safety Logic (Mocking a Drug-Condition conflict check)
                                // Example: Patients with "Acid Reflux" shouldn't usually have heavy "Ibuprofen"
                                if (diag.getDiseaseName().toLowerCase().contains("reflux")
                                                && meds.contains("ibuprofen")) {
                                        conflicts.add(ClinicalSafetyStatsDto.MedicationConflictDto.builder()
                                                        .patientId(pId)
                                                        .medicationName("Ibuprofen")
                                                        .conflictingCondition("Acid Reflux")
                                                        .severity("MEDIUM")
                                                        .build());
                                }
                        }

                        return ClinicalSafetyStatsDto.builder()
                                        .treatmentCoverageRate(diagnostics.isEmpty() ? 0
                                                        : (double) coveredCount / diagnostics.size() * 100)
                                        .polypharmacyRiskCount(polypharmacyCount)
                                        .chronicMonitoringAlerts(chronicAlerts)
                                        .potentialConflicts(conflicts)
                                        .build();

                } catch (Exception e) {
                        // Fallback if tracking-service is down
                        return ClinicalSafetyStatsDto.builder()
                                        .treatmentCoverageRate(0).polypharmacyRiskCount(0).chronicMonitoringAlerts(0)
                                        .potentialConflicts(Collections.emptyList())
                                        .build();
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

                // 1. Safe fetch prescriptions for this folder from tracking-service
                try {
                        String trackingUrl = "http://tracking-service/api/medical-folders/" + folderId;
                        JsonNode remoteFolder = restTemplate.getForObject(trackingUrl, JsonNode.class);

                        if (remoteFolder != null) {
                                remoteFolder.path("sessions").forEach(session -> {
                                        String date = session.path("createdAt").asText();
                                        session.path("prescriptions").forEach(p -> {
                                                p.path("medications").forEach(m -> {
                                                        String name = m.path("medicationName").asText();
                                                        prescriptions.add(FolderSpecificStatsDto.MedicationSummary
                                                                        .builder()
                                                                        .medicationName(name)
                                                                        .prescribedAt(date)
                                                                        .build());
                                                        prescribedMeds.add(name.toLowerCase());
                                                });
                                        });
                                });
                        }
                } catch (Exception e) {
                        // Simply ignore tracking-service errors for the rest of the logic
                        // prescriptions will be empty, which is handled gracefully
                        System.err.println("Tracking service unavailable during dossier analytics: " + e.getMessage());
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
