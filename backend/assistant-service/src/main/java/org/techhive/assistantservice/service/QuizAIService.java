package org.techhive.assistantservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.techhive.assistantservice.client.GameServiceClient;
import org.techhive.assistantservice.client.dto.AnswerDTO;
import org.techhive.assistantservice.client.dto.QuestionDTO;
import org.techhive.assistantservice.client.dto.QuizDTO;
import org.techhive.assistantservice.dto.QuizGenerateRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizAIService {

    private final ChatClient.Builder chatClientBuilder;
    private final GameServiceClient gameServiceClient;
    private final ObjectMapper objectMapper;

    /**
     * Generate a complete quiz with questions and answers using OpenAI,
     * then persist it via game-service Feign client.
     */
    public QuizDTO generateQuiz(QuizGenerateRequest request) {
        log.info("Generating AI quiz: topic={}, questions={}, difficulty={}",
                request.getTopic(), request.getNumberOfQuestions(), request.getDifficultyLevel());

        // 1. Generate questions + answers via OpenAI
        String aiResponse = callOpenAIForQuiz(request);
        log.debug("OpenAI response: {}", aiResponse);

        // 2. Parse the AI response
        List<Map<String, Object>> parsedQuestions = parseAIQuizResponse(aiResponse);

        // 3. Save the main Quiz entity using GameService
        QuizDTO initialQuiz = QuizDTO.builder()
                .topic(request.getTopic())
                .totalScore(0)
                .dateTaken(LocalDateTime.now())
                .caregiverId(request.getCaregiverId())
                .levelReached(request.getDifficultyLevel())
                .build();
        
        QuizDTO createdQuiz = gameServiceClient.createQuiz(initialQuiz);
        log.info("Quiz created in DB with ID: {}", createdQuiz.getId());

        // 4. Save Questions and Answers matching the AI response
        List<QuestionDTO> createdQuestions = new ArrayList<>();

        for (Map<String, Object> q : parsedQuestions) {
            QuestionDTO questionDTO = QuestionDTO.builder()
                    .text((String) q.get("question"))
                    .difficultyLevel(request.getDifficultyLevel())
                    .quizId(createdQuiz.getId())
                    .build();
            
            QuestionDTO createdQuestion = gameServiceClient.createQuestion(questionDTO);

            // Create answers for this question
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> answers = (List<Map<String, Object>>) q.get("answers");
            if (answers != null && !answers.isEmpty()) {
                List<AnswerDTO> answersToCreate = new ArrayList<>();
                for (Map<String, Object> a : answers) {
                    AnswerDTO answerDTO = AnswerDTO.builder()
                            .text((String) a.get("text"))
                            .isCorrect((Boolean) a.get("isCorrect"))
                            .explanation((String) a.get("explanation"))
                            .questionId(createdQuestion.getId())
                            .build();
                    answersToCreate.add(answerDTO);
                }
                List<AnswerDTO> savedAnswers = gameServiceClient.createAnswersBatch(answersToCreate);
                createdQuestion.setAnswers(savedAnswers);
            }
            createdQuestions.add(createdQuestion);
        }

        createdQuiz.setQuestions(createdQuestions);
        log.info("Quiz generation complete: {} questions saved to game-service database", createdQuestions.size());
        return createdQuiz;
    }

    private String callOpenAIForQuiz(QuizGenerateRequest request) {
        String difficultyLabel = switch (request.getDifficultyLevel()) {
            case 1 -> "easy (basic recall, simple recognition)";
            case 2 -> "medium (application, understanding context)";
            case 3 -> "hard (analysis, complex reasoning, tricky distractors)";
            default -> "medium";
        };

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(String.format("""
                You are an expert in creating cognitive assessment quizzes for Alzheimer's patients.
                Generate a quiz about "%s" with exactly %d questions at %s difficulty level.
                """, request.getTopic(), request.getNumberOfQuestions(), difficultyLabel));

        if (request.getCustomContext() != null && !request.getCustomContext().isBlank()) {
            promptBuilder.append("\nPATIENT CONTEXT (Crucial Data):\n")
                         .append(request.getCustomContext()).append("\n");
        }

        promptBuilder.append("""
                
                Context: This quiz is designed to assess memory and cognitive function in patients
                with potential Alzheimer's disease. Questions should be clear, unambiguous, and
                appropriate for the target difficulty. Use the PATIENT CONTEXT above (if provided) to personalize the formulation of questions.
                
                For each question, provide exactly 4 answer choices with:
                - Exactly ONE correct answer
                - Three plausible but incorrect distractors
                - A brief explanation for the correct answer
                
                IMPORTANT: Respond ONLY with a valid JSON array, no additional text.
                Format:
                [
                  {
                    "question": "Question text here?",
                    "answers": [
                      {"text": "Answer A", "isCorrect": true, "explanation": "Why this is correct"},
                      {"text": "Answer B", "isCorrect": false, "explanation": "Why this is incorrect"},
                      {"text": "Answer C", "isCorrect": false, "explanation": "Why this is incorrect"},
                      {"text": "Answer D", "isCorrect": false, "explanation": "Why this is incorrect"}
                    ]
                  }
                ]
                """);
        
        String prompt = promptBuilder.toString();

        try {
            ChatClient chatClient = chatClientBuilder.build();
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("OpenAI API call failed: {}", e.getMessage(), e);
            throw new RuntimeException("OpenAI API communication failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseAIQuizResponse(String aiResponse) {
        try {
            // Clean up the response - remove markdown code blocks if present
            String cleaned = aiResponse.trim();
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.substring(7);
            } else if (cleaned.startsWith("```")) {
                cleaned = cleaned.substring(3);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            cleaned = cleaned.trim();

            return objectMapper.readValue(cleaned, new TypeReference<List<Map<String, Object>>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse AI quiz response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse AI-generated quiz. Please try again.", e);
        }
    }
}
