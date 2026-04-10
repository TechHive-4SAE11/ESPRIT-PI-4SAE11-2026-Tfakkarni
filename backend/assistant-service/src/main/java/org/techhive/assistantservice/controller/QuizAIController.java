package org.techhive.assistantservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.assistantservice.client.dto.QuizDTO;
import org.techhive.assistantservice.dto.QuizGenerateRequest;
import org.techhive.assistantservice.service.QuizAIService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai/quiz")
@RequiredArgsConstructor
public class QuizAIController {

    private final QuizAIService quizAIService;

    /**
     * POST /api/ai/quiz/generate
     * Generate a complete quiz using AI (OpenAI GPT).
     * The quiz is saved to game-service via Feign.
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateQuiz(@Valid @RequestBody QuizGenerateRequest request) {
        log.info("AI Quiz generation request: topic={}, questions={}, difficulty={}, caregiver={}",
                request.getTopic(), request.getNumberOfQuestions(),
                request.getDifficultyLevel(), request.getCaregiverId());

        try {
            QuizDTO generatedQuiz = quizAIService.generateQuiz(request);
            return new ResponseEntity<>(generatedQuiz, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Quiz generation failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Quiz generation failed",
                            "message", e.getMessage()
                    ));
        }
    }
}
