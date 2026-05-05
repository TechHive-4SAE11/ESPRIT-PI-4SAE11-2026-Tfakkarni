package org.techhive.medicalservice.mapper;

import org.junit.jupiter.api.Test;
import org.techhive.medicalservice.dto.CreateDiagnosticsRequest;
import org.techhive.medicalservice.dto.CreateMedicalFolderRequest;
import org.techhive.medicalservice.dto.CreateMedicalHistoryRequest;
import org.techhive.medicalservice.dto.DiagnosticsResponse;
import org.techhive.medicalservice.dto.MedicalFolderResponse;
import org.techhive.medicalservice.dto.MedicalHistoryResponse;
import org.techhive.medicalservice.dto.UpdateDiagnosticsRequest;
import org.techhive.medicalservice.dto.UpdateMedicalFolderRequest;
import org.techhive.medicalservice.dto.UpdateMedicalHistoryRequest;
import org.techhive.medicalservice.entity.AttendanceRiskLevel;
import org.techhive.medicalservice.entity.DiagnosticAttachment;
import org.techhive.medicalservice.entity.Diagnostics;
import org.techhive.medicalservice.entity.MedicalFolder;
import org.techhive.medicalservice.entity.MedicalHistory;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MedicalMappersTest {

    @Test
    void diagnosticsMapper_shouldCreateUpdateAndRenderResponsesWithAttachments() {
        MedicalFolder folder = MedicalFolder.builder().id(44L).patientId("patient-44").doctorId("doctor-1").build();
        LocalDateTime diagnosisDate = LocalDateTime.of(2026, 5, 1, 8, 0);
        CreateDiagnosticsRequest createRequest = CreateDiagnosticsRequest.builder()
                .diseaseName("Diabetes")
                .stage("Stage I")
                .comorbidities("hypertension")
                .diagnosisDate(diagnosisDate)
                .build();

        Diagnostics entity = DiagnosticsMapper.toEntity(createRequest, folder);

        assertSame(folder, entity.getMedicalFolder());
        assertEquals("Diabetes", entity.getDiseaseName());
        assertEquals("Stage I", entity.getStage());
        assertEquals("hypertension", entity.getComorbidities());
        assertEquals(diagnosisDate, entity.getDiagnosisDate());

        LocalDateTime updatedDiagnosisDate = LocalDateTime.of(2026, 5, 2, 8, 0);
        DiagnosticsMapper.toEntity(UpdateDiagnosticsRequest.builder()
                .diseaseName("Asthma")
                .stage(null)
                .comorbidities("allergy")
                .diagnosisDate(updatedDiagnosisDate)
                .build(), entity);

        assertEquals("Asthma", entity.getDiseaseName());
        assertEquals("Stage I", entity.getStage(), "null update values should keep existing values");
        assertEquals("allergy", entity.getComorbidities());
        assertEquals(updatedDiagnosisDate, entity.getDiagnosisDate());

        entity.setId(9L);
        entity.setCreatedAt(LocalDateTime.of(2026, 5, 1, 9, 0));
        entity.setUpdatedAt(LocalDateTime.of(2026, 5, 2, 9, 0));
        entity.setAttachments(List.of(DiagnosticAttachment.builder()
                .id(77L)
                .diagnostic(entity)
                .fileName("scan.png")
                .originalFileName("brain-scan.png")
                .contentType("image/png")
                .fileSize(1234L)
                .description("MRI scan")
                .fileType("MRI")
                .createdAt(LocalDateTime.of(2026, 5, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 5, 1, 11, 0))
                .build()));

        DiagnosticsResponse response = DiagnosticsMapper.toResponse(entity);

        assertEquals(9L, response.getId());
        assertEquals(44L, response.getMedicalFolderId());
        assertEquals("Asthma", response.getDiseaseName());
        assertEquals(1, response.getAttachments().size());
        assertEquals("scan.png", response.getAttachments().get(0).getFileName());
        assertEquals("brain-scan.png", response.getAttachments().get(0).getOriginalFileName());
    }

    @Test
    void diagnosticsMapper_shouldReturnEmptyAttachmentListWhenEntityAttachmentsAreNull() {
        MedicalFolder folder = MedicalFolder.builder().id(11L).build();
        Diagnostics entity = Diagnostics.builder()
                .id(12L)
                .medicalFolder(folder)
                .diseaseName("Migraine")
                .diagnosisDate(LocalDateTime.of(2026, 5, 1, 12, 0))
                .attachments(null)
                .build();

        DiagnosticsResponse response = DiagnosticsMapper.toResponse(entity);

        assertNotNull(response.getAttachments());
        assertTrue(response.getAttachments().isEmpty());
    }

    @Test
    void medicalHistoryMapper_shouldCreateUpdateAndRenderResponses() {
        MedicalFolder folder = MedicalFolder.builder().id(13L).build();
        CreateMedicalHistoryRequest createRequest = CreateMedicalHistoryRequest.builder()
                .allergies("penicillin")
                .conditions("hypertension")
                .surgeries("appendectomy")
                .symptoms("fatigue")
                .recommendedTreatment("monitoring")
                .familyHistory("diabetes")
                .build();

        MedicalHistory history = MedicalHistoryMapper.toEntity(createRequest, folder);

        assertSame(folder, history.getMedicalFolder());
        assertEquals("penicillin", history.getAllergies());
        assertEquals("hypertension", history.getConditions());
        assertEquals("appendectomy", history.getSurgeries());
        assertEquals("fatigue", history.getSymptoms());
        assertEquals("monitoring", history.getRecommendedTreatment());
        assertEquals("diabetes", history.getFamilyHistory());

        MedicalHistoryMapper.toEntity(UpdateMedicalHistoryRequest.builder()
                .allergies("latex")
                .conditions(null)
                .surgeries("none")
                .symptoms("dizziness")
                .recommendedTreatment(null)
                .familyHistory("stroke")
                .build(), history);

        assertEquals("latex", history.getAllergies());
        assertEquals("hypertension", history.getConditions(), "null update values should keep existing values");
        assertEquals("none", history.getSurgeries());
        assertEquals("dizziness", history.getSymptoms());
        assertEquals("monitoring", history.getRecommendedTreatment());
        assertEquals("stroke", history.getFamilyHistory());

        history.setId(14L);
        history.setCreatedAt(LocalDateTime.of(2026, 5, 1, 13, 0));
        history.setUpdatedAt(LocalDateTime.of(2026, 5, 2, 13, 0));
        MedicalHistoryResponse response = MedicalHistoryMapper.toResponse(history);

        assertEquals(14L, response.getId());
        assertEquals(13L, response.getMedicalFolderId());
        assertEquals("latex", response.getAllergies());
        assertEquals("stroke", response.getFamilyHistory());
    }

    @Test
    void medicalFolderMapper_shouldHandleNullsCreateUpdateAndResponseFields() {
        MedicalFolderMapper mapper = new MedicalFolderMapper();
        assertNull(mapper.toEntity((CreateMedicalFolderRequest) null));
        MedicalFolder existing = MedicalFolder.builder().patientId("existing-patient").doctorId("existing-doctor").build();
        assertSame(existing, mapper.toEntity(null, existing));
        assertNull(mapper.toEntity(UpdateMedicalFolderRequest.builder().patientId("ignored").build(), null));
        assertNull(mapper.toResponse(null));

        CreateMedicalFolderRequest createRequest = CreateMedicalFolderRequest.builder()
                .patientId("patient-1")
                .doctorId("doctor-1")
                .bloodType("O+")
                .height(171.5)
                .weight(68.0)
                .build();
        MedicalFolder folder = mapper.toEntity(createRequest);

        assertEquals("patient-1", folder.getPatientId());
        assertEquals("doctor-1", folder.getDoctorId());
        assertEquals("O+", folder.getBloodType());
        assertEquals(171.5, folder.getHeight());
        assertEquals(68.0, folder.getWeight());

        mapper.toEntity(UpdateMedicalFolderRequest.builder()
                .patientId("patient-2")
                .doctorId(null)
                .bloodType("A-")
                .height(172.0)
                .weight(null)
                .build(), folder);

        assertEquals("patient-2", folder.getPatientId());
        assertEquals("doctor-1", folder.getDoctorId());
        assertEquals("A-", folder.getBloodType());
        assertEquals(172.0, folder.getHeight());
        assertEquals(68.0, folder.getWeight());

        folder.setId(33L);
        folder.setConsecutiveNoShows(2);
        folder.setTotalNoShows(4);
        folder.setBookingRestricted(true);
        folder.setRestrictionReason("review needed");
        folder.setManualReviewRequired(true);
        folder.setAttendanceRiskLevel(AttendanceRiskLevel.WARNING);
        folder.setAttendanceRestrictionOverridden(true);
        folder.setCreatedAt(LocalDateTime.of(2026, 5, 1, 14, 0));
        folder.setUpdatedAt(LocalDateTime.of(2026, 5, 2, 14, 0));

        MedicalFolderResponse response = mapper.toResponse(folder);

        assertEquals(33L, response.getId());
        assertEquals("patient-2", response.getPatientId());
        assertEquals("doctor-1", response.getDoctorId());
        assertEquals(2, response.getConsecutiveNoShows());
        assertEquals(4, response.getTotalNoShows());
        assertTrue(response.isBookingRestricted());
        assertTrue(response.isManualReviewRequired());
        assertEquals(AttendanceRiskLevel.WARNING, response.getAttendanceRiskLevel());
        assertTrue(response.isAttendanceRestrictionOverridden());
    }
}
