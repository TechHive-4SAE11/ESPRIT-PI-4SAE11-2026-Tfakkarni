package org.techhive.trackingservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
public class DailyRoomService {

    private final String apiKey;
    private final String apiUrl;
    private final int expiryMinutes;
    private final RestTemplate restTemplate;

    public DailyRoomService(
            @Value("${daily.api-key}") String apiKey,
            @Value("${daily.api-url}") String apiUrl,
            @Value("${meeting.room-expiry-minutes}") int expiryMinutes,
            @Qualifier("plainRestTemplate") RestTemplate restTemplate) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.expiryMinutes = expiryMinutes;
        this.restTemplate = restTemplate;
    }

    /**
     * Create a Daily.co room with the given name.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> createRoom(String roomName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            long expiry = System.currentTimeMillis() / 1000 + (long) expiryMinutes * 60;

            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("exp", expiry);
            properties.put("enable_screenshare", true);
            properties.put("enable_chat", true);
            properties.put("enable_prejoin_ui", false);
            properties.put("start_video_off", false);
            properties.put("start_audio_off", false);
            properties.put("lang", "fr");

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("name", roomName);
            body.put("privacy", "private");
            body.put("properties", properties);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl + "/rooms",
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            log.info("Daily.co room created: {}", roomName);
            return response.getBody();

        } catch (Exception e) {
            log.error("Failed to create Daily.co room '{}': {}", roomName, e.getMessage());
            throw new RuntimeException("Erreur lors de la création de la salle Daily.co: " + e.getMessage(), e);
        }
    }

    /**
     * Create a meeting token for a participant.
     */
    @SuppressWarnings("unchecked")
    public String createMeetingToken(String roomName, String userId, String userName, boolean isOwner) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            long expiry = System.currentTimeMillis() / 1000 + (long) expiryMinutes * 60;

            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("room_name", roomName);
            properties.put("user_id", userId);
            properties.put("user_name", userName);
            properties.put("is_owner", isOwner);
            properties.put("exp", expiry);
            properties.put("enable_screenshare", true);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("properties", properties);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl + "/meeting-tokens",
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            String token = response.getBody().get("token").toString();
            log.info("Meeting token created for user '{}' in room '{}'", userName, roomName);
            return token;

        } catch (Exception e) {
            log.error("Failed to create meeting token for room '{}': {}", roomName, e.getMessage());
            throw new RuntimeException("Erreur lors de la création du token: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a Daily.co room (non-blocking cleanup).
     */
    public void deleteRoom(String roomName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            restTemplate.exchange(
                    apiUrl + "/rooms/" + roomName,
                    HttpMethod.DELETE,
                    request,
                    Void.class
            );

            log.info("Daily.co room deleted: {}", roomName);

        } catch (Exception e) {
            log.warn("Failed to delete Daily.co room '{}': {}", roomName, e.getMessage());
        }
    }
}
