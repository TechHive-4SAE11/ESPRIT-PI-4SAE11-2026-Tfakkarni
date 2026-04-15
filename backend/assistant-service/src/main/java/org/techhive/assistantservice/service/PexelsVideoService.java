package org.techhive.assistantservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.techhive.assistantservice.config.VideoApiConfig;
import org.techhive.assistantservice.entity.GeneratedVideo;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PexelsVideoService {

    private final VideoApiConfig videoApiConfig;
    private final ObjectMapper objectMapper;

    /**
     * Searches for a stock MP4 video on Pexels using the first visual prompt from the storyboard.
     * Returns the direct video URL.
     */
    public String fetchVideo(GeneratedVideo video) {
        VideoApiConfig.PexelsConfig config = videoApiConfig.getPexels();
        if (config == null || config.getApiKey() == null || config.getApiKey().isBlank()) {
            log.warn("Pexels API key not configured.");
            return null;
        }

        // Extract a strong keyword from the storyboard's visualPrompt
        String prompt = "nature"; // fallback
        try {
            if (video.getStoryboardJson() != null && !video.getStoryboardJson().isBlank()) {
                List<Map<String, Object>> scenes = objectMapper.readValue(video.getStoryboardJson(), new TypeReference<>() {});
                if (!scenes.isEmpty() && scenes.get(0).containsKey("visualPrompt")) {
                    prompt = String.valueOf(scenes.get(0).get("visualPrompt"));
                }
            } else if (video.getTopic() != null) {
                prompt = video.getTopic();
            }
        } catch (Exception e) {
            log.warn("Could not parse storyboard JSON for Pexels, falling back to basic prompt.");
        }

        // Nettoyer le prompt pour la recherche (limiter à 3-4 mots forts sans ponctuation)
        String cleanedPrompt = prompt.replaceAll("[^a-zA-Z0-9 ]", "").trim();
        if (cleanedPrompt.split(" ").length > 4) {
             String[] words = cleanedPrompt.split(" ");
             cleanedPrompt = words[0] + " " + words[1] + " " + words[2];
        }
        
        final String finalQuery = cleanedPrompt;

        log.info("=== PEXELS API CALL === Searching stock video for keywords: '{}'", finalQuery);

        WebClient webClient = WebClient.builder()
                .baseUrl(config.getApiUrl())
                // Pexels just uses the raw API key in the Authorization header (No "Bearer ")
                .defaultHeader(HttpHeaders.AUTHORIZATION, config.getApiKey())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        try {
            // Search API Pexels
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/videos/search")
                            .queryParam("query", finalQuery)
                            .queryParam("per_page", 1)
                            .queryParam("orientation", "landscape")
                            .build())
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(15))
                    .block();

            if (response != null && response.containsKey("videos")) {
                List<Map<String, Object>> videos = (List<Map<String, Object>>) response.get("videos");
                if (videos != null && !videos.isEmpty()) {
                    List<Map<String, Object>> videoFiles = (List<Map<String, Object>>) videos.get(0).get("video_files");
                    if (videoFiles != null && !videoFiles.isEmpty()) {
                        // Return the link of the first video file (usually HD MP4)
                        String videoUrl = String.valueOf(videoFiles.get(0).get("link"));
                        log.info("Pexels successfully found a video! URL: {}", videoUrl);
                        return videoUrl;
                    }
                }
            }
            
            log.warn("Pexels API returned no videos for the query. Returning generic nature video.");
            // Fallback générique si aucun résultat exact n'est trouvé
            return "https://videos.pexels.com/video-files/853889/853889-hd_1920_1080_25fps.mp4";
            
        } catch (Exception e) {
            log.error("Pexels API call failed: {}", e.getMessage(), e);
            throw new RuntimeException("Pexels Video Search failed: " + e.getMessage(), e);
        }
    }
}
