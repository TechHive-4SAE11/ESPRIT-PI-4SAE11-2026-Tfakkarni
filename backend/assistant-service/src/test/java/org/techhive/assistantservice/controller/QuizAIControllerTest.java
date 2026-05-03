package org.techhive.assistantservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.techhive.assistantservice.client.MedicalServiceClient;
import org.techhive.assistantservice.client.dto.QuizDTO;
import org.techhive.assistantservice.dto.MedicalFolderDTO;
import org.techhive.assistantservice.dto.PatientDTO;
import org.techhive.assistantservice.dto.ReportAnalysisResult;
import org.techhive.assistantservice.service.PatientLookupService;
import org.techhive.assistantservice.service.QuizAIService;
import org.techhive.assistantservice.service.ReportAnalysisService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QuizAIController.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class QuizAIControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QuizAIService quizAIService;

    @MockBean
    private PatientLookupService patientLookupService;

    @MockBean
    private MedicalServiceClient medicalServiceClient;

    @MockBean
    private ReportAnalysisService reportAnalysisService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void generateQuiz_shouldReturn201() throws Exception {
        QuizDTO quizDTO = QuizDTO.builder()
                .id(1L)
                .topic("Memory Training")
                .questions(List.of())
                .build();

        when(quizAIService.generateQuiz(any())).thenReturn(quizDTO);

        String json = """
                {
                  "topic": "Memory Training",
                  "numberOfQuestions": 5,
                  "difficultyLevel": 1,
                  "caregiverId": 10
                }
                """;

        mockMvc.perform(post("/api/ai/quiz/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.topic").value("Memory Training"));
    }

    @Test
    void generateQuiz_whenServiceFails_shouldReturn500() throws Exception {
        when(quizAIService.generateQuiz(any())).thenThrow(new RuntimeException("AI error"));

        String json = """
                {
                  "topic": "Memory Training",
                  "numberOfQuestions": 5,
                  "difficultyLevel": 1,
                  "caregiverId": 10
                }
                """;

        mockMvc.perform(post("/api/ai/quiz/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void generateQuizFromPatientName_shouldUseAnalyzedFolderContext() throws Exception {
        PatientDTO patient = new PatientDTO();
        patient.setId(71L);
        patient.setKeycloakId("salma-keycloak");
        patient.setFirstName("Salma");
        patient.setLastName("Jaziri");
        patient.setAge(72);
        patient.setDiagnosis("Mild dementia");

        MedicalFolderDTO folder = new MedicalFolderDTO();
        folder.setId(17L);
        folder.setDiagnosis("Original folder diagnosis");

        ReportAnalysisResult analysis = ReportAnalysisResult.builder()
                .difficultyLevel(3)
                .recommendedTopics(List.of("daily objects"))
                .weakTopics(List.of("orientation", "recall"))
                .cognitiveLevel("AVANCE")
                .diagnosis("Mild dementia")
                .build();

        QuizDTO quiz = QuizDTO.builder()
                .id(9L)
                .topic("daily objects")
                .questions(List.of())
                .build();

        when(patientLookupService.findPatientByName("Salma Jaziri")).thenReturn(patient);
        when(medicalServiceClient.getMedicalFolderRaw("salma-keycloak")).thenReturn("[]");
        when(medicalServiceClient.getMedicalFolderByPatient("salma-keycloak")).thenReturn(List.of(folder));
        when(reportAnalysisService.analyzeMedicalFolder(any(MedicalFolderDTO.class))).thenReturn(analysis);
        when(quizAIService.generateQuiz(any())).thenReturn(quiz);

        String json = """
                {
                  "patientName": "Salma Jaziri",
                  "numberOfQuestions": 6
                }
                """;

        mockMvc.perform(post("/api/ai/quiz/generate-from-patient-name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topic").value("daily objects"));

        verify(quizAIService).generateQuiz(org.mockito.ArgumentMatchers.argThat(argument ->
                argument.getTopic().equals("daily objects")
                        && argument.getNumberOfQuestions().equals(6)
                        && argument.getDifficultyLevel().equals(3)
                        && argument.getCaregiverId().equals(71L)
                        && argument.getCustomContext().contains("Salma Jaziri")
                        && argument.getCustomContext().contains("orientation, recall")));
    }

    @Test
    void generateQuizFromPatientName_withExplicitDifficultyAndNoTopics_shouldUseDefaults() throws Exception {
        PatientDTO patient = new PatientDTO();
        patient.setId(72L);
        patient.setFirstName("Rania");
        patient.setLastName("Mejri");
        patient.setAge(68);
        patient.setDiagnosis("Memory impairment");

        MedicalFolderDTO folder = new MedicalFolderDTO();
        folder.setId(18L);

        ReportAnalysisResult analysis = ReportAnalysisResult.builder()
                .difficultyLevel(1)
                .recommendedTopics(List.of())
                .weakTopics(null)
                .cognitiveLevel("DEBUTANT")
                .diagnosis("Memory impairment")
                .build();

        QuizDTO quiz = QuizDTO.builder()
                .id(10L)
                .topic("mémoire et cognition")
                .questions(List.of())
                .build();

        when(patientLookupService.findPatientByName("Rania Mejri")).thenReturn(patient);
        when(medicalServiceClient.getMedicalFolderRaw("72")).thenThrow(new RuntimeException("raw unavailable"));
        when(medicalServiceClient.getMedicalFolderByPatient("72")).thenReturn(List.of(folder));
        when(reportAnalysisService.analyzeMedicalFolder(any(MedicalFolderDTO.class))).thenReturn(analysis);
        when(quizAIService.generateQuiz(any())).thenReturn(quiz);

        String json = """
                {
                  "patientName": "Rania Mejri",
                  "numberOfQuestions": 3,
                  "difficultyLevel": 2
                }
                """;

        mockMvc.perform(post("/api/ai/quiz/generate-from-patient-name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topic").value("mémoire et cognition"));

        verify(quizAIService).generateQuiz(org.mockito.ArgumentMatchers.argThat(argument ->
                argument.getTopic().equals("mémoire et cognition")
                        && argument.getDifficultyLevel().equals(2)
                        && argument.getCustomContext().contains("Points faibles: Aucun")));
    }

    @Test
    void generateQuizFromPatientName_whenMedicalFolderMissing_shouldReturn500() throws Exception {
        PatientDTO patient = new PatientDTO();
        patient.setId(73L);
        patient.setFirstName("Meriem");
        patient.setLastName("Gharbi");

        when(patientLookupService.findPatientByName("Meriem Gharbi")).thenReturn(patient);
        when(medicalServiceClient.getMedicalFolderByPatient("73")).thenReturn(List.of());

        String json = """
                {
                  "patientName": "Meriem Gharbi",
                  "numberOfQuestions": 3
                }
                """;

        mockMvc.perform(post("/api/ai/quiz/generate-from-patient-name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Custom quiz generation failed"))
                .andExpect(jsonPath("$.message").value("No medical folder found for patient ID 73"));
    }
}
