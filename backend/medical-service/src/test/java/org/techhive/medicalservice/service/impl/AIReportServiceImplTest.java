package org.techhive.medicalservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.medicalservice.client.MlServiceClient;
import org.techhive.medicalservice.dto.AIReportResponse;
import org.techhive.medicalservice.dto.ClinicalAnalysisResult;
import org.techhive.medicalservice.dto.DiagnosticsResponse;
import org.techhive.medicalservice.dto.DossierForMlRequest;
import org.techhive.medicalservice.dto.MedicalHistoryResponse;
import org.techhive.medicalservice.entity.AIReport;
import org.techhive.medicalservice.entity.MedicalFolder;
import org.techhive.medicalservice.repository.AIReportRepository;
import org.techhive.medicalservice.repository.MedicalFolderRepository;
import org.techhive.medicalservice.service.DiagnosticsService;
import org.techhive.medicalservice.service.MedicalHistoryService;

import jakarta.persistence.EntityNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIReportServiceImplTest {

    @Mock
    private AIReportRepository aiReportRepository;
    @Mock
    private MedicalFolderRepository medicalFolderRepository;
    @Mock
    private DiagnosticsService diagnosticsService;
    @Mock
    private MedicalHistoryService medicalHistoryService;
    @Mock
    private MlServiceClient mlServiceClient;

    private ObjectMapper objectMapper;
    private AIReportServiceImpl service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new AIReportServiceImpl(
                aiReportRepository,
                medicalFolderRepository,
                diagnosticsService,
                medicalHistoryService,
                mlServiceClient,
                objectMapper);
    }

    @Test
    void getByFolderIdMapsReportsAndParsesValidJson() {
        MedicalFolder folder = folder(42L);
        AIReport ready = report(7L, folder, AIReport.Status.READY, "{\"riskLevel\":\"HIGH\"}", null);
        AIReport pending = report(8L, folder, AIReport.Status.PENDING, null, null);
        when(aiReportRepository.findByMedicalFolderIdOrderByGeneratedAtDesc(42L)).thenReturn(List.of(ready, pending));

        List<AIReportResponse> responses = service.getByFolderId(42L);

        assertEquals(2, responses.size());
        assertEquals(7L, responses.get(0).getId());
        assertEquals(42L, responses.get(0).getMedicalFolderId());
        assertEquals("READY", responses.get(0).getStatus());
        assertInstanceOf(Map.class, responses.get(0).getReportJson());
        assertEquals("HIGH", ((Map<?, ?>) responses.get(0).getReportJson()).get("riskLevel"));
        assertNull(responses.get(1).getReportJson());
    }

    @Test
    void getLatestByFolderIdReturnsEmptyAndIgnoresInvalidJson() {
        when(aiReportRepository.findFirstByMedicalFolderIdOrderByGeneratedAtDesc(99L)).thenReturn(Optional.empty());
        assertTrue(service.getLatestByFolderId(99L).isEmpty());

        MedicalFolder folder = folder(42L);
        AIReport badJson = report(9L, folder, AIReport.Status.ERROR, "not-json", "bad payload");
        when(aiReportRepository.findFirstByMedicalFolderIdOrderByGeneratedAtDesc(42L)).thenReturn(Optional.of(badJson));

        AIReportResponse response = service.getLatestByFolderId(42L).orElseThrow();

        assertEquals(9L, response.getId());
        assertEquals("ERROR", response.getStatus());
        assertEquals("bad payload", response.getErrorMessage());
        assertNull(response.getReportJson());
    }

    @Test
    void generateReportRejectsMissingFolder() {
        when(medicalFolderRepository.findById(404L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> service.generateReport(404L));

        assertEquals("Medical folder not found: 404", ex.getMessage());
        verify(aiReportRepository, never()).save(any());
    }

    @Test
    void runAnalysisBuildsDossierSendsItToMlAndMarksReportReady() {
        MedicalFolder folder = folder(42L);
        AIReport report = report(77L, folder, AIReport.Status.PENDING, null, null);
        DiagnosticsResponse diagnostic = DiagnosticsResponse.builder()
                .id(11L)
                .diseaseName("Hypertension")
                .stage("Stage 1")
                .comorbidities("Diabetes")
                .diagnosisDate(LocalDateTime.of(2026, 1, 2, 3, 4))
                .build();
        MedicalHistoryResponse history = MedicalHistoryResponse.builder()
                .id(12L)
                .allergies("Penicillin")
                .conditions("Asthma")
                .surgeries("Appendectomy")
                .symptoms("Fatigue")
                .recommendedTreatment("Rest")
                .familyHistory("Cardiac")
                .createdAt(LocalDateTime.of(2026, 2, 3, 4, 5))
                .build();
        ClinicalAnalysisResult result = ClinicalAnalysisResult.builder()
                .riskLevel("MEDIUM")
                .advice("Follow up")
                .differentials(List.of("A", "B"))
                .anomalies(List.of("BP"))
                .contradictions(List.of("None"))
                .build();
        when(aiReportRepository.findById(77L)).thenReturn(Optional.of(report));
        when(diagnosticsService.getDiagnosticsByMedicalFolder(42L)).thenReturn(List.of(diagnostic));
        when(medicalHistoryService.getMedicalHistoryByMedicalFolder(42L)).thenReturn(List.of(history));
        when(mlServiceClient.analyzeDossier(any(DossierForMlRequest.class))).thenReturn(result);
        when(aiReportRepository.save(report)).thenReturn(report);

        service.runAnalysis(77L);

        ArgumentCaptor<DossierForMlRequest> requestCaptor = ArgumentCaptor.forClass(DossierForMlRequest.class);
        verify(mlServiceClient).analyzeDossier(requestCaptor.capture());
        DossierForMlRequest request = requestCaptor.getValue();
        assertEquals(42L, request.getFolderId());
        assertEquals("patient-42", request.getPatientId());
        assertEquals("doctor-42", request.getDoctorId());
        assertEquals("O+", request.getBloodType());
        assertEquals(181.0, request.getHeight());
        assertEquals(82.5, request.getWeight());
        assertEquals(1, request.getDiagnostics().size());
        assertEquals("Hypertension", request.getDiagnostics().get(0).getDiseaseName());
        assertEquals(1, request.getMedicalHistory().size());
        assertEquals("Asthma", request.getMedicalHistory().get(0).getConditions());
        assertEquals(AIReport.Status.READY, report.getStatus());
        assertNull(report.getErrorMessage());
        assertTrue(report.getReportJson().contains("MEDIUM"));
        verify(aiReportRepository).save(report);
    }

    @Test
    void runAnalysisMarksErrorWhenMlReturnsNull() {
        MedicalFolder folder = folder(42L);
        AIReport report = report(77L, folder, AIReport.Status.PENDING, null, null);
        when(aiReportRepository.findById(77L)).thenReturn(Optional.of(report));
        when(diagnosticsService.getDiagnosticsByMedicalFolder(42L)).thenReturn(List.of());
        when(medicalHistoryService.getMedicalHistoryByMedicalFolder(42L)).thenReturn(List.of());
        when(mlServiceClient.analyzeDossier(any(DossierForMlRequest.class))).thenReturn(null);

        service.runAnalysis(77L);

        assertEquals(AIReport.Status.ERROR, report.getStatus());
        assertEquals("ML service returned empty response", report.getErrorMessage());
        assertNull(report.getReportJson());
        verify(aiReportRepository).save(report);
    }

    @Test
    void runAnalysisHandlesSerializationErrorWithoutThrowing() throws Exception {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        AIReportServiceImpl serviceWithFailingMapper = new AIReportServiceImpl(
                aiReportRepository,
                medicalFolderRepository,
                diagnosticsService,
                medicalHistoryService,
                mlServiceClient,
                failingMapper);
        MedicalFolder folder = folder(42L);
        AIReport report = report(77L, folder, AIReport.Status.PENDING, null, null);
        when(aiReportRepository.findById(77L)).thenReturn(Optional.of(report));
        when(diagnosticsService.getDiagnosticsByMedicalFolder(42L)).thenReturn(List.of());
        when(medicalHistoryService.getMedicalHistoryByMedicalFolder(42L)).thenReturn(List.of());
        when(mlServiceClient.analyzeDossier(any(DossierForMlRequest.class)))
                .thenReturn(ClinicalAnalysisResult.builder().riskLevel("LOW").build());
        when(failingMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("broken json") { });

        serviceWithFailingMapper.runAnalysis(77L);

        assertEquals(AIReport.Status.ERROR, report.getStatus());
        assertTrue(report.getErrorMessage().startsWith("Serialization error: broken json"));
        verify(aiReportRepository).save(report);
    }

    @Test
    void runAnalysisAsyncCatchesFailuresAndTruncatesStoredMessage() {
        MedicalFolder folder = folder(42L);
        AIReport report = report(77L, folder, AIReport.Status.PENDING, null, null);
        String longMessage = "x".repeat(1100);
        when(aiReportRepository.findById(77L))
                .thenThrow(new IllegalStateException(longMessage))
                .thenReturn(Optional.of(report));

        service.runAnalysisAsync(77L);

        assertEquals(AIReport.Status.ERROR, report.getStatus());
        assertEquals(1024, report.getErrorMessage().length());
        assertEquals("x".repeat(1024), report.getErrorMessage());
        verify(aiReportRepository).save(report);
    }

    private static MedicalFolder folder(Long id) {
        return MedicalFolder.builder()
                .id(id)
                .patientId("patient-" + id)
                .doctorId("doctor-" + id)
                .bloodType("O+")
                .height(181.0)
                .weight(82.5)
                .createdAt(LocalDateTime.of(2025, 12, 1, 8, 30))
                .build();
    }

    private static AIReport report(Long id, MedicalFolder folder, AIReport.Status status, String reportJson, String errorMessage) {
        return AIReport.builder()
                .id(id)
                .medicalFolder(folder)
                .generatedAt(LocalDateTime.of(2026, 3, 4, 5, 6))
                .status(status)
                .reportJson(reportJson)
                .errorMessage(errorMessage)
                .build();
    }
}
