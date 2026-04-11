package org.techhive.analyticsservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.analyticsservice.dto.FeatureGateResponse;
import org.techhive.analyticsservice.entity.*;
import org.techhive.analyticsservice.repository.FeatureGateRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureGateService {

    private final FeatureGateRepository gateRepository;
    private final PatientScoreService scoreService;

    public FeatureGateResponse getFeatureGates(String patientKeycloakId) {
        FeatureGate gate = gateRepository.findByPatientKeycloakId(patientKeycloakId)
                .orElse(null);

        if (gate != null && gate.getComputedAt() != null
                && gate.getComputedAt().isAfter(LocalDateTime.now().minusHours(1))) {
            return toResponse(gate);
        }

        return computeAndSave(patientKeycloakId);
    }

    @Transactional
    public FeatureGateResponse computeAndSave(String patientKeycloakId) {
        log.info("Computing feature gates for patient {}", patientKeycloakId);

        var score = scoreService.getScore(patientKeycloakId);
        AlzheimerStage stage = score.getStage();

        FeatureGate gate = gateRepository.findByPatientKeycloakId(patientKeycloakId)
                .orElse(FeatureGate.builder()
                        .patientKeycloakId(patientKeycloakId)
                        .build());

        applyStageRules(gate, stage);
        gateRepository.save(gate);

        return toResponse(gate);
    }

    private void applyStageRules(FeatureGate gate, AlzheimerStage stage) {
        gate.setStage(stage);

        switch (stage) {
            case LOW_RISK -> {
                gate.setIotEnabled(false);
                gate.setIotLevel(IotLevel.DISABLED);
                gate.setGameComplexity(GameComplexity.STANDARD);
                gate.setMonitoringLevel(MonitoringLevel.OPTIONAL);
                gate.setNotificationEscalation(EscalationLevel.LOW);
                gate.setUiMode(UiMode.STANDARD);
                gate.setSafeZoneRequired(false);
                gate.setMeetingSuggestedFrequencyDays(0); // as needed
            }
            case EARLY -> {
                gate.setIotEnabled(true);
                gate.setIotLevel(IotLevel.BASIC);
                gate.setGameComplexity(GameComplexity.STANDARD);
                gate.setMonitoringLevel(MonitoringLevel.OPTIONAL);
                gate.setNotificationEscalation(EscalationLevel.MEDIUM);
                gate.setUiMode(UiMode.STANDARD);
                gate.setSafeZoneRequired(false);
                gate.setMeetingSuggestedFrequencyDays(30);
            }
            case MODERATE -> {
                gate.setIotEnabled(true);
                gate.setIotLevel(IotLevel.FULL);
                gate.setGameComplexity(GameComplexity.SIMPLIFIED);
                gate.setMonitoringLevel(MonitoringLevel.RECOMMENDED);
                gate.setNotificationEscalation(EscalationLevel.HIGH);
                gate.setUiMode(UiMode.SIMPLIFIED);
                gate.setSafeZoneRequired(true);
                gate.setMeetingSuggestedFrequencyDays(14);
            }
            case SEVERE -> {
                gate.setIotEnabled(true);
                gate.setIotLevel(IotLevel.EMERGENCY);
                gate.setGameComplexity(GameComplexity.MINIMAL);
                gate.setMonitoringLevel(MonitoringLevel.REQUIRED);
                gate.setNotificationEscalation(EscalationLevel.CRITICAL);
                gate.setUiMode(UiMode.ELDERLY_MAX);
                gate.setSafeZoneRequired(true);
                gate.setMeetingSuggestedFrequencyDays(7);
            }
            default -> {
                gate.setIotEnabled(false);
                gate.setIotLevel(IotLevel.DISABLED);
                gate.setGameComplexity(GameComplexity.STANDARD);
                gate.setMonitoringLevel(MonitoringLevel.OPTIONAL);
                gate.setNotificationEscalation(EscalationLevel.LOW);
                gate.setUiMode(UiMode.STANDARD);
                gate.setSafeZoneRequired(false);
                gate.setMeetingSuggestedFrequencyDays(0);
            }
        }
    }

    private FeatureGateResponse toResponse(FeatureGate gate) {
        return FeatureGateResponse.builder()
                .patientKeycloakId(gate.getPatientKeycloakId())
                .stage(gate.getStage())
                .iotEnabled(gate.isIotEnabled())
                .iotLevel(gate.getIotLevel())
                .gameComplexity(gate.getGameComplexity())
                .monitoringLevel(gate.getMonitoringLevel())
                .notificationEscalation(gate.getNotificationEscalation())
                .uiMode(gate.getUiMode())
                .safeZoneRequired(gate.isSafeZoneRequired())
                .meetingSuggestedFrequencyDays(gate.getMeetingSuggestedFrequencyDays())
                .computedAt(gate.getComputedAt())
                .build();
    }
}
