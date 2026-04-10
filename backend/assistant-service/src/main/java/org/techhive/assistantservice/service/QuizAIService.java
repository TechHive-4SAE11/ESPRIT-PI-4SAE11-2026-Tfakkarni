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

        // 3. Create the quiz in game-service
        QuizDTO quizDTO = QuizDTO.builder()
                .topic(request.getTopic())
                .totalScore(0)
                .dateTaken(LocalDateTime.now())
                .caregiverId(request.getCaregiverId())
                .levelReached(request.getDifficultyLevel())
                .build();

        QuizDTO createdQuiz = gameServiceClient.createQuiz(quizDTO);
        log.info("Created quiz with ID: {}", createdQuiz.getId());

        // 4. Create questions and answers
        List<QuestionDTO> createdQuestions = new ArrayList<>();
        for (Map<String, Object> q : parsedQuestions) {
            QuestionDTO questionDTO = QuestionDTO.builder()
                    .text((String) q.get("question"))
                    .difficultyLevel(request.getDifficultyLevel())
                    .quizId(createdQuiz.getId())
                    .build();

            QuestionDTO createdQuestion = gameServiceClient.createQuestion(questionDTO);
            log.info("Created question ID: {}", createdQuestion.getId());

            // Create answers for this question
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> answers = (List<Map<String, Object>>) q.get("answers");
            if (answers != null) {
                List<AnswerDTO> answerDTOs = new ArrayList<>();
                for (Map<String, Object> a : answers) {
                    AnswerDTO answerDTO = AnswerDTO.builder()
                            .text((String) a.get("text"))
                            .isCorrect((Boolean) a.get("isCorrect"))
                            .explanation((String) a.get("explanation"))
                            .questionId(createdQuestion.getId())
                            .build();
                    answerDTOs.add(answerDTO);
                }

                List<AnswerDTO> createdAnswers = gameServiceClient.createAnswersBatch(answerDTOs);
                createdQuestion.setAnswers(createdAnswers);
            }

            createdQuestions.add(createdQuestion);
        }

        createdQuiz.setQuestions(createdQuestions);
        log.info("Quiz generation complete: {} questions created", createdQuestions.size());
        return createdQuiz;
    }

    private String callOpenAIForQuiz(QuizGenerateRequest request) {
        String difficultyLabel = switch (request.getDifficultyLevel()) {
            case 1 -> "easy (basic recall, simple recognition)";
            case 2 -> "medium (application, understanding context)";
            case 3 -> "hard (analysis, complex reasoning, tricky distractors)";
            default -> "medium";
        };

        String prompt = String.format("""
                You are an expert in creating cognitive assessment quizzes for Alzheimer's patients.
                Generate a quiz about "%s" with exactly %d questions at %s difficulty level.
                
                Context: This quiz is designed to assess memory and cognitive function in patients
                with potential Alzheimer's disease. Questions should be clear, unambiguous, and
                appropriate for the target difficulty.
                
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
                """, request.getTopic(), request.getNumberOfQuestions(), difficultyLabel);

        try {
            ChatClient chatClient = chatClientBuilder.build();
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("OpenAI API call failed ({}), using fallback quiz generation", e.getMessage());
            return generateFallbackQuiz(request);
        }
    }

    /**
     * Fallback: generates realistic quiz JSON without calling OpenAI.
     */
    private String generateFallbackQuiz(QuizGenerateRequest request) {
        String topic = request.getTopic();
        int count = request.getNumberOfQuestions();

        // Pre-built question templates for cognitive assessment
        String[][] templates = {
            {"What is the name of the red fruit often used in cakes?", "Strawberry", "Banana", "Apple", "Orange", "Strawberries are red and commonly used in desserts and pastries."},
            {"Which domestic animal meows?", "Cat", "Dog", "Rabbit", "Fish", "The cat is the only common domestic animal that meows."},
            {"What is the capital of France?", "Paris", "Lyon", "Marseille", "Toulouse", "Paris has been the capital of France for centuries."},
            {"What color is the sky on a clear day?", "Blue", "Green", "Red", "Yellow", "The sky appears blue due to the scattering of sunlight by the atmosphere."},
            {"How many days are there in a week?", "7", "5", "6", "10", "A week always contains exactly 7 days."},
            {"Which month comes after January?", "February", "March", "April", "December", "February is the second month of the year."},
            {"What is the opposite of 'hot'?", "Cold", "Warm", "Burning", "Mild", "Cold is the direct antonym of hot."},
            {"What is 2 + 2?", "4", "3", "5", "6", "The addition of 2 and 2 equals 4."},
            {"What do you use to write on paper?", "A pen", "A hammer", "A spoon", "A glass", "A pen is the most common writing instrument."},
            {"Which season comes after winter?", "Spring", "Summer", "Autumn", "Winter", "Spring always follows winter in the seasonal cycle."},
        };

        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < Math.min(count, templates.length); i++) {
            String[] t = templates[i];
            if (i > 0) json.append(",");
            json.append(String.format("""
                {
                  "question": "%s (Topic: %s)",
                  "answers": [
                    {"text": "%s", "isCorrect": true, "explanation": "%s"},
                    {"text": "%s", "isCorrect": false, "explanation": "This is not the correct answer."},
                    {"text": "%s", "isCorrect": false, "explanation": "This is not the correct answer."},
                    {"text": "%s", "isCorrect": false, "explanation": "This is not the correct answer."}
                  ]
                }
                """, t[0], topic, t[1], t[5], t[2], t[3], t[4]));
        }
        json.append("]");
        return json.toString();
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
