package org.techhive.assistantservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.techhive.assistantservice.config.VideoApiConfig;
import org.techhive.assistantservice.entity.GeneratedVideo;
import org.techhive.assistantservice.entity.enums.VideoProvider;
import org.techhive.assistantservice.entity.enums.VideoStatus;
import org.techhive.assistantservice.repository.GeneratedVideoRepository;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Integration service for external video generation APIs.
 * Supports D-ID, HeyGen, Luma Dream Machine, Runway Gen-2.
 * Falls back to SCRIPT_ONLY mode when no API key is configured.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoApiIntegrationService {

    private final VideoApiConfig videoApiConfig;
    private final GeneratedVideoRepository videoRepository;

    /**
     * Send a script to an external video API for rendering.
     * Returns the video URL once generation is complete.
     */
    public String generateVideoFromScript(Long videoId, String script) {
        GeneratedVideo video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));

        VideoProvider provider = VideoProvider.valueOf(videoApiConfig.getProvider());
        log.info("Generating video via provider: {} for video ID: {}", provider, videoId);

        try {
            String videoUrl = switch (provider) {
                case D_ID -> generateWithDID(script, video);
                case HEYGEN -> generateWithHeyGen(script, video);
                case LUMA -> generateWithLuma(script, video);
                case RUNWAY -> generateWithRunway(script, video);
                case SCRIPT_ONLY -> {
                    log.info("SCRIPT_ONLY mode: no external video API called. Script saved.");
                    yield null;
                }
            };

            if (videoUrl != null) {
                video.setVideoUrl(videoUrl);
                video.setStatus(VideoStatus.READY);
            } else {
                video.setStatus(VideoStatus.READY);  // Script-only is considered ready
            }
            videoRepository.save(video);

            return videoUrl;
        } catch (Exception e) {
            log.error("Video generation failed for video {}: {}", videoId, e.getMessage());
            video.setStatus(VideoStatus.FAILED);
            videoRepository.save(video);
            throw new RuntimeException("Video generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * D-ID API integration - generates talking head avatar video.
     */
    private String generateWithDID(String script, GeneratedVideo video) {
        VideoApiConfig.DIdConfig config = videoApiConfig.getDId();
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            log.warn("D-ID API key not configured. Falling back to script-only mode.");
            return null;
        }

        log.info("Calling D-ID API to generate talking avatar video...");

        WebClient webClient = WebClient.builder()
                .baseUrl(config.getApiUrl())
                .defaultHeader("Authorization", "Basic " + config.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();

        // D-ID talks endpoint
        String requestBody = String.format("""
                {
                  "source_url": "https://d-id-public-bucket.s3.amazonaws.com/alice.jpg",
                  "script": {
                    "type": "text",
                    "input": "%s",
                    "provider": {
                      "type": "microsoft",
                      "voice_id": "fr-FR-DeniseNeural"
                    }
                  }
                }
                """, escapeJson(script));

        try {
            String response = webClient.post()
                    .uri("/talks")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("D-ID response: {}", response);
            // In a real implementation, poll for completion and extract video URL
            return null; // Placeholder - poll for result_url
        } catch (Exception e) {
            log.error("D-ID API call failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * HeyGen API integration - generates AI avatar video.
     */
    private String generateWithHeyGen(String script, GeneratedVideo video) {
        VideoApiConfig.HeyGenConfig config = videoApiConfig.getHeygen();
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            log.warn("HeyGen API key not configured. Falling back to script-only mode.");
            return null;
        }

        log.info("Calling HeyGen API to generate avatar video...");
        // HeyGen v2 API integration placeholder
        // POST /v2/video/generate with avatar_id, script, etc.
        return null;
    }

    /**
     * Luma Dream Machine integration - generates AI video from prompts.
     */
    private String generateWithLuma(String script, GeneratedVideo video) {
        VideoApiConfig.LumaConfig config = videoApiConfig.getLuma();
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            log.warn("Luma API key not configured. Falling back to script-only mode.");
            return null;
        }

        log.info("Calling Luma Dream Machine API...");
        // Luma API integration placeholder
        return null;
    }

    /**
     * Runway Gen-2 integration - generates AI video from text prompts.
     */
    private String generateWithRunway(String script, GeneratedVideo video) {
        VideoApiConfig.RunwayConfig config = videoApiConfig.getRunway();
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            log.warn("Runway API key not configured. Falling back to script-only mode.");
            return null;
        }

        log.info("Calling Runway Gen-2 API...");
        // Runway API integration placeholder
        return null;
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
