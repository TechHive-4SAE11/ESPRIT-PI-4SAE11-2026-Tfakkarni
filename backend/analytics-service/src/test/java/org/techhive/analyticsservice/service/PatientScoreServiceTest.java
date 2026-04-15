package org.techhive.analyticsservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.analyticsservice.client.*;
import org.techhive.analyticsservice.dto.PatientScoreResponse;
import org.techhive.analyticsservice.dto.ScoreAnalyticsResponse;
import org.techhive.analyticsservice.entity.AlzheimerStage;
import org.techhive.analyticsservice.entity.PatientCompositeScore;
import org.techhive.analyticsservice.entity.ScoreHistory;
import org.techhive.analyticsservice.repository.CognitiveDomainAnalysisRepository;
import org.techhive.analyticsservice.repository.PatientCompositeScoreRepository;
import org.techhive.analyticsservice.repository.ScoreHistoryRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientScoreServiceTest {

    @Mock
    private GameServiceClient gameClient;
    @Mock
    private TrackingServiceClient trackingClient;
    @Mock
    private MedicalServiceClient medicalClient;
    @Mock
    private IotServiceClient iotClient;
    @Mock
    private AlertServiceClient alertClient;

    @Mock
    private PatientCompositeScoreRepository scoreRepository;
    @Mock
    private ScoreHistoryRepository historyRepository;
    @Mock
    private CognitiveDomainAnalysisRepository domainRepository;

    @InjectMocks
    private PatientScoreService patientScoreService;

    private final String PATIENT_ID = "patient-123";

    @Test
    @DisplayName("Should compute and save high score for healthy patient")
    void shouldComputeHighScoreForHealthyPatient() {
        // Given
        ScoreAnalyticsResponse gameResponse = ScoreAnalyticsResponse.builder()
                .averageScore(90.0)
                .build();
        when(gameClient.getScoreAnalytics(PATIENT_ID)).thenReturn(gameResponse);

        Map<String, Object> complianceData = new HashMap<>();
        complianceData.put("taken", 9);
        complianceData.put("missed", 1);
        when(trackingClient.getMedicationCompliance(eq(PATIENT_ID), anyInt())).thenReturn(complianceData);

        when(scoreRepository.findByPatientKeycloakId(PATIENT_ID)).thenReturn(Optional.empty());

        // When
        PatientScoreResponse response = patientScoreService.computeAndSave(PATIENT_ID);

        // Then
        assertThat(response.getPatientKeycloakId()).isEqualTo(PATIENT_ID);
        assertThat(response.getCognitiveScore()).isEqualTo(90.0);
        assertThat(response.getMedicalStabilityScore()).isEqualTo(90.0);
        assertThat(response.getStage()).isEqualTo(AlzheimerStage.EARLY);
        
        verify(scoreRepository).save(any(PatientCompositeScore.class));
        verify(historyRepository).save(any(ScoreHistory.class));
    }

    @Test
    @DisplayName("Should classify as severe risk when scores are very low")
    void shouldClassifyAsSevereRiskForLowScores() {
        // Given
        when(gameClient.getScoreAnalytics(PATIENT_ID)).thenReturn(null);
        when(trackingClient.getMedicationCompliance(anyString(), anyInt())).thenReturn(null);
        when(scoreRepository.findByPatientKeycloakId(PATIENT_ID)).thenReturn(Optional.empty());

        // When
        PatientScoreResponse response = patientScoreService.computeAndSave(PATIENT_ID);

        // Then
        assertThat(response.getStage()).isEqualTo(AlzheimerStage.SEVERE);
    }

    @Test
    @DisplayName("Should compute early stage for medium scores")
    void shouldComputeEarlyStageForMediumScores() {
        // Given
        ScoreAnalyticsResponse gameResponse = ScoreAnalyticsResponse.builder()
                .averageScore(75.0)
                .build();
        when(gameClient.getScoreAnalytics(PATIENT_ID)).thenReturn(gameResponse);

        Map<String, Object> complianceData = new HashMap<>();
        complianceData.put("taken", 7);
        complianceData.put("missed", 3);
        when(trackingClient.getMedicationCompliance(eq(PATIENT_ID), anyInt())).thenReturn(complianceData);

        when(scoreRepository.findByPatientKeycloakId(PATIENT_ID)).thenReturn(Optional.empty());

        // When
        PatientScoreResponse response = patientScoreService.computeAndSave(PATIENT_ID);

        // Then
        // Calculation check: 
        // Cog: 75 * 0.3 = 22.5
        // Daily: 75 * 0.25 = 18.75
        // Med: 70 * 0.2 = 14
        // IoT: 20 * 0.15 = 3
        // Eng: 80 * 0.1 = 8
        // Total: 66.25 -> MODERATE (since Stage thresholds are 85, 70, 45)
        assertThat(response.getStage()).isEqualTo(AlzheimerStage.MODERATE);
    }
}
