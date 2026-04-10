package org.techhive.analyticsservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.techhive.analyticsservice.client.UserServiceClient;
import org.techhive.analyticsservice.dto.BatchJobResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScoreBatchScheduler {

    private final UserServiceClient userClient;
    private final PatientScoreService scoreService;
    private final FeatureGateService gateService;
    private final DoctorEffectivenessService doctorService;

    /**
     * Nightly batch: recompute all patient scores, feature gates, and doctor effectiveness.
     * Runs at 3:00 AM every day.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void nightlyScoreRecomputation() {
        runAllJobs();
    }

    public BatchJobResult runAllJobs() {
        log.info("Starting full score recomputation batch...");
        LocalDateTime start = LocalDateTime.now();
        long t0 = System.currentTimeMillis();
        int processed = 0;
        int errors = 0;

        try {
            BatchJobResult patientResult = runPatientScores();
            BatchJobResult doctorResult = runDoctorEffectiveness();
            processed = patientResult.getProcessedCount() + doctorResult.getProcessedCount();
            errors = patientResult.getErrorCount() + doctorResult.getErrorCount();
        } catch (Exception e) {
            log.error("Batch failed: {}", e.getMessage(), e);
            errors++;
        }

        long duration = System.currentTimeMillis() - t0;
        log.info("Full batch complete in {}ms. Processed: {}, Errors: {}", duration, processed, errors);

        return BatchJobResult.builder()
                .jobName("full-recomputation")
                .status(errors > 0 ? "COMPLETED_WITH_ERRORS" : "SUCCESS")
                .processedCount(processed)
                .errorCount(errors)
                .startedAt(start)
                .completedAt(LocalDateTime.now())
                .durationMs(duration)
                .message(String.format("Processed %d items with %d errors", processed, errors))
                .build();
    }

    public BatchJobResult runPatientScores() {
        log.info("Starting patient score computation...");
        LocalDateTime start = LocalDateTime.now();
        long t0 = System.currentTimeMillis();
        int count = 0;
        int errors = 0;

        try {
            List<Map<String, Object>> patients = userClient.getUsersByRole("patient");
            for (Map<String, Object> patient : patients) {
                String keycloakId = (String) patient.get("keycloakId");
                if (keycloakId == null) continue;
                try {
                    scoreService.computeAndSave(keycloakId);
                    gateService.computeAndSave(keycloakId);
                    count++;
                } catch (Exception e) {
                    log.warn("Failed for patient {}: {}", keycloakId, e.getMessage());
                    errors++;
                }
            }
        } catch (Exception e) {
            log.error("Patient batch failed: {}", e.getMessage(), e);
            errors++;
        }

        long duration = System.currentTimeMillis() - t0;
        return BatchJobResult.builder()
                .jobName("patient-scores")
                .status(errors > 0 ? "COMPLETED_WITH_ERRORS" : "SUCCESS")
                .processedCount(count)
                .errorCount(errors)
                .startedAt(start)
                .completedAt(LocalDateTime.now())
                .durationMs(duration)
                .message(String.format("Computed scores for %d patients", count))
                .build();
    }

    public BatchJobResult runDoctorEffectiveness() {
        log.info("Starting doctor effectiveness computation...");
        LocalDateTime start = LocalDateTime.now();
        long t0 = System.currentTimeMillis();
        int count = 0;
        int errors = 0;

        try {
            List<Map<String, Object>> doctors = userClient.getUsersByRole("doctor");
            for (Map<String, Object> doctor : doctors) {
                String keycloakId = (String) doctor.get("keycloakId");
                if (keycloakId == null) continue;
                try {
                    doctorService.computeForDoctor(keycloakId);
                    count++;
                } catch (Exception e) {
                    log.warn("Failed for doctor {}: {}", keycloakId, e.getMessage());
                    errors++;
                }
            }
        } catch (Exception e) {
            log.error("Doctor batch failed: {}", e.getMessage(), e);
            errors++;
        }

        long duration = System.currentTimeMillis() - t0;
        return BatchJobResult.builder()
                .jobName("doctor-effectiveness")
                .status(errors > 0 ? "COMPLETED_WITH_ERRORS" : "SUCCESS")
                .processedCount(count)
                .errorCount(errors)
                .startedAt(start)
                .completedAt(LocalDateTime.now())
                .durationMs(duration)
                .message(String.format("Computed effectiveness for %d doctors", count))
                .build();
    }
}
