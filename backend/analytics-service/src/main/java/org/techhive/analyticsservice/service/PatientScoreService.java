package org.techhive.analyticsservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.analyticsservice.client.*;
import org.techhive.analyticsservice.dto.*;
import org.techhive.analyticsservice.entity.*;
import org.techhive.analyticsservice.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientScoreService {

    private final GameServiceClient gameClient;
    private final TrackingServiceClient trackingClient;
    private final MedicalServiceClient medicalClient;
    private final IotServiceClient iotClient;
    private final AlertServiceClient alertClient;

    private final PatientCompositeScoreRepository scoreRepository;
    private final ScoreHistoryRepository historyRepository;
    private final CognitiveDomainAnalysisRepository domainRepository;

    private static final double W_COGNITIVE = 0.30;
    private static final double W_DAILY = 0.25;
    private static final double W_MEDICAL = 0.20;
    private static final double W_IOT = 0.15;
    private static final double W_ENGAGEMENT = 0.10;

    @Transactional
    public PatientScoreResponse computeAndSave(String patientKeycloakId) {
        log.info("Computing composite score for patient {}", patientKeycloakId);

        double cognitive = computeCognitiveScore(patientKeycloakId);
        double daily = computeDailyFunctioningScore(patientKeycloakId);
        double medical = computeMedicalStabilityScore(patientKeycloakId);
        double iot = computeIotRiskScore(patientKeycloakId);
        double engagement = computeEngagementScore(patientKeycloakId);

        double overall = (cognitive * W_COGNITIVE)
                + (daily * W_DAILY)
                + (medical * W_MEDICAL)
                + (iot * W_IOT)
                + (engagement * W_ENGAGEMENT);

        AlzheimerStage stage = classifyStage(overall, cognitive, iot);
        ScoreTrend trend = computeTrend(patientKeycloakId, overall);

        PatientCompositeScore score = scoreRepository
                .findByPatientKeycloakId(patientKeycloakId)
                .orElse(PatientCompositeScore.builder()
                        .patientKeycloakId(patientKeycloakId)
                        .build());

        score.setCognitiveScore(cognitive);
        score.setDailyFunctioningScore(daily);
        score.setMedicalStabilityScore(medical);
        score.setIotRiskScore(iot);
        score.setEngagementScore(engagement);
        score.setOverallScore(overall);
        score.setStage(stage);
        score.setScoreTrend(trend);
        scoreRepository.save(score);

        // Save history
        historyRepository.save(ScoreHistory.builder()
                .patientKeycloakId(patientKeycloakId)
                .cognitiveScore(cognitive)
                .dailyFunctioningScore(daily)
                .medicalStabilityScore(medical)
                .iotRiskScore(iot)
                .engagementScore(engagement)
                .overallScore(overall)
                .stage(stage)
                .build());

        List<CognitiveDomainDTO> domains = computeCognitiveDomains(patientKeycloakId);

        return PatientScoreResponse.builder()
                .patientKeycloakId(patientKeycloakId)
                .cognitiveScore(cognitive)
                .dailyFunctioningScore(daily)
                .medicalStabilityScore(medical)
                .iotRiskScore(iot)
                .engagementScore(engagement)
                .overallScore(overall)
                .stage(stage)
                .scoreTrend(trend)
                .computedAt(score.getComputedAt())
                .cognitiveDomains(domains)
                .build();
    }

    public PatientScoreResponse getScore(String patientKeycloakId) {
        Optional<PatientCompositeScore> existing = scoreRepository.findByPatientKeycloakId(patientKeycloakId);
        if (existing.isPresent()) {
            PatientCompositeScore s = existing.get();
            // Return cached if computed within the last hour
            if (s.getComputedAt() != null && s.getComputedAt().isAfter(LocalDateTime.now().minusHours(1))) {
                List<CognitiveDomainDTO> domains = domainRepository
                        .findByPatientKeycloakId(patientKeycloakId).stream()
                        .map(d -> CognitiveDomainDTO.builder()
                                .domainName(d.getDomainName())
                                .correctCount(d.getCorrectCount())
                                .incorrectCount(d.getIncorrectCount())
                                .accuracyPct(d.getAccuracyPct())
                                .trend(d.getTrend())
                                .build())
                        .collect(Collectors.toList());

                return PatientScoreResponse.builder()
                        .patientKeycloakId(patientKeycloakId)
                        .cognitiveScore(s.getCognitiveScore())
                        .dailyFunctioningScore(s.getDailyFunctioningScore())
                        .medicalStabilityScore(s.getMedicalStabilityScore())
                        .iotRiskScore(s.getIotRiskScore())
                        .engagementScore(s.getEngagementScore())
                        .overallScore(s.getOverallScore())
                        .stage(s.getStage())
                        .scoreTrend(s.getScoreTrend())
                        .computedAt(s.getComputedAt())
                        .cognitiveDomains(domains)
                        .build();
            }
        }
        return computeAndSave(patientKeycloakId);
    }

    public List<ScoreHistory> getScoreHistory(String patientKeycloakId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return historyRepository.findByPatientKeycloakIdAndRecordedAtAfterOrderByRecordedAtAsc(
                patientKeycloakId, since);
    }

    // ─── Sub-score computations ───

    private double computeCognitiveScore(String patientKeycloakId) {
        try {
            GameStatsResponse stats = gameClient.getPlayerStats(patientKeycloakId);
            List<DataPointPerformanceDTO> perf = gameClient.getPerformanceData(patientKeycloakId);

            if (stats.getTotalAttempts() == 0 && perf.isEmpty()) {
                return 50.0; // neutral if no data
            }

            // Average accuracy from game stats
            double gameAccuracy = stats.getTotalAttempts() > 0 ? stats.getAverageScore() : 50.0;

            // Performance accuracy from data points
            int totalCorrect = perf.stream().mapToInt(DataPointPerformanceDTO::getCorrectCount).sum();
            int totalIncorrect = perf.stream().mapToInt(DataPointPerformanceDTO::getIncorrectCount).sum();
            int totalAttempts = totalCorrect + totalIncorrect;
            double dataAccuracy = totalAttempts > 0 ? (totalCorrect * 100.0 / totalAttempts) : 50.0;

            // Blend: 60% game score, 40% data-point accuracy
            return Math.min(100.0, (gameAccuracy * 0.6) + (dataAccuracy * 0.4));
        } catch (Exception e) {
            log.warn("Failed to compute cognitive score for {}: {}", patientKeycloakId, e.getMessage());
            return 50.0;
        }
    }

    private double computeDailyFunctioningScore(String patientKeycloakId) {
        try {
            Map<String, Object> compliance = trackingClient.getMedicationCompliance(patientKeycloakId, 30);
            Map<String, Object> incidents = trackingClient.getIncidentTypes(patientKeycloakId, 30);
            Map<String, Object> streak = trackingClient.getStreak(patientKeycloakId);

            int taken = toInt(compliance.get("taken"));
            int missed = toInt(compliance.get("missed"));
            double complianceRate = (taken + missed) > 0 ? (taken * 100.0 / (taken + missed)) : 50.0;

            @SuppressWarnings("unchecked")
            List<Integer> incidentValues = (List<Integer>) incidents.getOrDefault("values", Collections.emptyList());
            int totalIncidents = incidentValues.stream().mapToInt(Integer::intValue).sum();
            // Fewer incidents = higher score; cap penalty at 50 points
            double incidentPenalty = Math.min(50.0, totalIncidents * 5.0);

            int currentStreak = toInt(streak.get("currentStreak"));
            double streakBonus = Math.min(20.0, currentStreak * 2.0);

            return Math.max(0.0, Math.min(100.0, complianceRate - incidentPenalty + streakBonus));
        } catch (Exception e) {
            log.warn("Failed to compute daily functioning for {}: {}", patientKeycloakId, e.getMessage());
            return 50.0;
        }
    }

    private double computeMedicalStabilityScore(String patientKeycloakId) {
        try {
            List<Map<String, Object>> folders = medicalClient.getMedicalFoldersByPatient(patientKeycloakId);
            if (folders.isEmpty()) return 50.0;

            double score = 70.0; // baseline

            for (Map<String, Object> folder : folders) {
                Object folderId = folder.get("id");
                if (folderId == null) continue;
                Long id = ((Number) folderId).longValue();

                List<Map<String, Object>> diagnostics = medicalClient.getDiagnosticsByFolder(id);
                List<Map<String, Object>> goals = medicalClient.getCoachingGoals(id);

                // Comorbidities reduce score
                score -= diagnostics.size() * 5.0;

                // Coaching goal completion boosts score
                long completedGoals = goals.stream()
                        .filter(g -> "COMPLETED".equals(g.get("status")))
                        .count();
                long totalGoals = goals.size();
                if (totalGoals > 0) {
                    score += (completedGoals * 20.0 / totalGoals);
                }
            }

            // Appointment attendance
            List<Map<String, Object>> appointments = medicalClient.getAppointmentsByPatient(patientKeycloakId);
            long completed = appointments.stream()
                    .filter(a -> "COMPLETED".equals(a.get("status")))
                    .count();
            long cancelled = appointments.stream()
                    .filter(a -> "CANCELLED".equals(a.get("status")))
                    .count();
            long total = completed + cancelled;
            if (total > 0) {
                double attendanceRate = completed * 100.0 / total;
                score += (attendanceRate - 50.0) * 0.2; // slight bonus/penalty around 50%
            }

            return Math.max(0.0, Math.min(100.0, score));
        } catch (Exception e) {
            log.warn("Failed to compute medical stability for {}: {}", patientKeycloakId, e.getMessage());
            return 50.0;
        }
    }

    private double computeIotRiskScore(String patientKeycloakId) {
        try {
            // Higher score = LESS risk (inverted for consistency: 100 = safe, 0 = dangerous)
            double score = 80.0;

            // Geofence violations
            List<Map<String, Object>> violations = alertClient.getGeofenceViolations(patientKeycloakId);
            score -= Math.min(40.0, violations.size() * 8.0); // each violation costs 8 points

            // Heart rate anomalies
            String today = LocalDate.now().toString();
            List<Map<String, Object>> readings = iotClient.getHeartbeatReadings(patientKeycloakId, today);
            long abnormalReadings = readings.stream()
                    .filter(r -> {
                        int bpm = toInt(r.get("bpm"));
                        return bpm > 120 || bpm < 40;
                    })
                    .count();
            score -= Math.min(30.0, abnormalReadings * 3.0);

            // Sleep quality
            Map<String, Object> sleep = iotClient.getSleepAnalysis(patientKeycloakId, today);
            if (sleep != null && !sleep.isEmpty()) {
                Object qualityObj = sleep.get("overallQuality");
                if (qualityObj instanceof String quality) {
                    switch (quality) {
                        case "GOOD" -> score += 10;
                        case "FAIR" -> {}
                        case "POOR" -> score -= 10;
                    }
                }
            }

            return Math.max(0.0, Math.min(100.0, score));
        } catch (Exception e) {
            log.warn("Failed to compute IoT risk for {}: {}", patientKeycloakId, e.getMessage());
            return 50.0;
        }
    }

    private double computeEngagementScore(String patientKeycloakId) {
        try {
            double score = 50.0;

            // Game activity as engagement proxy
            GameStatsResponse gameStats = gameClient.getPlayerStats(patientKeycloakId);
            score += Math.min(25.0, gameStats.getTotalAttempts() * 2.5);

            // Daily log streak as engagement proxy
            Map<String, Object> streak = trackingClient.getStreak(patientKeycloakId);
            int currentStreak = toInt(streak.get("currentStreak"));
            score += Math.min(25.0, currentStreak * 3.0);

            return Math.max(0.0, Math.min(100.0, score));
        } catch (Exception e) {
            log.warn("Failed to compute engagement for {}: {}", patientKeycloakId, e.getMessage());
            return 50.0;
        }
    }

    // ─── Stage classification ───

    private AlzheimerStage classifyStage(double overall, double cognitive, double iot) {
        if (overall >= 75) return AlzheimerStage.LOW_RISK;
        if (overall >= 55 && cognitive >= 50) return AlzheimerStage.EARLY;
        if (overall >= 35 || (cognitive >= 30 && iot >= 40)) return AlzheimerStage.MODERATE;
        return AlzheimerStage.SEVERE;
    }

    // ─── Trend computation ───

    private ScoreTrend computeTrend(String patientKeycloakId, double currentScore) {
        List<ScoreHistory> recent = historyRepository
                .findTop2ByPatientKeycloakIdOrderByRecordedAtDesc(patientKeycloakId);
        if (recent.size() < 2) return ScoreTrend.INSUFFICIENT_DATA;

        double previous = recent.get(0).getOverallScore(); // most recent before current
        double diff = currentScore - previous;
        if (diff > 5) return ScoreTrend.IMPROVING;
        if (diff < -5) return ScoreTrend.DECLINING;
        return ScoreTrend.STABLE;
    }

    // ─── Cognitive domain analysis ───

    @Transactional
    public List<CognitiveDomainDTO> computeCognitiveDomains(String patientKeycloakId) {
        try {
            List<DataPointPerformanceDTO> perfData = gameClient.getPerformanceData(patientKeycloakId);
            List<Map<String, Object>> tags = gameClient.getPatientTags(patientKeycloakId);

            if (perfData.isEmpty()) return Collections.emptyList();

            // Group performance by data type (PHOTO, PLACE, MOVIE, QUESTION) as cognitive domains
            Map<String, int[]> domainStats = new LinkedHashMap<>();

            // By data type
            for (DataPointPerformanceDTO p : perfData) {
                String domain = p.getDataType();
                domainStats.computeIfAbsent(domain, k -> new int[]{0, 0});
                domainStats.get(domain)[0] += p.getCorrectCount();
                domainStats.get(domain)[1] += p.getIncorrectCount();
            }

            // Also aggregate by tag names if available (these represent temporal/semantic domains)
            for (Map<String, Object> tag : tags) {
                String tagName = (String) tag.get("name");
                if (tagName != null) {
                    domainStats.computeIfAbsent("TAG:" + tagName, k -> new int[]{0, 0});
                    // Tags contribute to domain awareness even without direct stats
                }
            }

            // Persist and return
            domainRepository.deleteByPatientKeycloakId(patientKeycloakId);

            List<CognitiveDomainDTO> result = new ArrayList<>();
            for (Map.Entry<String, int[]> entry : domainStats.entrySet()) {
                int correct = entry.getValue()[0];
                int incorrect = entry.getValue()[1];
                int total = correct + incorrect;
                double accuracy = total > 0 ? (correct * 100.0 / total) : 0;

                CognitiveDomainAnalysis analysis = CognitiveDomainAnalysis.builder()
                        .patientKeycloakId(patientKeycloakId)
                        .domainName(entry.getKey())
                        .correctCount(correct)
                        .incorrectCount(incorrect)
                        .accuracyPct(accuracy)
                        .trend(ScoreTrend.INSUFFICIENT_DATA)
                        .build();
                domainRepository.save(analysis);

                result.add(CognitiveDomainDTO.builder()
                        .domainName(entry.getKey())
                        .correctCount(correct)
                        .incorrectCount(incorrect)
                        .accuracyPct(accuracy)
                        .trend(ScoreTrend.INSUFFICIENT_DATA)
                        .build());
            }

            return result;
        } catch (Exception e) {
            log.warn("Failed to compute cognitive domains for {}: {}", patientKeycloakId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private int toInt(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number n) return n.intValue();
        try { return Integer.parseInt(obj.toString()); } catch (NumberFormatException e) { return 0; }
    }
}
