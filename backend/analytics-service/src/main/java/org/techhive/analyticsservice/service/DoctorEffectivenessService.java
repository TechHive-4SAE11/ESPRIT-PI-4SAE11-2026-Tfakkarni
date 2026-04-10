package org.techhive.analyticsservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.analyticsservice.client.*;
import org.techhive.analyticsservice.dto.DoctorEffectivenessResponse;
import org.techhive.analyticsservice.entity.*;
import org.techhive.analyticsservice.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorEffectivenessService {

    private final UserServiceClient userClient;
    private final MedicalServiceClient medicalClient;
    private final PatientCompositeScoreRepository scoreRepository;
    private final ScoreHistoryRepository historyRepository;
    private final DoctorEffectivenessScoreRepository effectivenessRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public DoctorEffectivenessResponse computeForDoctor(String doctorKeycloakId) {
        log.info("Computing effectiveness for doctor {}", doctorKeycloakId);

        // Get doctor's patients via medical folders
        List<String> patientIds = getPatientIdsForDoctor(doctorKeycloakId);
        int patientCount = patientIds.size();

        if (patientCount == 0) {
            return buildEmptyResponse(doctorKeycloakId);
        }

        // Compute metrics
        int improving = 0;
        int declining = 0;
        int stable = 0;

        for (String patientId : patientIds) {
            Optional<PatientCompositeScore> scoreOpt = scoreRepository.findByPatientKeycloakId(patientId);
            if (scoreOpt.isEmpty()) continue;

            ScoreTrend trend = scoreOpt.get().getScoreTrend();
            if (trend == ScoreTrend.IMPROVING) improving++;
            else if (trend == ScoreTrend.DECLINING) declining++;
            else stable++;
        }

        int assessed = improving + declining + stable;
        double stabilizationRate = assessed > 0 ? ((improving + stable) * 100.0 / assessed) : 0;
        double declineRate = assessed > 0 ? (declining * 100.0 / assessed) : 0;

        // Appointment show rate
        double appointmentShowRate = computeAppointmentShowRate(doctorKeycloakId, patientIds);

        // Coaching completion
        double coachingCompletion = computeCoachingCompletion(doctorKeycloakId, patientIds);

        // Detect red flags
        List<String> flags = new ArrayList<>();
        if (declineRate > 50) flags.add("HIGH_DECLINE_RATE");
        if (appointmentShowRate < 40) flags.add("LOW_APPOINTMENT_ATTENDANCE");
        if (patientCount > 5 && coachingCompletion < 20) flags.add("LOW_COACHING_ENGAGEMENT");
        if (declineRate > 70 && patientCount >= 3) flags.add("POTENTIAL_CARE_QUALITY_ISSUE");

        String flagsJson;
        try {
            flagsJson = flags.isEmpty() ? null : objectMapper.writeValueAsString(flags);
        } catch (JsonProcessingException e) {
            flagsJson = null;
        }

        DoctorEffectivenessScore entity = effectivenessRepository
                .findByDoctorKeycloakId(doctorKeycloakId)
                .orElse(DoctorEffectivenessScore.builder()
                        .doctorKeycloakId(doctorKeycloakId)
                        .build());

        entity.setPatientCount(patientCount);
        entity.setStabilizationRate(stabilizationRate);
        entity.setDeclineRate(declineRate);
        entity.setCoachingCompletionRate(coachingCompletion);
        entity.setAppointmentShowRate(appointmentShowRate);
        entity.setRiskFlags(flagsJson);
        effectivenessRepository.save(entity);

        return toResponse(entity, getDoctorName(doctorKeycloakId));
    }

    public DoctorEffectivenessResponse getEffectiveness(String doctorKeycloakId) {
        Optional<DoctorEffectivenessScore> existing = effectivenessRepository
                .findByDoctorKeycloakId(doctorKeycloakId);
        if (existing.isPresent() && existing.get().getComputedAt() != null
                && existing.get().getComputedAt().isAfter(LocalDateTime.now().minusHours(6))) {
            return toResponse(existing.get(), getDoctorName(doctorKeycloakId));
        }
        return computeForDoctor(doctorKeycloakId);
    }

    public List<DoctorEffectivenessResponse> getDoctorRanking() {
        return effectivenessRepository.findAllByOrderByStabilizationRateDesc().stream()
                .map(e -> toResponse(e, getDoctorName(e.getDoctorKeycloakId())))
                .collect(Collectors.toList());
    }

    public List<DoctorEffectivenessResponse> getRedFlags() {
        return effectivenessRepository.findByRiskFlagsIsNotNull().stream()
                .filter(e -> e.getRiskFlags() != null && !e.getRiskFlags().equals("[]"))
                .map(e -> toResponse(e, getDoctorName(e.getDoctorKeycloakId())))
                .collect(Collectors.toList());
    }

    // ─── Helpers ───

    private List<String> getPatientIdsForDoctor(String doctorKeycloakId) {
        try {
            // Medical folders link doctorId to patientId
            // We need to find all folders for this doctor
            // The medical-service getMedicalFoldersByPatient doesn't help here directly
            // We'll use the user-service to get all patients, then cross-reference
            List<Map<String, Object>> patients = userClient.getUsersByRole("patient");
            List<String> patientIds = new ArrayList<>();

            for (Map<String, Object> patient : patients) {
                String patientKeycloakId = (String) patient.get("keycloakId");
                if (patientKeycloakId == null) continue;

                List<Map<String, Object>> folders = medicalClient.getMedicalFoldersByPatient(patientKeycloakId);
                boolean isDoctorsPatient = folders.stream()
                        .anyMatch(f -> doctorKeycloakId.equals(f.get("doctorId")));
                if (isDoctorsPatient) {
                    patientIds.add(patientKeycloakId);
                }
            }
            return patientIds;
        } catch (Exception e) {
            log.warn("Failed to get patients for doctor {}: {}", doctorKeycloakId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private double computeAppointmentShowRate(String doctorKeycloakId, List<String> patientIds) {
        try {
            int totalCompleted = 0;
            int totalScheduled = 0;
            for (String patientId : patientIds) {
                List<Map<String, Object>> appointments = medicalClient.getAppointmentsByPatient(patientId);
                for (Map<String, Object> apt : appointments) {
                    String status = (String) apt.get("status");
                    if ("COMPLETED".equals(status)) totalCompleted++;
                    if (status != null) totalScheduled++;
                }
            }
            return totalScheduled > 0 ? (totalCompleted * 100.0 / totalScheduled) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private double computeCoachingCompletion(String doctorKeycloakId, List<String> patientIds) {
        try {
            int totalGoals = 0;
            int completedGoals = 0;
            for (String patientId : patientIds) {
                List<Map<String, Object>> folders = medicalClient.getMedicalFoldersByPatient(patientId);
                for (Map<String, Object> folder : folders) {
                    Object folderId = folder.get("id");
                    if (folderId == null) continue;
                    Long id = ((Number) folderId).longValue();
                    List<Map<String, Object>> goals = medicalClient.getCoachingGoals(id);
                    totalGoals += goals.size();
                    completedGoals += goals.stream()
                            .filter(g -> "COMPLETED".equals(g.get("status")))
                            .count();
                }
            }
            return totalGoals > 0 ? (completedGoals * 100.0 / totalGoals) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private String getDoctorName(String doctorKeycloakId) {
        try {
            Map<String, Object> user = userClient.getUserByKeycloakId(doctorKeycloakId);
            String first = (String) user.getOrDefault("firstName", "");
            String last = (String) user.getOrDefault("lastName", "");
            return (first + " " + last).trim();
        } catch (Exception e) {
            return doctorKeycloakId;
        }
    }

    @SuppressWarnings("unchecked")
    private DoctorEffectivenessResponse toResponse(DoctorEffectivenessScore entity, String doctorName) {
        List<String> flags = Collections.emptyList();
        if (entity.getRiskFlags() != null) {
            try {
                flags = objectMapper.readValue(entity.getRiskFlags(), List.class);
            } catch (JsonProcessingException ignored) {}
        }

        return DoctorEffectivenessResponse.builder()
                .doctorKeycloakId(entity.getDoctorKeycloakId())
                .doctorName(doctorName)
                .patientCount(entity.getPatientCount())
                .stabilizationRate(entity.getStabilizationRate())
                .declineRate(entity.getDeclineRate())
                .avgComplianceImprovement(entity.getAvgComplianceImprovement())
                .sessionFrequency(entity.getSessionFrequency())
                .coachingCompletionRate(entity.getCoachingCompletionRate())
                .appointmentShowRate(entity.getAppointmentShowRate())
                .riskFlags(flags)
                .computedAt(entity.getComputedAt())
                .build();
    }

    private DoctorEffectivenessResponse buildEmptyResponse(String doctorKeycloakId) {
        return DoctorEffectivenessResponse.builder()
                .doctorKeycloakId(doctorKeycloakId)
                .doctorName(getDoctorName(doctorKeycloakId))
                .patientCount(0)
                .stabilizationRate(0.0)
                .declineRate(0.0)
                .riskFlags(Collections.emptyList())
                .build();
    }
}
