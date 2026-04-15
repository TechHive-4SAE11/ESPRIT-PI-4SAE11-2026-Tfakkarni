package org.techhive.analyticsservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.techhive.analyticsservice.client.MedicalServiceClient;
import org.techhive.analyticsservice.client.TrackingServiceClient;
import org.techhive.analyticsservice.client.UserServiceClient;
import org.techhive.analyticsservice.dto.DoctorMatchResponse;
import org.techhive.analyticsservice.dto.SeverePatientResponse;
import org.techhive.analyticsservice.entity.AlzheimerStage;
import org.techhive.analyticsservice.entity.DoctorEffectivenessScore;
import org.techhive.analyticsservice.entity.PatientCompositeScore;
import org.techhive.analyticsservice.repository.DoctorEffectivenessScoreRepository;
import org.techhive.analyticsservice.repository.PatientCompositeScoreRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorMatchingService {

    private final PatientCompositeScoreRepository scoreRepository;
    private final DoctorEffectivenessScoreRepository effectivenessRepository;
    private final TrackingServiceClient trackingClient;
    private final MedicalServiceClient medicalClient;
    private final UserServiceClient userClient;

    // Composite score weights
    private static final double W_STABILIZATION = 0.50;
    private static final double W_RATING = 0.30;
    private static final double W_SHOW_RATE = 0.20;

    /**
     * Returns all doctors ranked by a composite matching score.
     * Composite = stabilizationRate×0.50 + (avgRating/5×100)×0.30 + appointmentShowRate×0.20
     * Doctors with risk flags are pushed to the bottom.
     */
    public List<DoctorMatchResponse> getRankedDoctors() {
        // 1. Get all effectiveness scores
        List<DoctorEffectivenessScore> allEffectiveness = effectivenessRepository
                .findAllByOrderByStabilizationRateDesc();

        // 2. Get rating rankings from tracking-service
        Map<String, RatingInfo> ratingMap = buildRatingMap();

        // 3. Compute composite score for each doctor
        List<DoctorMatchResponse> results = new ArrayList<>();
        for (DoctorEffectivenessScore eff : allEffectiveness) {
            String docId = eff.getDoctorKeycloakId();
            RatingInfo rating = ratingMap.getOrDefault(docId, new RatingInfo(0.0, 0));

            double ratingNormalized = (rating.avgRating / 5.0) * 100.0;
            double composite = (eff.getStabilizationRate() * W_STABILIZATION)
                    + (ratingNormalized * W_RATING)
                    + (eff.getAppointmentShowRate() * W_SHOW_RATE);

            boolean hasFlags = eff.getRiskFlags() != null
                    && !eff.getRiskFlags().isEmpty()
                    && !eff.getRiskFlags().equals("[]");

            results.add(DoctorMatchResponse.builder()
                    .doctorKeycloakId(docId)
                    .doctorName(getDoctorName(docId))
                    .matchScore(Math.round(composite * 100.0) / 100.0)
                    .averageRating(rating.avgRating)
                    .totalRatings(rating.totalRatings)
                    .stabilizationRate(eff.getStabilizationRate())
                    .declineRate(eff.getDeclineRate())
                    .appointmentShowRate(eff.getAppointmentShowRate())
                    .currentPatientCount(eff.getPatientCount())
                    .hasRiskFlags(hasFlags)
                    .build());
        }

        // Sort: non-flagged first (by composite desc), then flagged (by composite desc)
        results.sort((a, b) -> {
            if (a.isHasRiskFlags() != b.isHasRiskFlags()) {
                return a.isHasRiskFlags() ? 1 : -1;
            }
            return Double.compare(b.getMatchScore(), a.getMatchScore());
        });

        return results;
    }

    /**
     * Returns the best doctor recommendation for a given stage (SEVERE or MODERATE).
     */
    public Optional<DoctorMatchResponse> recommendDoctor(AlzheimerStage stage) {
        List<DoctorMatchResponse> ranked = getRankedDoctors();
        // Return top non-flagged doctor
        return ranked.stream()
                .filter(d -> !d.isHasRiskFlags())
                .findFirst();
    }

    /**
     * Returns all patients at SEVERE or MODERATE stage with their current doctor
     * and a recommendation for a better doctor if available.
     */
    public List<SeverePatientResponse> getSeverePatientsWithRecommendations() {
        List<PatientCompositeScore> severePatients = scoreRepository.findByStageIn(
                List.of(AlzheimerStage.SEVERE, AlzheimerStage.MODERATE));

        if (severePatients.isEmpty()) {
            return Collections.emptyList();
        }

        // Pre-compute ranked doctors once
        List<DoctorMatchResponse> rankedDoctors = getRankedDoctors();
        Optional<DoctorMatchResponse> topDoctor = rankedDoctors.stream()
                .filter(d -> !d.isHasRiskFlags())
                .findFirst();

        List<SeverePatientResponse> results = new ArrayList<>();
        for (PatientCompositeScore patient : severePatients) {
            String patientId = patient.getPatientKeycloakId();

            // Find current doctor via medical folders
            DoctorInfo currentDoctor = findCurrentDoctor(patientId);

            SeverePatientResponse.SeverePatientResponseBuilder builder = SeverePatientResponse.builder()
                    .patientKeycloakId(patientId)
                    .patientName(getPatientName(patientId))
                    .stage(patient.getStage())
                    .overallScore(patient.getOverallScore() != null ? patient.getOverallScore() : 0)
                    .cognitiveScore(patient.getCognitiveScore() != null ? patient.getCognitiveScore() : 0)
                    .currentDoctorKeycloakId(currentDoctor.keycloakId)
                    .currentDoctorName(currentDoctor.name);

            // Recommend a better doctor (skip current doctor)
            Optional<DoctorMatchResponse> recommended = rankedDoctors.stream()
                    .filter(d -> !d.isHasRiskFlags())
                    .filter(d -> !d.getDoctorKeycloakId().equals(currentDoctor.keycloakId))
                    .findFirst();

            if (recommended.isEmpty()) {
                // Fall back to top doctor even if same as current
                recommended = topDoctor;
            }

            recommended.ifPresent(doc -> {
                builder.recommendedDoctorKeycloakId(doc.getDoctorKeycloakId());
                builder.recommendedDoctorName(doc.getDoctorName());
                builder.recommendedDoctorMatchScore(doc.getMatchScore());
            });

            results.add(builder.build());
        }

        // Sort by severity (SEVERE first), then by overall score ascending (worst first)
        results.sort((a, b) -> {
            int stageCmp = stageOrder(a.getStage()) - stageOrder(b.getStage());
            if (stageCmp != 0) return stageCmp;
            return Double.compare(a.getOverallScore(), b.getOverallScore());
        });

        return results;
    }

    // ─── Helpers ───

    private Map<String, RatingInfo> buildRatingMap() {
        try {
            List<Map<String, Object>> rankings = trackingClient.getDoctorRatingsRanking();
            Map<String, RatingInfo> map = new HashMap<>();
            for (Map<String, Object> entry : rankings) {
                String docId = (String) entry.get("doctorKeycloakId");
                if (docId == null) continue;
                double avg = entry.get("averageRating") != null
                        ? ((Number) entry.get("averageRating")).doubleValue() : 0.0;
                long total = entry.get("totalRatings") != null
                        ? ((Number) entry.get("totalRatings")).longValue() : 0;
                map.put(docId, new RatingInfo(avg, total));
            }
            return map;
        } catch (Exception e) {
            log.warn("Failed to fetch doctor ratings: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private DoctorInfo findCurrentDoctor(String patientKeycloakId) {
        try {
            List<Map<String, Object>> folders = medicalClient.getMedicalFoldersByPatient(patientKeycloakId);
            if (folders != null && !folders.isEmpty()) {
                // Take the most recent folder's doctor
                Map<String, Object> folder = folders.get(0);
                String doctorId = (String) folder.get("doctorId");
                if (doctorId != null) {
                    return new DoctorInfo(doctorId, getDoctorName(doctorId));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to find current doctor for patient {}: {}", patientKeycloakId, e.getMessage());
        }
        return new DoctorInfo(null, "Non assigné");
    }

    private String getDoctorName(String keycloakId) {
        try {
            Map<String, Object> user = userClient.getUserByKeycloakId(keycloakId);
            String first = (String) user.getOrDefault("firstName", "");
            String last = (String) user.getOrDefault("lastName", "");
            return (first + " " + last).trim();
        } catch (Exception e) {
            return keycloakId;
        }
    }

    private String getPatientName(String keycloakId) {
        try {
            Map<String, Object> user = userClient.getUserByKeycloakId(keycloakId);
            String first = (String) user.getOrDefault("firstName", "");
            String last = (String) user.getOrDefault("lastName", "");
            return (first + " " + last).trim();
        } catch (Exception e) {
            return keycloakId;
        }
    }

    private int stageOrder(AlzheimerStage stage) {
        return switch (stage) {
            case SEVERE -> 0;
            case MODERATE -> 1;
            case EARLY -> 2;
            case LOW_RISK -> 3;
            default -> 4;
        };
    }

    private static class RatingInfo {
        final double avgRating;
        final long totalRatings;

        RatingInfo(double avgRating, long totalRatings) {
            this.avgRating = avgRating;
            this.totalRatings = totalRatings;
        }
    }

    private static class DoctorInfo {
        final String keycloakId;
        final String name;

        DoctorInfo(String keycloakId, String name) {
            this.keycloakId = keycloakId;
            this.name = name;
        }
    }
}
