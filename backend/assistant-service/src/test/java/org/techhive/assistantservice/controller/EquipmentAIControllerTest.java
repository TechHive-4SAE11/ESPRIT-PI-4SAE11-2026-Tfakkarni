package org.techhive.assistantservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.techhive.assistantservice.client.MedicalServiceClient;
import org.techhive.assistantservice.dto.EquipmentRecommendRequest;
import org.techhive.assistantservice.dto.EquipmentRecommendResponse;
import org.techhive.assistantservice.dto.MedicalFolderDTO;
import org.techhive.assistantservice.dto.PatientDTO;
import org.techhive.assistantservice.dto.ReportAnalysisResult;
import org.techhive.assistantservice.service.EquipmentAIService;
import org.techhive.assistantservice.service.PatientLookupService;
import org.techhive.assistantservice.service.ReportAnalysisService;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EquipmentAIControllerTest {

    private final EquipmentAIService equipmentAIService = mock(EquipmentAIService.class);
    private final PatientLookupService patientLookupService = mock(PatientLookupService.class);
    private final MedicalServiceClient medicalServiceClient = mock(MedicalServiceClient.class);
    private final ReportAnalysisService reportAnalysisService = mock(ReportAnalysisService.class);
    private final EquipmentAIController controller = new EquipmentAIController(
            equipmentAIService, patientLookupService, medicalServiceClient, reportAnalysisService);

    @Test
    void recommendEquipment_shouldReturnServiceResponse() {
        EquipmentRecommendRequest request = equipmentRequest(88L, "MOBILITY", "MODERATE");
        EquipmentRecommendResponse serviceResponse = EquipmentRecommendResponse.builder()
                .patientId(88L)
                .condition("MOBILITY")
                .severity("MODERATE")
                .recommendations(List.of())
                .generalAdvice("Use supervised mobility aids")
                .build();
        when(equipmentAIService.recommendEquipment(request)).thenReturn(serviceResponse);

        ResponseEntity<?> response = controller.recommendEquipment(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
    }

    @Test
    void recommendEquipment_whenServiceFails_shouldReturn500Body() {
        EquipmentRecommendRequest request = equipmentRequest(88L, "MOBILITY", "MODERATE");
        when(equipmentAIService.recommendEquipment(request)).thenThrow(new RuntimeException("AI unavailable"));

        ResponseEntity<?> response = controller.recommendEquipment(request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<?, ?> body = assertInstanceOf(Map.class, response.getBody());
        assertEquals("Equipment recommendation failed", body.get("error"));
        assertEquals("AI unavailable", body.get("message"));
    }

    @Test
    void recommendEquipmentFromPatientName_whenPatientNameMissing_shouldReturnBadRequest() {
        ResponseEntity<?> response = controller.recommendEquipmentFromPatientName(Map.of("patientName", "   "));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<?, ?> body = assertInstanceOf(Map.class, response.getBody());
        assertEquals("Patient name is required", body.get("error"));
    }

    @Test
    void recommendEquipmentFromPatientName_shouldAnalyzeFolderAndRecommend() {
        PatientDTO patient = patient(31L, "Amira", "Saidi", "amira-keycloak");
        MedicalFolderDTO folder = medicalFolder();
        ReportAnalysisResult analysis = ReportAnalysisResult.builder()
                .diagnosis("MOBILITY")
                .cognitiveLevel("SEVERE")
                .weakTopics(List.of("balance", "night walking"))
                .recommendedTopics(List.of("walker", "fall prevention"))
                .build();
        EquipmentRecommendResponse recommendation = EquipmentRecommendResponse.builder()
                .patientId(31L)
                .condition("MOBILITY")
                .severity("SEVERE")
                .recommendations(List.of())
                .build();

        when(patientLookupService.findPatientByName("Amira Saidi")).thenReturn(patient);
        when(medicalServiceClient.getMedicalFolderByPatient("amira-keycloak")).thenReturn(List.of(folder));
        when(reportAnalysisService.analyzeMedicalFolder(folder)).thenReturn(analysis);
        when(equipmentAIService.recommendEquipment(any(EquipmentRecommendRequest.class))).thenReturn(recommendation);

        ResponseEntity<?> response = controller.recommendEquipmentFromPatientName(Map.of("patientName", "Amira Saidi"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(recommendation, response.getBody());
        verify(equipmentAIService).recommendEquipment(org.mockito.ArgumentMatchers.argThat(argument ->
                argument.getPatientId().equals(31L)
                        && argument.getCondition().equals("MOBILITY")
                        && argument.getSeverity().equals("SEVERE")
                        && argument.getCustomContext().contains("Amira Saidi")
                        && argument.getCustomContext().contains("fall prevention")));
    }

    @Test
    void recommendEquipmentFromPatientName_whenMedicalFolderMissing_shouldReturn500Body() {
        PatientDTO patient = patient(32L, "Karim", "Bouzid", null);
        when(patientLookupService.findPatientByName("Karim Bouzid")).thenReturn(patient);
        when(medicalServiceClient.getMedicalFolderByPatient("32")).thenReturn(List.of());

        ResponseEntity<?> response = controller.recommendEquipmentFromPatientName(Map.of("patientName", "Karim Bouzid"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<?, ?> body = assertInstanceOf(Map.class, response.getBody());
        assertEquals("Custom equipment recommendation failed", body.get("error"));
        assertEquals("No medical folder found for patient ID 32", body.get("message"));
    }

    private EquipmentRecommendRequest equipmentRequest(Long patientId, String condition, String severity) {
        return EquipmentRecommendRequest.builder()
                .patientId(patientId)
                .condition(condition)
                .severity(severity)
                .build();
    }

    private PatientDTO patient(Long id, String firstName, String lastName, String keycloakId) {
        PatientDTO patient = new PatientDTO();
        patient.setId(id);
        patient.setFirstName(firstName);
        patient.setLastName(lastName);
        patient.setAge(69);
        patient.setDiagnosis("Mobility impairment");
        patient.setKeycloakId(keycloakId);
        return patient;
    }

    private MedicalFolderDTO medicalFolder() {
        MedicalFolderDTO folder = new MedicalFolderDTO();
        folder.setId(70L);
        folder.setDiagnosis("Mobility impairment");
        return folder;
    }
}
