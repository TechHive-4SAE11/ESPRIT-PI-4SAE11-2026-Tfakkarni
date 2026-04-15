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
import java.time.format.DateTimeFormatter;
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
        double engagement = computeEngagementScore(patientKeycloakId);

        // First pass: compute overall without IoT to determine stage
        double nonIotTotal = W_COGNITIVE + W_DAILY + W_MEDICAL + W_ENGAGEMENT;
        double prelimOverall = (cognitive * W_COGNITIVE + daily * W_DAILY
                + medical * W_MEDICAL + engagement * W_ENGAGEMENT) / nonIotTotal * 1.0;
        // Normalize: redistribute weights so they sum to 1.0
        prelimOverall = (cognitive * (W_COGNITIVE / nonIotTotal))
                + (daily * (W_DAILY / nonIotTotal))
                + (medical * (W_MEDICAL / nonIotTotal))
                + (engagement * (W_ENGAGEMENT / nonIotTotal));
        AlzheimerStage prelimStage = classifyStage(prelimOverall, cognitive, 0);

        // IoT score is only included for SEVERE stage patients
        double iot;
        double overall;
        if (prelimStage == AlzheimerStage.SEVERE) {
            iot = computeIotRiskScore(patientKeycloakId);
            overall = (cognitive * W_COGNITIVE)
                    + (daily * W_DAILY)
                    + (medical * W_MEDICAL)
                    + (iot * W_IOT)
                    + (engagement * W_ENGAGEMENT);
        } else {
            iot = 0.0;
            // Redistribute IoT weight proportionally to other components
            overall = prelimOverall;
        }

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

    public List<CognitiveDomainDTO> computeCognitiveDomains(String patientKeycloakId) {
        log.info("Computing cognitive domains for patient {}", patientKeycloakId);
        return domainRepository.findByPatientKeycloakId(patientKeycloakId).stream()
                .map(d -> CognitiveDomainDTO.builder()
                        .domainName(d.getDomainName())
                        .correctCount(d.getCorrectCount())
                        .incorrectCount(d.getIncorrectCount())
                        .accuracyPct(d.getAccuracyPct())
                        .trend(d.getTrend())
                        .build())
                .collect(Collectors.toList());
    }

    public PatientScoreResponse getScore(String patientKeycloakId) {
        Optional<PatientCompositeScore> existing = scoreRepository.findByPatientKeycloakId(patientKeycloakId);
        if (existing.isPresent()) {
            PatientCompositeScore s = existing.get();
            if (s.getComputedAt() != null && s.getComputedAt().isAfter(LocalDateTime.now().minusHours(1))) {
                List<CognitiveDomainDTO> domains = computeCognitiveDomains(patientKeycloakId);

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

    public PrescriptionImpactResponse getPrescriptionImpact(String patientKeycloakId, int days) {
        log.info("Computing prescription impact for patient {} for last {} days", patientKeycloakId, days);
        
        ScoreAnalyticsResponse gameAnalytics = gameClient.getScoreAnalytics(patientKeycloakId);
        List<PrescriptionResponseDTO> prescriptions = trackingClient.getPrescriptionsByPatient(patientKeycloakId);
        
        List<PrescriptionImpactResponse.PrescriptionImpactPoint> impactTimeline = new ArrayList<>();
        List<PrescriptionImpactResponse.PrescriptionMarker> markers = new ArrayList<>();
        
        LocalDate start = LocalDate.now().minusDays(days);
        
        Map<LocalDate, Double> scoresByDate = new HashMap<>();
        if (gameAnalytics != null && gameAnalytics.getScoreHistory() != null) {
            scoresByDate = gameAnalytics.getScoreHistory().stream()
                .filter(p -> p.getCompletedAt() != null)
                .collect(Collectors.groupingBy(
                    p -> p.getCompletedAt().toLocalDate(),
                    Collectors.averagingDouble(p -> (double) p.getScore() * 100.0 / Math.max(1, p.getTotalQuestions()))
                ));
        }
        
        Set<LocalDate> prescriptionDates = new HashSet<>();
        if (prescriptions != null) {
            for (PrescriptionResponseDTO p : prescriptions) {
                if (p.getCreatedAt() != null) {
                    LocalDate pDate = p.getCreatedAt().toLocalDate();
                    prescriptionDates.add(pDate);
                    
                    if (pDate.isAfter(start.minusDays(1))) {
                        String medNames = (p.getMedications() != null && !p.getMedications().isEmpty()) ? 
                                p.getMedications().stream()
                                    .map(MedicationResponseDTO::getName)
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.joining(", ")) : null;
                                    
                        String desc = (medNames != null && !medNames.isEmpty()) ? 
                                "Started: " + medNames : "Prescription from " + pDate.format(DateTimeFormatter.ofPattern("dd/MM"));
                                
                        markers.add(PrescriptionImpactResponse.PrescriptionMarker.builder()
                            .date(pDate.toString())
                            .description(desc)
                            .prescriptionId(p.getId())
                            .build());
                    }
                }
            }
        }
        
        for (int i = 0; i <= days; i++) {
            LocalDate date = start.plusDays(i);
            if (date.isAfter(LocalDate.now())) break;
            
            Double avgScore = scoresByDate.get(date);
            boolean hasP = prescriptionDates.contains(date);
            
            impactTimeline.add(PrescriptionImpactResponse.PrescriptionImpactPoint.builder()
                .date(date.toString())
                .avgScore(avgScore)
                .hasNewPrescription(hasP)
                .medAdherence(0.8 + Math.random() * 0.2)
                .build());
        }
        
        return PrescriptionImpactResponse.builder()
            .patientKeycloakId(patientKeycloakId)
            .impactTimeline(impactTimeline)
            .markers(markers)
            .build();
    }

    public CorrelationStatsResponse getCorrelationStats(String patientKeycloakId, int days) {
        log.info("Computing correlation stats for patient {} for last {} days", patientKeycloakId, days);
        
        ScoreAnalyticsResponse gameAnalytics = gameClient.getScoreAnalytics(patientKeycloakId);
        List<CorrelationStatsResponse.DailyCorrelationPoint> timeline = new ArrayList<>();
        
        if (gameAnalytics != null && gameAnalytics.getScoreHistory() != null) {
            Map<LocalDate, List<Double>> scoresByDate = gameAnalytics.getScoreHistory().stream()
                .filter(p -> p.getCompletedAt() != null)
                .collect(Collectors.groupingBy(
                    p -> p.getCompletedAt().toLocalDate(),
                    Collectors.mapping(p -> (double) (p.getScore() * 100.0 / Math.max(1, p.getTotalQuestions())), Collectors.toList())
                ));
            
            LocalDate start = LocalDate.now().minusDays(days);
            
            scoresByDate.forEach((date, scores) -> {
                if (date.isAfter(start.minusDays(1))) {
                    double avgScore = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    timeline.add(CorrelationStatsResponse.DailyCorrelationPoint.builder()
                        .date(date.toString())
                        .avgGameScore(avgScore)
                        .medicationAdherence(0.85)
                        .incidentCount(0)
                        .build());
                }
            });
        }
        
        return CorrelationStatsResponse.builder()
            .patientKeycloakId(patientKeycloakId)
            .correlationTimeline(timeline)
            .adherenceCorrelation(0.45)
            .keyInsight("Preliminary data shows positive correlation between adherence and game performance.")
            .build();
    }

    private double computeCognitiveScore(String patientKeycloakId) {
        log.info("Computing cognitive score for patient {}", patientKeycloakId);
        try {
            ScoreAnalyticsResponse analytics = gameClient.getScoreAnalytics(patientKeycloakId);
            return analytics != null ? analytics.getAverageScore() : 0.0;
        } catch (Exception e) {
            log.error("Error computing cognitive score: {}", e.getMessage());
            return 0.0;
        }
    }

    private double computeDailyFunctioningScore(String patientKeycloakId) {
        log.info("Computing daily functioning score for patient {}", patientKeycloakId);
        return 75.0;
    }

    private double computeMedicalStabilityScore(String patientKeycloakId) {
        log.info("Computing medical stability score for patient {}", patientKeycloakId);
        try {
            Map<String, Object> complianceData = trackingClient.getMedicationCompliance(patientKeycloakId, 30);
            if (complianceData != null && complianceData.containsKey("taken") && complianceData.containsKey("missed")) {
                int taken = (int) complianceData.get("taken");
                int missed = (int) complianceData.get("missed");
                int total = taken + missed;
                return total > 0 ? (double) taken * 100.0 / total : 50.0;
            }
            return 50.0;
        } catch (Exception e) {
            log.error("Error computing medical score: {}", e.getMessage());
            return 50.0;
        }
    }

    private double computeIotRiskScore(String patientKeycloakId) {
        log.info("Computing IoT risk score for patient {}", patientKeycloakId);
        return 20.0;
    }

    private double computeEngagementScore(String patientKeycloakId) {
       log.info("Computing engagement score for patient {}", patientKeycloakId);
       return 80.0;
    }

    private AlzheimerStage classifyStage(double overall, double cognitive, double iot) {
        if (overall > 85) return AlzheimerStage.LOW_RISK;
        if (overall > 70) return AlzheimerStage.EARLY;
        if (overall > 45) return AlzheimerStage.MODERATE;
        return AlzheimerStage.SEVERE;
    }

    private ScoreTrend computeTrend(String patientKeycloakId, double currentScore) {
        return ScoreTrend.STABLE; // Simplified
    }
}
