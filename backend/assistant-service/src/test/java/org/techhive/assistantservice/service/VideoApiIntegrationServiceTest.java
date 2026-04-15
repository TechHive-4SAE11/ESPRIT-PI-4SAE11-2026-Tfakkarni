package org.techhive.assistantservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.assistantservice.config.VideoApiConfig;
import org.techhive.assistantservice.entity.GeneratedVideo;
import org.techhive.assistantservice.entity.enums.VideoProvider;
import org.techhive.assistantservice.entity.enums.VideoStatus;
import org.techhive.assistantservice.repository.GeneratedVideoRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoApiIntegrationServiceTest {

    @Mock
    private VideoApiConfig videoApiConfig;

    @Mock
    private GeneratedVideoRepository videoRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PexelsVideoService pexelsVideoService;

    @InjectMocks
    private VideoApiIntegrationService videoApiIntegrationService;

    private GeneratedVideo sampleVideo;

    @BeforeEach
    void setUp() {
        sampleVideo = new GeneratedVideo();
        sampleVideo.setId(1L);
        sampleVideo.setScript("A beautiful memory about family");
        sampleVideo.setStatus(VideoStatus.SCRIPT_ONLY);
    }

    @Test
    void isExternalProviderConfigured_withScriptOnly_shouldReturnFalse() {
        when(videoApiConfig.getProvider()).thenReturn("SCRIPT_ONLY");

        boolean configured = videoApiIntegrationService.isExternalProviderConfigured();

        assertFalse(configured);
    }

    @Test
    void isExternalProviderConfigured_withPexelsAndKey_shouldReturnTrue() {
        when(videoApiConfig.getProvider()).thenReturn("PEXELS");
        VideoApiConfig.PexelsConfig pexelsConfig = new VideoApiConfig.PexelsConfig();
        pexelsConfig.setApiKey("test-key");
        when(videoApiConfig.getPexels()).thenReturn(pexelsConfig);

        boolean configured = videoApiIntegrationService.isExternalProviderConfigured();

        assertTrue(configured);
    }

    @Test
    void isExternalProviderConfigured_withPexelsButNoKey_shouldReturnFalse() {
        when(videoApiConfig.getProvider()).thenReturn("PEXELS");
        VideoApiConfig.PexelsConfig pexelsConfig = new VideoApiConfig.PexelsConfig();
        pexelsConfig.setApiKey("");
        when(videoApiConfig.getPexels()).thenReturn(pexelsConfig);

        boolean configured = videoApiIntegrationService.isExternalProviderConfigured();

        assertFalse(configured);
    }

    @Test
    void generateVideoFromScript_withPexels_shouldReturnUrl() {
        when(videoRepository.findById(1L)).thenReturn(Optional.of(sampleVideo));
        when(videoApiConfig.getProvider()).thenReturn("PEXELS");
        when(pexelsVideoService.fetchVideo(any(GeneratedVideo.class))).thenReturn("https://pexels.com/video/123");
        when(videoRepository.save(any(GeneratedVideo.class))).thenReturn(sampleVideo);

        String videoUrl = videoApiIntegrationService.generateVideoFromScript(1L, "test script");

        assertEquals("https://pexels.com/video/123", videoUrl);
        assertEquals(VideoStatus.READY, sampleVideo.getStatus());
        verify(videoRepository, times(2)).save(sampleVideo); // GENERATING + READY
    }

    @Test
    void generateVideoFromScript_whenVideoNotFound_shouldThrowException() {
        when(videoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                videoApiIntegrationService.generateVideoFromScript(99L, "test"));
    }

    @Test
    void generateVideoFromScript_whenPexelsFails_shouldRevertToScriptOnly() {
        when(videoRepository.findById(1L)).thenReturn(Optional.of(sampleVideo));
        when(videoApiConfig.getProvider()).thenReturn("PEXELS");
        when(pexelsVideoService.fetchVideo(any())).thenThrow(new RuntimeException("Pexels API down"));
        when(videoRepository.save(any(GeneratedVideo.class))).thenReturn(sampleVideo);

        assertThrows(RuntimeException.class, () ->
                videoApiIntegrationService.generateVideoFromScript(1L, "test"));

        assertEquals(VideoStatus.SCRIPT_ONLY, sampleVideo.getStatus());
    }

    @Test
    void generateVideoFromScript_withScriptOnlyProvider_shouldReturnNull() {
        when(videoRepository.findById(1L)).thenReturn(Optional.of(sampleVideo));
        when(videoApiConfig.getProvider()).thenReturn("SCRIPT_ONLY");
        when(videoRepository.save(any(GeneratedVideo.class))).thenReturn(sampleVideo);

        String videoUrl = videoApiIntegrationService.generateVideoFromScript(1L, "test script");

        assertNull(videoUrl);
        assertEquals(VideoStatus.READY, sampleVideo.getStatus());
    }
}
