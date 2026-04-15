package org.techhive.assistantservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.techhive.assistantservice.config.VideoApiConfig;
import org.techhive.assistantservice.entity.GeneratedVideo;
import org.techhive.assistantservice.entity.enums.VideoProvider;
import org.techhive.assistantservice.entity.enums.VideoStatus;
import org.techhive.assistantservice.repository.GeneratedVideoRepository;

/**
 * Integration service for external video generation APIs.
 * Uses Pexels for stock video search.
 * Falls back to SCRIPT_ONLY mode when no API key is configured.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoApiIntegrationService {

    private final VideoApiConfig videoApiConfig;
    private final GeneratedVideoRepository videoRepository;
    private final ObjectMapper objectMapper;
    private final PexelsVideoService pexelsVideoService;

    /**
     * Check if an external video provider is configured.
     */
    public boolean isExternalProviderConfigured() {
        VideoProvider provider = VideoProvider.valueOf(videoApiConfig.getProvider());
        if (provider == VideoProvider.SCRIPT_ONLY) {
            return false;
        }

        return switch (provider) {
            case PEXELS -> videoApiConfig.getPexels() != null
                    && videoApiConfig.getPexels().getApiKey() != null
                    && !videoApiConfig.getPexels().getApiKey().isBlank();
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
                case PEXELS -> pexelsVideoService.fetchVideo(video);
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
            // Revert state in DB so it doesn't get stuck indefinitely on GENERATING
            video.setStatus(VideoStatus.SCRIPT_ONLY);
            videoRepository.save(video);
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
