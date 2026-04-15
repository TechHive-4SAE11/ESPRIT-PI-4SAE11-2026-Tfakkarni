package org.techhive.assistantservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.techhive.assistantservice.client.GameServiceClient;
import org.techhive.assistantservice.client.dto.AnswerDTO;
import org.techhive.assistantservice.client.dto.QuestionDTO;
import org.techhive.assistantservice.client.dto.QuizDTO;
import org.techhive.assistantservice.dto.QuizGenerateRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizAIServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private GameServiceClient gameServiceClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private QuizAIService quizAIService;

    private QuizGenerateRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleRequest = QuizGenerateRequest.builder()
                .topic("Memory Training")
                .numberOfQuestions(2)
                .difficultyLevel(1)
                .caregiverId(10L)
                .build();
    }

    @Test
    void generateQuiz_shouldCallOpenAIAndSaveViaGameService() {
        // Mock OpenAI response
        String aiResponse = """
                [
                  {
                    "question": "What is the capital of France?",
                    "answers": [
                      {"text": "Paris", "isCorrect": true, "explanation": "Paris is the capital"},
                      {"text": "London", "isCorrect": false, "explanation": "London is UK capital"},
                      {"text": "Berlin", "isCorrect": false, "explanation": "Berlin is German capital"},
                      {"text": "Madrid", "isCorrect": false, "explanation": "Madrid is Spanish capital"}
                    ]
                  }
                ]
                """;

        // Mock ChatClient chain
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(aiResponse);

        // Mock GameService calls
        QuizDTO createdQuiz = QuizDTO.builder().id(1L).topic("Memory Training").build();
        when(gameServiceClient.createQuiz(any(QuizDTO.class))).thenReturn(createdQuiz);

        QuestionDTO createdQuestion = QuestionDTO.builder().id(1L).text("What is the capital of France?").build();
        when(gameServiceClient.createQuestion(any(QuestionDTO.class))).thenReturn(createdQuestion);

        List<AnswerDTO> savedAnswers = List.of(
                AnswerDTO.builder().id(1L).text("Paris").isCorrect(true).build()
        );
        when(gameServiceClient.createAnswersBatch(anyList())).thenReturn(savedAnswers);

        // Execute
        QuizDTO result = quizAIService.generateQuiz(sampleRequest);

        // Verify
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Memory Training", result.getTopic());
        verify(gameServiceClient).createQuiz(any(QuizDTO.class));
        verify(gameServiceClient).createQuestion(any(QuestionDTO.class));
        verify(gameServiceClient).createAnswersBatch(anyList());
    }

    @Test
    void generateQuiz_whenOpenAIFails_shouldThrowException() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("API error"));

        assertThrows(RuntimeException.class, () ->
                quizAIService.generateQuiz(sampleRequest));
    }

    @Test
    void generateQuiz_withCustomContext_shouldIncludeInPrompt() {
        QuizGenerateRequest requestWithContext = QuizGenerateRequest.builder()
                .topic("Cognitive")
                .numberOfQuestions(1)
                .difficultyLevel(2)
                .caregiverId(10L)
                .customContext("Patient has mild cognitive impairment")
                .build();

        String aiResponse = """
                [{"question": "Q1?", "answers": [
                  {"text": "A", "isCorrect": true, "explanation": "E"},
                  {"text": "B", "isCorrect": false, "explanation": "E"},
                  {"text": "C", "isCorrect": false, "explanation": "E"},
                  {"text": "D", "isCorrect": false, "explanation": "E"}
                ]}]
                """;

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(aiResponse);

        QuizDTO createdQuiz = QuizDTO.builder().id(2L).topic("Cognitive").build();
        when(gameServiceClient.createQuiz(any())).thenReturn(createdQuiz);
        QuestionDTO createdQ = QuestionDTO.builder().id(1L).text("Q1?").build();
        when(gameServiceClient.createQuestion(any())).thenReturn(createdQ);
        when(gameServiceClient.createAnswersBatch(anyList())).thenReturn(List.of());

        QuizDTO result = quizAIService.generateQuiz(requestWithContext);

        assertNotNull(result);
        verify(requestSpec).user(contains("PATIENT CONTEXT"));
    }
}
