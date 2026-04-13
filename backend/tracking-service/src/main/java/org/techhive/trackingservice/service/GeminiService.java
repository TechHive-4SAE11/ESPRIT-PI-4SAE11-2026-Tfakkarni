package org.techhive.trackingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.techhive.trackingservice.dto.PatientAnswerDTO;
import org.techhive.trackingservice.entity.Question;
import org.techhive.trackingservice.repository.QuestionRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    private final QuestionRepository questionRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    public String generateRecommendation(List<PatientAnswerDTO> answers) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Gemini API key is not configured. Falling back to simple logic.");
            return null;
        }

        String prompt = constructPrompt(answers);
        
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);
        content.put("parts", List.of(part));
        requestBody.put("contents", List.of(content));

        // Adding system instruction for JSON output
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("response_mime_type", "application/json");
        requestBody.put("generationConfig", generationConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            String url = GEMINI_API_URL + apiKey;
            return restTemplate.postForObject(url, entity, String.class);
        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage());
            return null;
        }
    }

    private String constructPrompt(List<PatientAnswerDTO> answers) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an Alzheimer care specialist. Based on the patient's answers to the following questionnaire, recommend a list of care activities. ");
        sb.append("The response MUST be a JSON array of objects with the following fields: ");
        sb.append("'activityType' (one of: PHYSICAL_ACTIVITY, NUTRITION_PLAN, SLEEP, SOCIAL_ACTIVITY), ");
        sb.append("'activityName' (short descriptive name), ");
        sb.append("'description' (detailed advice), ");
        sb.append("'frequency' (e.g., 'Daily', '3 times a week'), ");
        sb.append("'duration' (e.g., '30 minutes', '8 hours').\n\n");
        sb.append("Patient Answers:\n");

        for (PatientAnswerDTO dto : answers) {
            Question q = questionRepository.findById(dto.getQuestionId()).orElse(null);
            String questionText = (q != null) ? q.getText() : "Question ID: " + dto.getQuestionId();
            sb.append("- ").append(questionText).append(": ").append(dto.getAnswer()).append("\n");
        }

        sb.append("\nReturn ONLY the JSON array.");
        return sb.toString();
    }
}
