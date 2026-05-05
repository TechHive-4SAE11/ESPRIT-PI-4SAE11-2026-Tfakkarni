package org.techhive.medicalservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.techhive.medicalservice.client.TrackingServiceClient;
import org.techhive.medicalservice.client.UserServiceClient;
import org.techhive.medicalservice.config.GeminiSafetyAuditProperties;
import org.techhive.medicalservice.dto.ClinicalSafetyStatsDto;
import org.techhive.medicalservice.dto.CrossPatientDiseaseDto;
import org.techhive.medicalservice.dto.DiagnosticsByMonthDto;
import org.techhive.medicalservice.dto.DiseaseCountDto;
import org.techhive.medicalservice.dto.FlaggedPatientDto;
import org.techhive.medicalservice.dto.FolderSpecificStatsDto;
import org.techhive.medicalservice.dto.MonthComparisonDto;
import org.techhive.medicalservice.dto.audit.PatientMedicationAuditRequest;
import org.techhive.medicalservice.dto.audit.PatientMedicationAuditResponse;
import org.techhive.medicalservice.dto.audit.PatientMedicationSummaryDto;
import org.techhive.medicalservice.entity.AttendanceRiskLevel;
import org.techhive.medicalservice.entity.Diagnostics;
import org.techhive.medicalservice.entity.MedicalFolder;
import org.techhive.medicalservice.repository.DiagnosticsRepository;
import org.techhive.medicalservice.repository.MedicalFolderRepository;
import org.techhive.medicalservice.repository.MedicalHistoryRepository;
import org.techhive.medicalservice.service.safety.GeminiSafetyAuditService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DossierAnalyticsServiceImplTest {

    @Mock
    private DiagnosticsRepository diagnosticsRepository;
    @Mock
    private MedicalFolderRepository medicalFolderRepository;
    @Mock
    private MedicalHistoryRepository medicalHistoryRepository;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private TrackingServiceClient trackingServiceClient;
    @Mock
    private GeminiSafetyAuditService geminiSafetyAuditService;

    private DossierAnalyticsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DossierAnalyticsServiceImpl(
                diagnosticsRepository,
                medicalFolderRepository,
                medicalHistoryRepository,
                userServiceClient,
                trackingServiceClient,
                new ObjectMapper(),
                geminiSafetyAuditService,
                geminiProperties(false, 10));
    }

    @Test
    void getTopDiseases_mapsRepositoryRowsUsingRequestedLimit() {
        when(diagnosticsRepository.findDiseaseCounts(PageRequest.of(0, 2)))
                .thenReturn(List.of(new Object[] { "Diabetes", 7L }, new Object[] { "Asthma", 3 }));

        List<DiseaseCountDto> result = service.getTopDiseases(2);

        assertEquals(2, result.size());
        assertEquals("Diabetes", result.get(0).getDiseaseName());
        assertEquals(7L, result.get(0).getCount());
        assertEquals("Asthma", result.get(1).getDiseaseName());
        assertEquals(3L, result.get(1).getCount());
        verify(diagnosticsRepository).findDiseaseCounts(PageRequest.of(0, 2));
    }

    @Test
    void getDiagnosticsByMonth_filtersRequestedYearAndMapsCounts() {
        when(diagnosticsRepository.findDiagnosticsCountByMonthAndDisease()).thenReturn(List.of(
                new Object[] { 2026, 1, "Alzheimer", 2L },
                new Object[] { 2025, 12, "Asthma", 9L },
                new Object[] { 2026, 2, "Diabetes", 4 }));

        List<DiagnosticsByMonthDto> result = service.getDiagnosticsByMonth(2026);

        assertEquals(2, result.size());
        assertEquals(2026, result.get(0).getYear());
        assertEquals(1, result.get(0).getMonth());
        assertEquals("Alzheimer", result.get(0).getDiseaseName());
        assertEquals(2L, result.get(0).getCount());
        assertEquals("Diabetes", result.get(1).getDiseaseName());
        assertEquals(4L, result.get(1).getCount());
    }

    @Test
    void getMonthComparison_collectsCurrentAndPreviousMonthCounters() {
        when(diagnosticsRepository.countByDiagnosisDateAfter(any(LocalDateTime.class))).thenReturn(11L);
        when(diagnosticsRepository.countByDiagnosisDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(5L);
        when(medicalFolderRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(8L);
        when(medicalFolderRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(4L);

        MonthComparisonDto result = service.getMonthComparison();

        assertEquals(11L, result.getThisMonthDiagnostics());
        assertEquals(5L, result.getLastMonthDiagnostics());
        assertEquals(8L, result.getThisMonthFolders());
        assertEquals(4L, result.getLastMonthFolders());
    }

    @Test
    void getByDisease_returnsEmptyForBlankDiseaseNameWithoutRepositoryCall() {
        assertTrue(service.getByDisease("   ", "stage-1").isEmpty());

        verifyNoInteractions(userServiceClient);
        verify(diagnosticsRepository, never()).findByDiseaseNameContainingIgnoreCaseAndOptionalStage(any(), any());
    }

    @Test
    void getByDisease_trimsSearchAndStageAndEnrichesDisplayNames() {
        MedicalFolder folder = MedicalFolder.builder()
                .id(22L)
                .patientId("patient-1")
                .doctorId("doctor-1")
                .build();
        Diagnostics diagnostic = Diagnostics.builder()
                .id(99L)
                .medicalFolder(folder)
                .diseaseName("Diabetes")
                .stage("Stage II")
                .diagnosisDate(LocalDateTime.of(2026, 5, 1, 10, 30))
                .build();
        when(diagnosticsRepository.findByDiseaseNameContainingIgnoreCaseAndOptionalStage("diab", "Stage II"))
                .thenReturn(List.of(diagnostic));
        when(userServiceClient.getUserByKeycloakId("patient-1")).thenReturn(userJson("Nadia", "Trabelsi"));
        when(userServiceClient.getUserByKeycloakId("doctor-1")).thenReturn(userJson("Sami", "Mansouri"));

        List<CrossPatientDiseaseDto> result = service.getByDisease(" diab ", " Stage II ");

        assertEquals(1, result.size());
        CrossPatientDiseaseDto row = result.get(0);
        assertEquals(99L, row.getDiagnosticsId());
        assertEquals(22L, row.getMedicalFolderId());
        assertEquals("patient-1", row.getPatientId());
        assertEquals("Nadia Trabelsi", row.getPatientDisplayName());
        assertEquals("doctor-1", row.getDoctorId());
        assertEquals("Sami Mansouri", row.getDoctorDisplayName());
    }

    @Test
    void getClinicalSafetyStats_combinesTrackingAuditRulesAndGeminiOutput() {
        MedicalFolder patientWithConflicts = MedicalFolder.builder().id(1L).patientId("patient-a").doctorId("doctor-a").build();
        MedicalFolder untreatedChronicPatient = MedicalFolder.builder().id(2L).patientId("patient-b").doctorId("doctor-b").build();
        Diagnostics reflux = Diagnostics.builder()
                .id(1L)
                .medicalFolder(patientWithConflicts)
                .diseaseName("Reflux disease")
                .comorbidities("bleeding risk")
                .build();
        Diagnostics dementia = Diagnostics.builder()
                .id(2L)
                .medicalFolder(untreatedChronicPatient)
                .diseaseName("Alzheimer dementia")
                .comorbidities("memory loss")
                .build();
        when(diagnosticsRepository.findAll()).thenReturn(List.of(reflux, dementia));
        when(medicalFolderRepository.findAll()).thenReturn(List.of(patientWithConflicts, untreatedChronicPatient));
        when(trackingServiceClient.getPatientMedications(any(PatientMedicationAuditRequest.class)))
                .thenReturn(PatientMedicationAuditResponse.builder().patients(Map.of(
                        "patient-a", PatientMedicationSummaryDto.builder()
                                .totalActiveMedications(5)
                                .activeMedicationNames(List.of("ibuprofen", "warfarin", "aspirin", "donepezil", "aricept"))
                                .build()))
                        .build());
        when(geminiSafetyAuditService.isEnabled()).thenReturn(true);
        ObjectNode gemini = JsonNodeFactory.instance.objectNode();
        gemini.putArray("chronicAlerts").addObject().put("patientId", "patient-b");
        gemini.putArray("conflicts").addObject()
                .put("patientId", "patient-b")
                .put("medicationName", "none")
                .put("conflictingCondition", "untreated dementia")
                .put("severity", "LOW");
        when(geminiSafetyAuditService.analyzePatientPool(any(String.class))).thenReturn(Optional.of(gemini));
        when(userServiceClient.getUserByKeycloakId("patient-a")).thenReturn(userJson("Imen", "Kefi"));
        when(userServiceClient.getUserByKeycloakId("patient-b")).thenReturn(userJson("Rania", "Jebali"));

        ClinicalSafetyStatsDto result = service.getClinicalSafetyStats();

        assertEquals(50.0, result.getTreatmentCoverageRate());
        assertEquals(1L, result.getPolypharmacyRiskCount());
        assertEquals(1L, result.getChronicMonitoringAlerts());
        assertTrue(result.isGeminiEnriched());
        assertTrue(result.getGeminiNote().contains("Google Gemini"));
        assertTrue(result.getPotentialConflicts().stream()
                .anyMatch(c -> c.getPatientId().equals("patient-a")
                        && c.getMedicationName().equals("Ibuprofen")
                        && c.getConflictingCondition().contains("acid-related")));
        assertTrue(result.getPotentialConflicts().stream()
                .anyMatch(c -> c.getMedicationName().equals("Warfarin + Aspirin")));
        assertTrue(result.getPotentialConflicts().stream()
                .anyMatch(c -> c.getMedicationName().equals("Donepezil + Aricept")));
        assertTrue(result.getPotentialConflicts().stream()
                .anyMatch(c -> c.getPatientId().equals("patient-b")
                        && c.getConflictingCondition().contains("AI review")
                        && c.getSeverity().equals("MEDIUM")));
        assertTrue(result.getPotentialConflicts().stream()
                .anyMatch(c -> "Imen Kefi".equals(c.getPatientDisplayName())));
    }

    @Test
    void getClinicalSafetyStats_returnsEmptyAuditWhenTrackingClientFails() {
        MedicalFolder folder = MedicalFolder.builder().id(5L).patientId("patient-c").build();
        Diagnostics diagnostic = Diagnostics.builder().id(6L).medicalFolder(folder).diseaseName("Asthma").build();
        when(diagnosticsRepository.findAll()).thenReturn(List.of(diagnostic));
        when(medicalFolderRepository.findAll()).thenReturn(List.of(folder));
        when(trackingServiceClient.getPatientMedications(any(PatientMedicationAuditRequest.class)))
                .thenThrow(new RuntimeException("tracking offline"));
        when(geminiSafetyAuditService.isEnabled()).thenReturn(false);

        ClinicalSafetyStatsDto result = service.getClinicalSafetyStats();

        assertEquals(0.0, result.getTreatmentCoverageRate());
        assertEquals(0L, result.getPolypharmacyRiskCount());
        assertEquals(1L, result.getChronicMonitoringAlerts());
        assertFalse(result.isGeminiEnriched());
        assertTrue(result.getGeminiNote().contains("AI assist off"));
    }

    @Test
    void getFolderStats_returnsNullWhenFolderMissing() {
        when(medicalFolderRepository.findById(404L)).thenReturn(Optional.empty());

        assertNull(service.getFolderStats(404L));
    }

    @Test
    void getFolderStats_buildsTimelineSeverityAndMedicationCoverageFromTracking() {
        MedicalFolder folder = MedicalFolder.builder().id(7L).patientId("patient-z").build();
        Diagnostics diabetes = Diagnostics.builder()
                .id(1L)
                .medicalFolder(folder)
                .diseaseName("Metformin")
                .stage("HIGH")
                .diagnosisDate(LocalDateTime.of(2026, 1, 2, 9, 0))
                .build();
        Diagnostics asthma = Diagnostics.builder()
                .id(2L)
                .medicalFolder(folder)
                .diseaseName("Asthma")
                .stage("LOW")
                .diagnosisDate(LocalDateTime.of(2026, 1, 1, 9, 0))
                .build();
        when(medicalFolderRepository.findById(7L)).thenReturn(Optional.of(folder));
        when(diagnosticsRepository.findByMedicalFolderId(7L)).thenReturn(List.of(diabetes, asthma));
        when(medicalHistoryRepository.findByMedicalFolderId(7L)).thenReturn(List.of(mock(org.techhive.medicalservice.entity.MedicalHistory.class)));
        when(trackingServiceClient.getMedicalFoldersByPatientId("patient-z")).thenReturn(new ObjectMapper().createArrayNode()
                .add(JsonNodeFactory.instance.objectNode().put("id", 70L)));
        when(trackingServiceClient.getSessionsByFolderId(70L)).thenReturn(new ObjectMapper().createArrayNode()
                .add(JsonNodeFactory.instance.objectNode().put("id", 700L).put("createdAt", "2026-01-03T08:00:00")));
        ObjectNode prescription = JsonNodeFactory.instance.objectNode();
        prescription.putArray("medications").addObject().put("medicationName", "metformin");
        when(trackingServiceClient.getPrescriptionsBySessionId(700L)).thenReturn(new ObjectMapper().createArrayNode().add(prescription));

        FolderSpecificStatsDto result = service.getFolderStats(7L);

        assertEquals(2L, result.getTotalDiagnostics());
        assertEquals(1L, result.getTotalMedicalHistory());
        assertEquals(50.0, result.getTreatmentCoverageRate());
        assertEquals(1L, result.getSeverityDistribution().get("HIGH"));
        assertEquals("Asthma", result.getTimeline().get(0).getDiseaseName());
        assertEquals("metformin", result.getPrescriptions().get(0).getMedicationName());
    }

    @Test
    void getFlaggedPatients_returnsSortedRowsWithDisplayNames() {
        MedicalFolder highRisk = MedicalFolder.builder()
                .id(1L)
                .patientId("patient-high")
                .consecutiveNoShows(3)
                .attendanceRiskLevel(AttendanceRiskLevel.RESTRICTED)
                .bookingRestricted(true)
                .manualReviewRequired(true)
                .restrictionReason("three no-shows")
                .build();
        MedicalFolder lowRisk = MedicalFolder.builder()
                .id(2L)
                .patientId("patient-low")
                .consecutiveNoShows(1)
                .attendanceRiskLevel(AttendanceRiskLevel.WARNING)
                .build();
        MedicalFolder normal = MedicalFolder.builder()
                .id(3L)
                .patientId("patient-ok")
                .attendanceRiskLevel(AttendanceRiskLevel.NONE)
                .build();
        when(medicalFolderRepository.findAll()).thenReturn(List.of(lowRisk, normal, highRisk));
        when(userServiceClient.getUserByKeycloakId("patient-high")).thenReturn(userJson("Salma", "Gharbi"));
        when(userServiceClient.getUserByKeycloakId("patient-low")).thenReturn(userJson("Youssef", "Dridi"));

        List<FlaggedPatientDto> result = service.getFlaggedPatients();

        assertEquals(2, result.size());
        assertEquals("patient-high", result.get(0).getPatientId());
        assertEquals("Salma Gharbi", result.get(0).getPatientDisplayName());
        assertEquals(3, result.get(0).getConsecutiveNoShows());
        assertTrue(result.get(0).isBookingRestricted());
        assertEquals("patient-low", result.get(1).getPatientId());
        assertEquals("Youssef Dridi", result.get(1).getPatientDisplayName());
    }

    private static ObjectNode userJson(String firstName, String lastName) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("firstName", firstName);
        node.put("lastName", lastName);
        return node;
    }

    private static GeminiSafetyAuditProperties geminiProperties(boolean enabled, int maxPatientsPerRequest) {
        GeminiSafetyAuditProperties properties = new GeminiSafetyAuditProperties();
        properties.setEnabled(enabled);
        properties.setMaxPatientsPerRequest(maxPatientsPerRequest);
        return properties;
    }
}
