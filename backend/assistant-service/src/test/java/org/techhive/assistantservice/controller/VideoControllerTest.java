package org.techhive.assistantservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.techhive.assistantservice.dto.VideoGenerateResponse;
import org.techhive.assistantservice.service.VideoApiIntegrationService;
import org.techhive.assistantservice.service.VideoFeedbackService;
import org.techhive.assistantservice.service.VideoScriptService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VideoController.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class VideoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VideoScriptService videoScriptService;

    @MockBean
    private VideoApiIntegrationService videoApiIntegrationService;

    @MockBean
    private VideoFeedbackService videoFeedbackService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void generateVideo_shouldReturn201() throws Exception {
        VideoGenerateResponse response = VideoGenerateResponse.builder()
                .videoId(1L)
                .patientId(10L)
                .topic("Childhood Memories")
                .memoryType("PHOTO")
                .duration(60)
                .status("SCRIPT_ONLY")
                .script("Welcome to this memory journey...")
                .storyboard(List.of())
                .createdAt(LocalDateTime.now())
                .build();

        when(videoScriptService.generateVideoScript(any())).thenReturn(response);

        String json = """
                {
                  "patientId": 10,
                  "topic": "Childhood Memories",
                  "memoryType": "PHOTO",
                  "duration": 60
                }
                """;

        mockMvc.perform(post("/api/ai/video/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.topic").value("Childhood Memories"))
                .andExpect(jsonPath("$.status").value("SCRIPT_ONLY"));
    }

    @Test
    void getVideosByPatient_shouldReturn200() throws Exception {
        VideoGenerateResponse response = VideoGenerateResponse.builder()
                .videoId(1L)
                .patientId(10L)
                .topic("Family")
                .status("READY")
                .build();

        when(videoScriptService.getVideosByPatient(10L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/ai/video/patient/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].topic").value("Family"));
    }

    @Test
    void watchVideo_shouldReturn200() throws Exception {
        VideoGenerateResponse response = VideoGenerateResponse.builder()
                .videoId(1L)
                .topic("Memories")
                .status("READY")
                .build();

        when(videoScriptService.getVideoById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/ai/video/1/watch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topic").value("Memories"));
    }

    @Test
    void watchVideo_whenNotFound_shouldReturn404() throws Exception {
        when(videoScriptService.getVideoById(99L)).thenThrow(new RuntimeException("Not found"));

        mockMvc.perform(get("/api/ai/video/99/watch"))
                .andExpect(status().isNotFound());
    }
}
