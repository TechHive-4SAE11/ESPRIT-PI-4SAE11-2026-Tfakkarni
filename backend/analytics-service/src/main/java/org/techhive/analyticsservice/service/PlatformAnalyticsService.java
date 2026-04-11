package org.techhive.analyticsservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.techhive.analyticsservice.client.UserServiceClient;
import org.techhive.analyticsservice.dto.PlatformOverviewResponse;
import org.techhive.analyticsservice.entity.AlzheimerStage;
import org.techhive.analyticsservice.entity.PatientCompositeScore;
import org.techhive.analyticsservice.repository.CognitiveDomainAnalysisRepository;
import org.techhive.analyticsservice.repository.DoctorEffectivenessScoreRepository;
import org.techhive.analyticsservice.repository.PatientCompositeScoreRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformAnalyticsService {

    private final PatientCompositeScoreRepository scoreRepository;
    private final DoctorEffectivenessScoreRepository effectivenessRepository;
    private final CognitiveDomainAnalysisRepository domainRepository;
    private final UserServiceClient userClient;

    public PlatformOverviewResponse getPlatformOverview() {
        List<PatientCompositeScore> allScores = scoreRepository.findAll();

        // Stage distribution
        Map<AlzheimerStage, Integer> stageDistribution = new EnumMap<>(AlzheimerStage.class);
        for (AlzheimerStage stage : AlzheimerStage.values()) {
            stageDistribution.put(stage, 0);
        }
        for (PatientCompositeScore score : allScores) {
            stageDistribution.merge(score.getStage(), 1, Integer::sum);
        }

        // Platform average
        double avgScore = allScores.stream()
                .mapToDouble(PatientCompositeScore::getOverallScore)
                .average()
                .orElse(0.0);

        // Cognitive domain weaknesses (aggregate across all patients)
        Map<String, Integer> domainWeakness = new LinkedHashMap<>();
        domainRepository.findAll().stream()
                .filter(d -> d.getAccuracyPct() != null && d.getAccuracyPct() < 50)
                .forEach(d -> domainWeakness.merge(d.getDomainName(), 1, Integer::sum));

        // Counts
        int totalPatients;
        int totalDoctors;
        try {
            totalPatients = userClient.getUsersByRole("patient").size();
            totalDoctors = userClient.getUsersByRole("doctor").size();
        } catch (Exception e) {
            totalPatients = allScores.size();
            totalDoctors = 0;
        }

        long redFlagCount = effectivenessRepository.findByRiskFlagsIsNotNull().stream()
                .filter(e -> e.getRiskFlags() != null && !e.getRiskFlags().equals("[]"))
                .count();

        return PlatformOverviewResponse.builder()
                .totalPatients(totalPatients)
                .totalDoctors(totalDoctors)
                .stageDistribution(stageDistribution)
                .platformAvgScore(Math.round(avgScore * 100.0) / 100.0)
                .cognitiveDomainWeakness(domainWeakness)
                .totalGameAttempts(0) // populated via game-service stats if needed
                .totalIncidents(0) // populated via tracking-service if needed
                .redFlagDoctorCount((int) redFlagCount)
                .build();
    }
}
