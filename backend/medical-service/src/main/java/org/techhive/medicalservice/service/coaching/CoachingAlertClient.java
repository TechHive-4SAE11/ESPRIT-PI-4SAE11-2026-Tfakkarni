package org.techhive.medicalservice.service.coaching;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class CoachingAlertClient {

    private final RestTemplate restTemplate;

    @Value("${alert-service.url:http://localhost:18084}")
    private String alertServiceUrl;

    /** Use plain RestTemplate — URL is absolute (localhost or gateway). */
    public CoachingAlertClient(@Qualifier("externalRestTemplate") RestTemplate externalRestTemplate) {
        this.restTemplate = externalRestTemplate;
    }

    public void sendCoachingPush(String patientId, String title, String body, Long goalId, String subType) {
        if (patientId == null || patientId.isBlank()) {
            return;
        }
        String base = alertServiceUrl.replaceAll("/$", "");
        String url = base + "/api/alerts/push/coaching";
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("patientId", patientId);
            payload.put("title", title);
            payload.put("body", body);
            payload.put("goalId", goalId);
            payload.put("notificationSubType", subType != null ? subType : "REMINDER");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(url, new HttpEntity<>(payload, headers), Map.class);
            log.debug("Coaching push requested for patient {} goal {}", patientId, goalId);
        } catch (Exception e) {
            log.warn("Could not reach alert-service for coaching push: {}", e.getMessage());
        }
    }
}
