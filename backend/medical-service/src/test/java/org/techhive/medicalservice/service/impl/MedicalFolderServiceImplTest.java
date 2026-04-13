package org.techhive.medicalservice.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.medicalservice.dto.CreateMedicalFolderRequest;
import org.techhive.medicalservice.dto.MedicalFolderResponse;
import org.techhive.medicalservice.entity.Diagnostics;
import org.techhive.medicalservice.entity.MedicalFolder;
import org.techhive.medicalservice.entity.MedicalHistory;
import org.techhive.medicalservice.exception.ResourceNotFoundException;
import org.techhive.medicalservice.mapper.MedicalFolderMapper;
import org.techhive.medicalservice.repository.AIReportRepository;
import org.techhive.medicalservice.repository.DiagnosticsRepository;
import org.techhive.medicalservice.repository.MedicalFolderRepository;
import org.techhive.medicalservice.repository.MedicalHistoryRepository;
import org.techhive.medicalservice.service.AttendanceMonitoringService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalFolderServiceImplTest {

    @Mock
    private MedicalFolderRepository medicalFolderRepository;
    @Mock
    private DiagnosticsRepository diagnosticsRepository;
    @Mock
    private MedicalHistoryRepository medicalHistoryRepository;
    @Mock
    private AIReportRepository aiReportRepository;
    @Mock
    private MedicalFolderMapper medicalFolderMapper;

    @Mock
    private AttendanceMonitoringService attendanceMonitoringService;

    @InjectMocks
    private MedicalFolderServiceImpl medicalFolderService;

    @Test
    @DisplayName("[MedicalFolder] Create folder -> should save and map response")
    void createMedicalFolder_shouldSaveAndReturnResponse() {
        printStep("GIVEN a valid create medical folder request");
        CreateMedicalFolderRequest request = CreateMedicalFolderRequest.builder()
                .patientId("patient-1")
                .doctorId("doctor-1")
                .bloodType("A+")
                .height(175.0)
                .weight(72.0)
                .build();

        MedicalFolder toSave = MedicalFolder.builder()
                .patientId("patient-1")
                .doctorId("doctor-1")
                .bloodType("A+")
                .height(175.0)
                .weight(72.0)
                .build();
        MedicalFolder saved = MedicalFolder.builder()
                .id(10L)
                .patientId("patient-1")
                .doctorId("doctor-1")
                .build();
        MedicalFolderResponse expected = MedicalFolderResponse.builder()
                .id(10L)
                .patientId("patient-1")
                .doctorId("doctor-1")
                .build();

        when(medicalFolderMapper.toEntity(request)).thenReturn(toSave);
        when(medicalFolderRepository.save(toSave)).thenReturn(saved);
        when(medicalFolderMapper.toResponse(saved)).thenReturn(expected);

        printStep("WHEN createMedicalFolder is executed");
        MedicalFolderResponse result = medicalFolderService.createMedicalFolder(request);

        printStep("THEN response should contain saved id and patient id");
        assertNotNull(result, "Expected a non-null response after creating medical folder.");
        assertEquals(10L, result.getId(), "Expected created medical folder id to be 10.");
        assertEquals("patient-1", result.getPatientId(), "Expected response patientId to match input.");
        verify(medicalFolderRepository).save(toSave);
        verify(medicalFolderMapper).toResponse(saved);
        printStep("PASS createMedicalFolder_shouldSaveAndReturnResponse");
    }

    @Test
    @DisplayName("[MedicalFolder] Get by id -> should throw when folder is missing")
    void getMedicalFolderById_shouldThrowWhenNotFound() {
        printStep("GIVEN unknown medical folder id=404");
        when(medicalFolderRepository.findById(404L)).thenReturn(Optional.empty());

        printStep("WHEN getMedicalFolderById is executed");
        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> medicalFolderService.getMedicalFolderById(404L),
                "Expected ResourceNotFoundException for unknown folder id.");

        printStep("THEN exception message should explain folder was not found");
        assertTrue(ex.getMessage().contains("Medical folder not found"),
                "Expected exception message to mention missing medical folder.");
        verify(medicalFolderRepository).findById(404L);
        printStep("PASS getMedicalFolderById_shouldThrowWhenNotFound");
    }

    @Test
    @DisplayName("[MedicalFolder] Delete folder -> should delete dependencies before folder")
    void deleteMedicalFolder_shouldDeleteDependenciesThenFolder() {
        printStep("GIVEN folder with AI reports, diagnostics, and medical history");
        Long folderId = 7L;
        MedicalFolder folder = MedicalFolder.builder().id(folderId).patientId("p").doctorId("d").build();
        Diagnostics diag = Diagnostics.builder().id(1L).build();
        MedicalHistory history = MedicalHistory.builder().id(2L).build();
        org.techhive.medicalservice.entity.AIReport report = org.techhive.medicalservice.entity.AIReport.builder().id(3L).build();

        when(medicalFolderRepository.findById(folderId)).thenReturn(Optional.of(folder));
        when(aiReportRepository.findByMedicalFolderIdOrderByGeneratedAtDesc(folderId)).thenReturn(List.of(report));
        when(diagnosticsRepository.findByMedicalFolderId(folderId)).thenReturn(List.of(diag));
        when(medicalHistoryRepository.findByMedicalFolderId(folderId)).thenReturn(List.of(history));

        printStep("WHEN deleteMedicalFolder is executed");
        medicalFolderService.deleteMedicalFolder(folderId);

        printStep("THEN dependent entities should be deleted before folder delete");
        verify(aiReportRepository).deleteAll(List.of(report));
        verify(diagnosticsRepository).deleteAll(List.of(diag));
        verify(medicalHistoryRepository).deleteAll(List.of(history));
        verify(medicalFolderRepository).delete(folder);
        printStep("PASS deleteMedicalFolder_shouldDeleteDependenciesThenFolder");
    }

    private void printStep(String message) {
        System.out.println("[TEST][MedicalFolderServiceImplTest] " + message);
    }
}

