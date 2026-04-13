package org.techhive.trackingservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.trackingservice.dto.*;
import org.techhive.trackingservice.entity.*;
import org.techhive.trackingservice.enums.CareActivityType;
import org.techhive.trackingservice.repository.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionnaireService {

    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionRepository questionRepository;
    private final PatientAnswerRepository patientAnswerRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Questionnaire> getAllQuestionnaires() {
        return questionnaireRepository.findAll();
    }

    @Transactional
    public void submitAnswers(QuestionnaireSubmissionDTO submission) {
        List<PatientAnswer> answers = submission.getAnswers().stream()
                .map(dto -> {
                    Question question = questionRepository.findById(dto.getQuestionId())
                            .orElseThrow(() -> new RuntimeException("Question not found: " + dto.getQuestionId()));
                    return PatientAnswer.builder()
                            .patientId(submission.getPatientId())
                            .question(question)
                            .answer(dto.getAnswer())
                            .build();
                })
                .collect(Collectors.toList());
        patientAnswerRepository.saveAll(answers);
    }

    public CarePlanResponseDTO recommendCarePlan(QuestionnaireSubmissionDTO submission) {
        String geminiResponse = geminiService.generateRecommendation(submission.getAnswers());
        
        if (geminiResponse != null) {
            try {
                // Parse Gemini API response structure
                JsonNode root = objectMapper.readTree(geminiResponse);
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    String jsonText = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
                    
                    // Cleanup JSON if needed (sometimes Gemini wraps in markdown blocks)
                    jsonText = jsonText.trim();
                    if (jsonText.startsWith("```json")) {
                        jsonText = jsonText.substring(7, jsonText.length() - 3).trim();
                    } else if (jsonText.startsWith("```")) {
                        jsonText = jsonText.substring(3, jsonText.length() - 3).trim();
                    }

                    List<CareActivityResponseDTO> activities = objectMapper.readValue(jsonText, new TypeReference<List<CareActivityResponseDTO>>() {});
                    return CarePlanResponseDTO.builder()
                            .activities(activities)
                            .build();
                }
            } catch (Exception e) {
                log.error("Failed to parse Gemini response, falling back to simple logic: {}", e.getMessage());
            }
        }

        // Simple fallback recommendation logic based on keywords in answers
        List<CareActivityRequestDTO> recommendedActivities = new ArrayList<>();

        for (PatientAnswerDTO answerDto : submission.getAnswers()) {
            Question question = questionRepository.findById(answerDto.getQuestionId()).orElse(null);
            if (question == null) continue;

            String answer = answerDto.getAnswer().toLowerCase();
            String questionText = question.getText().toLowerCase();

            if (questionText.contains("sleep") && answer.contains("less than 6")) {
                recommendedActivities.add(CareActivityRequestDTO.builder()
                        .activityType(CareActivityType.SLEEP)
                        .activityName("Improve Sleep Hygiene")
                        .description("Aim for at least 7-8 hours of sleep. Establish a regular bedtime.")
                        .frequency("Daily")
                        .duration("8 hours")
                        .build());
            }

            if (questionText.contains("exercise") || questionText.contains("physical")) {
                if (answer.contains("no") || answer.contains("little")) {
                    recommendedActivities.add(CareActivityRequestDTO.builder()
                            .activityType(CareActivityType.PHYSICAL_ACTIVITY)
                            .activityName("Daily Walking")
                            .description("Take a 30-minute walk in a safe, familiar environment.")
                            .frequency("Daily")
                            .duration("30 minutes")
                            .build());
                }
            }

            if (questionText.contains("social") || questionText.contains("friends")) {
                if (answer.contains("rarely") || answer.contains("no")) {
                    recommendedActivities.add(CareActivityRequestDTO.builder()
                            .activityType(CareActivityType.SOCIAL_ACTIVITY)
                            .activityName("Social Engagement")
                            .description("Attend a local community center or call a family member.")
                            .frequency("3 times a week")
                            .duration("1 hour")
                            .build());
                }
            }
            
            if (questionText.contains("eat") || questionText.contains("diet") || questionText.contains("nutrition")) {
                recommendedActivities.add(CareActivityRequestDTO.builder()
                        .activityType(CareActivityType.NUTRITION_PLAN)
                        .activityName("Balanced Diet")
                        .description("Ensure a diet rich in Omega-3, antioxidants, and vitamins.")
                        .frequency("Daily")
                        .duration("3 meals")
                        .build());
            }
        }

        // If no specific recommendations, provide a general one
        if (recommendedActivities.isEmpty()) {
            recommendedActivities.add(CareActivityRequestDTO.builder()
                    .activityType(CareActivityType.PHYSICAL_ACTIVITY)
                    .activityName("General Activity")
                    .description("Keep moving and stay engaged with daily tasks.")
                    .frequency("Daily")
                    .duration("Variable")
                    .build());
        }

        return CarePlanResponseDTO.builder()
                .activities(recommendedActivities.stream()
                        .map(req -> CareActivityResponseDTO.builder()
                                .activityType(req.getActivityType())
                                .activityName(req.getActivityName())
                                .description(req.getDescription())
                                .frequency(req.getFrequency())
                                .duration(req.getDuration())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}

