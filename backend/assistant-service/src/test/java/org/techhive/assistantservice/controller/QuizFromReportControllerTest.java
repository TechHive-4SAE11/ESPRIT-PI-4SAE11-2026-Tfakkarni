package org.techhive.assistantservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.techhive.assistantservice.client.MedicalServiceClient;
import org.techhive.assistantservice.client.dto.QuizDTO;
import org.techhive.assistantservice.dto.AIReportDTO;
import org.techhive.assistantservice.dto.MedicalFolderDTO;
import org.techhive.assistantservice.dto.PatientDTO;
import org.techhive.assistantservice.dto.QuizGenerateRequest;
import org.techhive.assistantservice.dto.ReportBasedQuizRequest;
import org.techhive.assistantservice.service.PatientLookupService;
import org.techhive.assistantservice.service.QuizAIService;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuizFromReportControllerTest {

    private final PatientLookupService patientLookupService = mock(PatientLookupService.class);
    private final MedicalServiceClient medicalServiceClient = mock(MedicalServiceClient.class);
    private final QuizAIService quizAIService = mock(QuizAIService.class);
    private final QuizFromReportController controller = new QuizFromReportController(
            patientLookupService, medicalServiceClient, quizAIService);

    @Test
    void generateQuizFromPatientReport_shouldUsePatientFolderAndAiReportContext() {
        PatientDTO patient = patient(33L, "Leyla", "Mansouri", "leyla-keycloak", 3);
        MedicalFolderDTO folder = medicalFolder(77L);
        AIReportDTO aiReport = AIReportDTO.builder()
                .id(91L)
                .status("READY")
                .reportJson("{\"weakTopics\":[\"dates\"]}")
                .build();
        QuizDTO quiz = QuizDTO.builder().id(5L).topic("Cognitive evaluation and Memory").questions(List.of()).build();

        when(patientLookupService.findPatientByName("Leyla Mansouri")).thenReturn(patient);
        when(medicalServiceClient.getMedicalFolderByPatient("leyla-keycloak")).thenReturn(List.of(folder));
        when(medicalServiceClient.getLatestAIReport(77L)).thenReturn(aiReport);
        when(quizAIService.generateQuiz(any(QuizGenerateRequest.class))).thenReturn(quiz);

        ResponseEntity<?> response = controller.generateQuizFromPatientReport(request("Leyla Mansouri", 4));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(quiz, response.getBody());
        verify(quizAIService).generateQuiz(org.mockito.ArgumentMatchers.argThat(argument ->
                argument.getNumberOfQuestions().equals(4)
                        && argument.getDifficultyLevel().equals(3)
                        && argument.getCaregiverId().equals(1L)
                        && argument.getCustomContext().contains("Leyla Mansouri")
                        && argument.getCustomContext().contains("dates")));
    }

    @Test
    void generateQuizFromPatientReport_withoutAiReport_shouldStillGenerateQuizWithDefaults() {
        PatientDTO patient = patient(44L, "Nour", "Trabelsi", null, null);
        MedicalFolderDTO folder = medicalFolder(88L);
        QuizDTO quiz = QuizDTO.builder().id(6L).topic("Cognitive evaluation and Memory").questions(List.of()).build();

        when(patientLookupService.findPatientByName("Nour Trabelsi")).thenReturn(patient);
        when(medicalServiceClient.getMedicalFolderByPatient("44")).thenReturn(List.of(folder));
        when(medicalServiceClient.getLatestAIReport(88L)).thenThrow(new RuntimeException("missing report"));
        when(quizAIService.generateQuiz(any(QuizGenerateRequest.class))).thenReturn(quiz);

        ResponseEntity<?> response = controller.generateQuizFromPatientReport(request("Nour Trabelsi", null));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(quizAIService).generateQuiz(org.mockito.ArgumentMatchers.argThat(argument ->
                argument.getNumberOfQuestions().equals(5)
                        && argument.getDifficultyLevel().equals(2)
                        && argument.getCustomContext().contains("IA Report JSON (Points faibles & Traitements): Aucun")));
    }

    @Test
    void generateQuizFromPatientReport_withoutMedicalFolder_shouldReturn500Body() {
        PatientDTO patient = patient(55L, "Sana", "Ayari", null, 1);
        when(patientLookupService.findPatientByName("Sana Ayari")).thenReturn(patient);
        when(medicalServiceClient.getMedicalFolderByPatient("55")).thenReturn(List.of());

        ResponseEntity<?> response = controller.generateQuizFromPatientReport(request("Sana Ayari", 2));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<?, ?> body = assertInstanceOf(Map.class, response.getBody());
        assertEquals("Custom quiz generation failed", body.get("error"));
        assertEquals("No medical folder found for patient ID 55", body.get("message"));
    }

    private ReportBasedQuizRequest request(String patientName, Integer numberOfQuestions) {
        ReportBasedQuizRequest request = new ReportBasedQuizRequest();
        request.setPatientName(patientName);
        request.setNumberOfQuestions(numberOfQuestions);
        return request;
    }

    private PatientDTO patient(Long id, String firstName, String lastName, String keycloakId, Integer cognitiveLevel) {
        PatientDTO patient = new PatientDTO();
        patient.setId(id);
        patient.setFirstName(firstName);
        patient.setLastName(lastName);
        patient.setKeycloakId(keycloakId);
        patient.setAge(73);
        patient.setDiagnosis("Memory impairment");
        patient.setCognitiveLevel(cognitiveLevel);
        return patient;
    }

    private MedicalFolderDTO medicalFolder(Long id) {
        MedicalFolderDTO folder = new MedicalFolderDTO();
        folder.setId(id);
        folder.setPatientId("patient-ref");
        folder.setDiagnosis("Memory impairment");
        return folder;
    }
}
