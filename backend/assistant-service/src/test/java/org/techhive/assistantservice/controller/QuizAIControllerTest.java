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
}
