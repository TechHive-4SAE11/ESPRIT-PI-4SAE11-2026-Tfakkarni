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
import org.techhive.gameservice.entity.Quiz;
import org.techhive.gameservice.service.IQuizService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QuizController.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "google.translate.api-key=test",
        "elevenlabs.api-key=test",
        "elevenlabs.voice-id-en=test",
        "elevenlabs.voice-id-tn=test",
        "elevenlabs.model-id=test"
})
class QuizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IQuizService quizService;

    @Autowired
    private ObjectMapper objectMapper;

    private Quiz sampleQuiz;

    @BeforeEach
    void setUp() {
        sampleQuiz = new Quiz();
        sampleQuiz.setId(1L);
        sampleQuiz.setTopic("Memory Training");
        sampleQuiz.setTotalScore(80);
        sampleQuiz.setCaregiverId(10L);
        sampleQuiz.setDateTaken(LocalDateTime.now());
        sampleQuiz.setLevelReached(2);
    }

    @Test
    void createQuiz_shouldReturn201() throws Exception {
        when(quizService.createQuiz(any())).thenReturn(sampleQuiz);

        String json = """
                {
                  "topic": "Memory Training",
                  "totalScore": 0,
                  "caregiverId": 10,
                  "levelReached": 1
                }
                """;

        mockMvc.perform(post("/api/games/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.topic").value("Memory Training"));
    }

    @Test
    void createQuiz_withInvalidData_shouldReturn400() throws Exception {
        when(quizService.createQuiz(any())).thenReturn(null);

        String json = """
                {
                  "topic": "Memory Training",
                  "totalScore": 0,
                  "caregiverId": 10
                }
                """;

        mockMvc.perform(post("/api/games/quiz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getQuizById_whenExists_shouldReturn200() throws Exception {
        when(quizService.getQuizById(1L)).thenReturn(sampleQuiz);

        mockMvc.perform(get("/api/games/quiz/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.topic").value("Memory Training"));
    }

    @Test
    void getQuizById_whenNotExists_shouldReturn404() throws Exception {
        when(quizService.getQuizById(99L)).thenReturn(null);

        mockMvc.perform(get("/api/games/quiz/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllQuizzes_shouldReturn200() throws Exception {
        when(quizService.getAllQuizzes()).thenReturn(List.of(sampleQuiz));

        mockMvc.perform(get("/api/games/quiz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].topic").value("Memory Training"));
    }

    @Test
    void deleteQuiz_shouldReturn204() throws Exception {
        doNothing().when(quizService).deleteQuiz(1L);

        mockMvc.perform(delete("/api/games/quiz/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void searchQuizzesByTopic_shouldReturn200() throws Exception {
        when(quizService.searchQuizzesByTopic("Memory")).thenReturn(List.of(sampleQuiz));

        mockMvc.perform(get("/api/games/quiz/search").param("topic", "Memory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].topic").value("Memory Training"));
    }

    @Test
    void getQuizCountByCaregiver_shouldReturn200() throws Exception {
        when(quizService.getQuizCountByCaregiver(10L)).thenReturn(5L);

        mockMvc.perform(get("/api/games/quiz/caregiver/10/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    void startQuiz_whenExists_shouldReturn200() throws Exception {
        sampleQuiz.setTotalScore(0);
        when(quizService.startQuiz(1L)).thenReturn(sampleQuiz);

        mockMvc.perform(post("/api/games/quiz/1/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScore").value(0));
    }

    @Test
    void completeQuiz_shouldReturn200() throws Exception {
        sampleQuiz.setTotalScore(95);
        when(quizService.completeQuiz(1L, 95, 3)).thenReturn(sampleQuiz);

        String json = """
                {"score": 95, "levelReached": 3}
                """;

        mockMvc.perform(post("/api/games/quiz/1/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScore").value(95));
    }
}
