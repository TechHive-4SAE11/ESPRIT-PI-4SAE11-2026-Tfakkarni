package org.techhive.medicalservice.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.medicalservice.dto.CreateDiagnosticsRequest;
import org.techhive.medicalservice.dto.DiagnosticsResponse;
import org.techhive.medicalservice.dto.UpdateDiagnosticsRequest;
import org.techhive.medicalservice.entity.Diagnostics;
import org.techhive.medicalservice.entity.MedicalFolder;
import org.techhive.medicalservice.mapper.DiagnosticsMapper;
import org.techhive.medicalservice.repository.DiagnosticsRepository;
import org.techhive.medicalservice.repository.MedicalFolderRepository;
import org.techhive.medicalservice.service.AIReportService;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiagnosticsServiceImplTest {

    @Mock
    private DiagnosticsRepository diagnosticsRepository;
    @Mock
    private MedicalFolderRepository medicalFolderRepository;
    @Mock
    private AIReportService aiReportService;

    @InjectMocks
    private DiagnosticsServiceImpl diagnosticsService;

    @Test
    @DisplayName("[Diagnostics] Create diagnostics -> should save and trigger AI report")
    void createDiagnostics_shouldPersistAndTriggerAiReport() {
        printStep("GIVEN valid create diagnostics request linked to existing medical folder");
        Long folderId = 11L;
        CreateDiagnosticsRequest request = CreateDiagnosticsRequest.builder()
                .medicalFolderId(folderId)
                .diseaseName("Hypertension")
                .stage("Stage 1")
                .comorbidities("None")
                .diagnosisDate(LocalDateTime.now().minusDays(1))
                .build();

        MedicalFolder folder = MedicalFolder.builder().id(folderId).patientId("p1").doctorId("d1").build();
        Diagnostics mapped = Diagnostics.builder().diseaseName("Hypertension").medicalFolder(folder).build();
        Diagnostics saved = Diagnostics.builder().id(100L).diseaseName("Hypertension").medicalFolder(folder).build();
        DiagnosticsResponse response = DiagnosticsResponse.builder().id(100L).medicalFolderId(folderId).diseaseName("Hypertension").build();

        when(medicalFolderRepository.findById(folderId)).thenReturn(Optional.of(folder));
        when(diagnosticsRepository.save(mapped)).thenReturn(saved);

        try (MockedStatic<DiagnosticsMapper> mapper = mockStatic(DiagnosticsMapper.class)) {
            mapper.when(() -> DiagnosticsMapper.toEntity(request, folder)).thenReturn(mapped);
            mapper.when(() -> DiagnosticsMapper.toResponse(saved)).thenReturn(response);

            printStep("WHEN createDiagnostics is executed");
            DiagnosticsResponse result = diagnosticsService.createDiagnostics(request);

            printStep("THEN diagnostics should be returned and AI report should be requested");
            assertNotNull(result, "Expected diagnostics response after successful creation.");
            assertEquals(100L, result.getId(), "Expected diagnostics id to be 100.");
            assertEquals("Hypertension", result.getDiseaseName(), "Expected disease name to match input.");
            verify(aiReportService).generateReport(folderId);
            printStep("PASS createDiagnostics_shouldPersistAndTriggerAiReport");
        }
    }

    @Test
    @DisplayName("[Diagnostics] Update diagnostics -> should throw when diagnostics is missing")
    void updateDiagnostics_shouldThrowWhenDiagnosticsMissing() {
        printStep("GIVEN unknown diagnostics id=404");
        UpdateDiagnosticsRequest request = UpdateDiagnosticsRequest.builder()
                .diseaseName("Updated")
                .build();
        when(diagnosticsRepository.findById(404L)).thenReturn(Optional.empty());

        printStep("WHEN updateDiagnostics is executed");
        EntityNotFoundException ex = assertThrows(
                EntityNotFoundException.class,
                () -> diagnosticsService.updateDiagnostics(404L, request),
                "Expected EntityNotFoundException for missing diagnostics.");

        printStep("THEN exception message should mention missing diagnostics");
        assertTrue(ex.getMessage().contains("Diagnostics not found"),
                "Expected exception message to mention missing diagnostics.");
        printStep("PASS updateDiagnostics_shouldThrowWhenDiagnosticsMissing");
    }

    @Test
    @DisplayName("[Diagnostics] Delete diagnostics -> should throw when id is unknown")
    void deleteDiagnostics_shouldThrowWhenUnknownId() {
        printStep("GIVEN unknown diagnostics id=999");
        when(diagnosticsRepository.existsById(999L)).thenReturn(false);

        printStep("WHEN deleteDiagnostics is executed");
        EntityNotFoundException ex = assertThrows(
                EntityNotFoundException.class,
                () -> diagnosticsService.deleteDiagnostics(999L),
                "Expected EntityNotFoundException for unknown diagnostics id.");

        printStep("THEN deleteById should not be called and message should be clear");
        assertTrue(ex.getMessage().contains("Diagnostics not found"),
                "Expected exception message to mention missing diagnostics.");
        verify(diagnosticsRepository, never()).deleteById(anyLong());
        printStep("PASS deleteDiagnostics_shouldThrowWhenUnknownId");
    }

    private void printStep(String message) {
        System.out.println("[TEST][DiagnosticsServiceImplTest] " + message);
    }
}

