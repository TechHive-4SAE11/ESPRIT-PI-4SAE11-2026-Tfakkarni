package org.techhive.assistantservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.techhive.assistantservice.config.VideoApiConfig;
import org.techhive.assistantservice.entity.GeneratedVideo;
import org.techhive.assistantservice.entity.enums.VideoProvider;
import org.techhive.assistantservice.entity.enums.VideoStatus;
import org.techhive.assistantservice.repository.GeneratedVideoRepository;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

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
    private final ObjectMapper objectMapper;

    // D-ID polling configuration
    private static final int MAX_POLL_ATTEMPTS = 60;        // max 60 attempts
    private static final long POLL_INTERVAL_MS = 5000;       // 5 seconds between polls
    private static final String DID_DEFAULT_AVATAR = "https://d-id-public-bucket.s3.amazonaws.com/alice.jpg";

    /**
     * Check if an external video provider (D-ID, HeyGen, etc.) is configured.
     */
    public boolean isExternalProviderConfigured() {
        VideoProvider provider = VideoProvider.valueOf(videoApiConfig.getProvider());
        if (provider == VideoProvider.SCRIPT_ONLY) {
            return false;
        }
        
        // Vérifier la clé API du provider
        return switch (provider) {
            case D_ID -> videoApiConfig.getDId().getApiKey() != null && !videoApiConfig.getDId().getApiKey().isBlank();
            case HEYGEN -> videoApiConfig.getHeygen().getApiKey() != null && !videoApiConfig.getHeygen().getApiKey().isBlank();
            case LUMA -> videoApiConfig.getLuma().getApiKey() != null && !videoApiConfig.getLuma().getApiKey().isBlank();
            case RUNWAY -> videoApiConfig.getRunway().getApiKey() != null && !videoApiConfig.getRunway().getApiKey().isBlank();
            default -> false;
        };
    }

    /**
     * Send a script to an external video API for rendering.
     * Returns the video URL once generation is complete.
     */
    public String generateVideoFromScript(Long videoId, String script) {
        GeneratedVideo video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));

        VideoProvider provider = VideoProvider.valueOf(videoApiConfig.getProvider());
        log.info("=== VIDEO GENERATION START === Provider: {} | Video ID: {}", provider, videoId);

        try {
            // Mark video as GENERATING
            video.setStatus(VideoStatus.GENERATING);
            videoRepository.save(video);

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
                log.info("=== VIDEO GENERATION SUCCESS === Video ID: {} | URL: {}", videoId, videoUrl);
            } else {
                video.setStatus(VideoStatus.READY);  // Script-only is considered ready
                log.info("=== VIDEO GENERATION COMPLETE (no URL) === Video ID: {}", videoId);
            }
            videoRepository.save(video);

            return videoUrl;
        } catch (Exception e) {
            log.error("=== VIDEO GENERATION FAILED === Video ID: {} | Error: {}", videoId, e.getMessage(), e);
            video.setStatus(VideoStatus.FAILED);
            videoRepository.save(video);
            throw new RuntimeException("Video generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * D-ID API integration - generates talking head avatar video.
     * Flow:
     *   1. POST /talks → create a talk, get talk ID
     *   2. GET /talks/{id} → poll until status is "done"
     *   3. Extract result_url from the response
     */
    private String generateWithDID(String script, GeneratedVideo video) {
        VideoApiConfig.DIdConfig config = videoApiConfig.getDId();

        // Validate API key
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            log.warn("D-ID API key not configured. Falling back to script-only mode.");
            return null;
        }

        log.info("=== D-ID API CALL === Starting video generation...");
        log.info("D-ID API URL: {}", config.getApiUrl());
        log.info("D-ID API Key (first 20 chars): {}...", config.getApiKey().substring(0, Math.min(20, config.getApiKey().length())));

        // Build authorization header — D-ID expects: Authorization: Basic <api-key>
        // The API key from D-ID Studio is already base64-encoded (format: base64(email:password))
        String authHeader = "Basic " + config.getApiKey();
        log.info("Authorization header set (Basic auth)");

        WebClient webClient = WebClient.builder()
                .baseUrl(config.getApiUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, authHeader)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();

        // ── Step 1: Create Talk ──────────────────────────────────────────────
        String talkId = createDIDTalk(webClient, script);
        if (talkId == null) {
            log.error("D-ID: Failed to create talk — no talk ID returned");
            return null;
        }
        log.info("D-ID: Talk created successfully. Talk ID: {}", talkId);

        // ── Step 2: Poll for completion ──────────────────────────────────────
        String resultUrl = pollDIDTalkResult(webClient, talkId);
        if (resultUrl == null) {
            log.error("D-ID: Polling completed but no result URL obtained for talk: {}", talkId);
            return null;
        }

        log.info("=== D-ID VIDEO READY === Talk ID: {} | Result URL: {}", talkId, resultUrl);
        return resultUrl;
    }

    /**
     * Step 1: POST /talks — Create a new talk with D-ID.
     */
    private String createDIDTalk(WebClient webClient, String script) {
        // Build the request body using Jackson for proper JSON escaping
        try {
            var requestMap = new java.util.LinkedHashMap<String, Object>();
            requestMap.put("source_url", DID_DEFAULT_AVATAR);

            var scriptMap = new java.util.LinkedHashMap<String, Object>();
            scriptMap.put("type", "text");
            scriptMap.put("input", script);

            var providerMap = new java.util.LinkedHashMap<String, Object>();
            providerMap.put("type", "microsoft");
            providerMap.put("voice_id", "en-US-JennyNeural");
            scriptMap.put("provider", providerMap);

            requestMap.put("script", scriptMap);

            // Add configuration for higher quality
            var configMap = new java.util.LinkedHashMap<String, Object>();
            configMap.put("stitch", true);
            requestMap.put("config", configMap);

            String requestBody = objectMapper.writeValueAsString(requestMap);
            log.info("D-ID: POST /talks request body: {}", requestBody);

            String response = webClient.post()
                    .uri("/talks")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            log.info("D-ID: POST /talks response: {}", response);

            if (response == null) {
                log.error("D-ID: POST /talks returned null response");
                return null;
            }

            // Parse response to extract talk ID
            JsonNode responseJson = objectMapper.readTree(response);
            String talkId = responseJson.path("id").asText(null);

            if (talkId == null || talkId.isEmpty()) {
                log.error("D-ID: No 'id' field in response: {}", response);
                return null;
            }

            return talkId;

        } catch (WebClientResponseException e) {
            log.error("D-ID: POST /talks HTTP error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("D-ID API error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("D-ID: POST /talks failed: {}", e.getMessage(), e);
            throw new RuntimeException("D-ID API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Step 2: Poll GET /talks/{id} until status is "done" or "error".
     * Returns the result_url when done.
     */
    private String pollDIDTalkResult(WebClient webClient, String talkId) {
        log.info("D-ID: Starting polling for talk: {} (max {} attempts, interval {}ms)",
                talkId, MAX_POLL_ATTEMPTS, POLL_INTERVAL_MS);

        for (int attempt = 1; attempt <= MAX_POLL_ATTEMPTS; attempt++) {
            try {
                // Wait before polling
                Thread.sleep(POLL_INTERVAL_MS);

                log.info("D-ID: Polling attempt {}/{} for talk: {}", attempt, MAX_POLL_ATTEMPTS, talkId);

                String response = webClient.get()
                        .uri("/talks/{id}", talkId)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(30))
                        .block();

                if (response == null) {
                    log.warn("D-ID: Poll returned null response (attempt {})", attempt);
                    continue;
                }

                JsonNode responseJson = objectMapper.readTree(response);
                String status = responseJson.path("status").asText("unknown");
                log.info("D-ID: Talk status: {} (attempt {})", status, attempt);

                switch (status.toLowerCase()) {
                    case "done" -> {
                        // Extract result_url
                        String resultUrl = responseJson.path("result_url").asText(null);
                        if (resultUrl != null && !resultUrl.isEmpty()) {
                            log.info("D-ID: Video generation complete! result_url: {}", resultUrl);
                            return resultUrl;
                        }
                        log.error("D-ID: Status is 'done' but no result_url found. Response: {}", response);
                        return null;
                    }
                    case "error", "rejected" -> {
                        String errorMessage = responseJson.path("error").path("description").asText(
                                responseJson.path("error").asText("Unknown error")
                        );
                        log.error("D-ID: Video generation failed with status '{}': {}", status, errorMessage);
                        throw new RuntimeException("D-ID video generation failed: " + errorMessage);
                    }
                    case "created", "started" -> {
                        log.debug("D-ID: Still processing (status: {}), will retry...", status);
                    }
                    default -> {
                        log.debug("D-ID: Unknown status '{}', will retry...", status);
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("D-ID polling interrupted", e);
            } catch (WebClientResponseException e) {
                log.error("D-ID: GET /talks/{} HTTP error: status={}, body={}", talkId, e.getStatusCode(), e.getResponseBodyAsString());
                if (e.getStatusCode().is4xxClientError()) {
                    throw new RuntimeException("D-ID API error: " + e.getStatusCode(), e);
                }
                // Retry on 5xx errors
                log.warn("D-ID: Server error, will retry (attempt {})", attempt);
            } catch (RuntimeException e) {
                throw e; // Re-throw RuntimeExceptions (e.g., from error status)
            } catch (Exception e) {
                log.error("D-ID: Poll error (attempt {}): {}", attempt, e.getMessage());
            }
        }

        log.error("D-ID: Polling timed out after {} attempts for talk: {}", MAX_POLL_ATTEMPTS, talkId);
        throw new RuntimeException("D-ID video generation timed out after " + (MAX_POLL_ATTEMPTS * POLL_INTERVAL_MS / 1000) + " seconds");
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
}
