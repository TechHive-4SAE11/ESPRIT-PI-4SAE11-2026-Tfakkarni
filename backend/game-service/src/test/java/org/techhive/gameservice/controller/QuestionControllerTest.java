package org.techhive.gameservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.techhive.gameservice.entity.Question;
import org.techhive.gameservice.entity.Quiz;
import org.techhive.gameservice.service.IQuestionService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QuestionController.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "google.translate.api-key=test",
        "elevenlabs.api-key=test",
        "elevenlabs.voice-id-en=test",
        "elevenlabs.voice-id-tn=test",
        "elevenlabs.model-id=test"
})
class QuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IQuestionService questionService;

    @Autowired
    private ObjectMapper objectMapper;

    private Question sampleQuestion;
    private Quiz sampleQuiz;

    @BeforeEach
    void setUp() {
        sampleQuiz = new Quiz();
        sampleQuiz.setId(1L);
        sampleQuiz.setTopic("Memory");

        sampleQuestion = new Question();
        sampleQuestion.setId(1L);
        sampleQuestion.setText("What is the capital of France?");
        sampleQuestion.setDifficultyLevel(1);
        sampleQuestion.setQuiz(sampleQuiz);
    }

    @Test
    void createQuestion_shouldReturn201() throws Exception {
        when(questionService.createQuestion(any())).thenReturn(sampleQuestion);

        String json = """
                {
                  "text": "What is the capital of France?",
                  "difficultyLevel": 1,
                  "quizId": 1
                }
                """;

        mockMvc.perform(post("/api/games/quiz/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("What is the capital of France?"));
    }

    @Test
    void getQuestionById_whenExists_shouldReturn200() throws Exception {
        when(questionService.getQuestionById(1L)).thenReturn(sampleQuestion);

        mockMvc.perform(get("/api/games/quiz/questions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("What is the capital of France?"));
    }

    @Test
    void getQuestionById_whenNotExists_shouldReturn404() throws Exception {
        when(questionService.getQuestionById(99L)).thenReturn(null);

        mockMvc.perform(get("/api/games/quiz/questions/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllQuestions_shouldReturn200() throws Exception {
        when(questionService.getAllQuestions()).thenReturn(List.of(sampleQuestion));

        mockMvc.perform(get("/api/games/quiz/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("What is the capital of France?"));
    }

    @Test
    void deleteQuestion_shouldReturn204() throws Exception {
        doNothing().when(questionService).deleteQuestion(1L);

        mockMvc.perform(delete("/api/games/quiz/questions/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getQuestionsByQuizId_shouldReturn200() throws Exception {
        when(questionService.getQuestionsByQuizId(1L)).thenReturn(List.of(sampleQuestion));

        mockMvc.perform(get("/api/games/quiz/questions/quiz/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quizId").value(1));
    }

    @Test
    void searchQuestions_shouldReturn200() throws Exception {
        when(questionService.searchQuestions("capital")).thenReturn(List.of(sampleQuestion));

        mockMvc.perform(get("/api/games/quiz/questions/search").param("keyword", "capital"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("What is the capital of France?"));
    }

    @Test
    void getQuestionCountByQuizId_shouldReturn200() throws Exception {
        when(questionService.getQuestionCountByQuizId(1L)).thenReturn(5L);

        mockMvc.perform(get("/api/games/quiz/questions/quiz/1/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    void calculateTotalPoints_shouldReturn200() throws Exception {
        when(questionService.calculateTotalPoints(1L)).thenReturn(40);

        mockMvc.perform(get("/api/games/quiz/questions/quiz/1/total-points"))
                .andExpect(status().isOk())
                .andExpect(content().string("40"));
    }
}
