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
import org.techhive.gameservice.entity.Answer;
import org.techhive.gameservice.entity.Question;
import org.techhive.gameservice.service.IAnswerService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnswerController.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "google.translate.api-key=test",
        "elevenlabs.api-key=test",
        "elevenlabs.voice-id-en=test",
        "elevenlabs.voice-id-tn=test",
        "elevenlabs.model-id=test"
})
class AnswerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IAnswerService answerService;

    @Autowired
    private ObjectMapper objectMapper;

    private Answer sampleAnswer;
    private Question sampleQuestion;

    @BeforeEach
    void setUp() {
        sampleQuestion = new Question();
        sampleQuestion.setId(1L);
        sampleQuestion.setText("What is 2+2?");

        sampleAnswer = new Answer();
        sampleAnswer.setId(1L);
        sampleAnswer.setText("4");
        sampleAnswer.setIsCorrect(true);
        sampleAnswer.setExplanation("Basic math");
        sampleAnswer.setQuestion(sampleQuestion);
    }

    @Test
    void createAnswer_shouldReturn201() throws Exception {
        when(answerService.createAnswer(any())).thenReturn(sampleAnswer);

        String json = """
                {
                  "text": "4",
                  "isCorrect": true,
                  "explanation": "Basic math",
                  "questionId": 1
                }
                """;

        mockMvc.perform(post("/api/games/quiz/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("4"));
    }

    @Test
    void getAnswerById_whenExists_shouldReturn200() throws Exception {
        when(answerService.getAnswerById(1L)).thenReturn(sampleAnswer);

        mockMvc.perform(get("/api/games/quiz/answer/getAnswerById/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("4"));
    }

    @Test
    void getAnswerById_whenNotExists_shouldReturn404() throws Exception {
        when(answerService.getAnswerById(99L)).thenReturn(null);

        mockMvc.perform(get("/api/games/quiz/answer/getAnswerById/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllAnswers_shouldReturn200() throws Exception {
        when(answerService.getAllAnswers()).thenReturn(List.of(sampleAnswer));

        mockMvc.perform(get("/api/games/quiz/answer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("4"));
    }

    @Test
    void getAnswersByQuestionId_shouldReturn200() throws Exception {
        when(answerService.getAnswersByQuestionId(1L)).thenReturn(List.of(sampleAnswer));

        mockMvc.perform(get("/api/games/quiz/answer/question/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("4"));
    }

    @Test
    void getCorrectAnswer_shouldReturn200() throws Exception {
        when(answerService.getCorrectAnswerByQuestionId(1L)).thenReturn(sampleAnswer);

        mockMvc.perform(get("/api/games/quiz/answer/question/1/correct"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCorrect").value(true));
    }

    @Test
    void validateAnswer_shouldReturn200() throws Exception {
        when(answerService.validateAnswer(1L, 1L)).thenReturn(true);
        when(answerService.getAnswerById(1L)).thenReturn(sampleAnswer);

        String json = """
                {"questionId": 1, "answerId": 1}
                """;

        mockMvc.perform(post("/api/games/quiz/answer/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void createAnswersBatch_shouldReturn201() throws Exception {
        when(answerService.createAnswersBatch(anyList())).thenReturn(List.of(sampleAnswer));

        String json = """
                [
                  {"text": "4", "isCorrect": true, "explanation": "Correct", "questionId": 1},
                  {"text": "5", "isCorrect": false, "explanation": "Wrong", "questionId": 1}
                ]
                """;

        mockMvc.perform(post("/api/games/quiz/answer/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());
    }

    @Test
    void isAnswerCorrect_shouldReturn200() throws Exception {
        when(answerService.getAnswerById(1L)).thenReturn(sampleAnswer);

        mockMvc.perform(get("/api/games/quiz/answer/1/is-correct"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(true));
    }
}
