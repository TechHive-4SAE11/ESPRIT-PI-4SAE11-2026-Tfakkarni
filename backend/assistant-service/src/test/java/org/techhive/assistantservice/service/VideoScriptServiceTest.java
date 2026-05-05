package org.techhive.assistantservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.techhive.assistantservice.client.GameServiceClient;
import org.techhive.assistantservice.client.MedicalServiceClient;
import org.techhive.assistantservice.dto.MedicalFolderDTO;
import org.techhive.assistantservice.dto.VideoGenerateRequest;
import org.techhive.assistantservice.dto.VideoGenerateResponse;
import org.techhive.assistantservice.entity.GeneratedVideo;
import org.techhive.assistantservice.repository.GeneratedVideoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoScriptServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;
    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private GameServiceClient gameServiceClient;
    @Mock
    private MedicalServiceClient medicalServiceClient;
    @Mock
    private GeneratedVideoRepository videoRepository;
    @Mock
    private VideoApiIntegrationService videoApiIntegrationService;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private VideoScriptService service;

    private void stubChatFailure() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("OpenAI unavailable"));
    }

    @Test
    void generateVideoScriptWhenAiFailsShouldUseStoryFallbackAndPersistScriptOnlyVideo() {
        stubChatFailure();
        when(gameServiceClient.getWeakTopicsByCaregiver(42L)).thenReturn(List.of("orientation", "attention"));
        MedicalFolderDTO folder = new MedicalFolderDTO();
        folder.setDiagnosis("Mild dementia");
        folder.setEvolution("Stable");
        folder.setTreatments("Cognitive therapy");
        folder.setRecommendations("Daily routine");
        when(medicalServiceClient.getMedicalFolderByPatient("42")).thenReturn(List.of(folder));
        when(videoRepository.save(any(GeneratedVideo.class))).thenAnswer(invocation -> {
            GeneratedVideo video = invocation.getArgument(0);
            video.setId(7L);
            video.setCreatedAt(LocalDateTime.of(2026, 5, 3, 13, 20));
            return video;
        });

        VideoGenerateRequest request = VideoGenerateRequest.builder()
                .patientId(42L)
                .topic("Family lunch")
                .memoryType("STORY")
                .duration(80)
                .patientName("Noura Ben Salem")
                .patientAge(74)
                .interests("cooking, jasmine tea")
                .build();

        VideoGenerateResponse response = service.generateVideoScript(request);

        assertEquals(7L, response.getVideoId());
        assertEquals(42L, response.getPatientId());
        assertEquals("Family lunch", response.getTopic());
        assertEquals("STORY", response.getMemoryType());
        assertEquals("SCRIPT_ONLY", response.getStatus());
        assertTrue(response.getScript().contains("Family lunch"));
        assertEquals(4, response.getStoryboard().size());
        assertEquals(20, response.getStoryboard().get(0).getDurationSeconds());
        verify(videoRepository).save(any(GeneratedVideo.class));
    }

    @Test
    void generateVideoScriptShouldUseAiTitleWhenTopicIsNullAndHandleClientFailures() {
        stubChatFailure();
        when(gameServiceClient.getWeakTopicsByCaregiver(9L)).thenThrow(new RuntimeException("game down"));
        when(medicalServiceClient.getMedicalFolderByPatient("9")).thenThrow(new RuntimeException("medical down"));
        when(videoRepository.save(any(GeneratedVideo.class))).thenAnswer(invocation -> {
            GeneratedVideo video = invocation.getArgument(0);
            video.setId(9L);
            return video;
        });

        VideoGenerateRequest request = VideoGenerateRequest.builder()
                .patientId(9L)
                .topic(null)
                .memoryType("EXERCISE")
                .duration(60)
                .build();

        VideoGenerateResponse response = service.generateVideoScript(request);

        assertEquals("Memory Exercise: Childhood memories", response.getTopic());
        assertEquals("EXERCISE", response.getMemoryType());
        assertEquals(4, response.getStoryboard().size());
    }

    @Test
    void getVideosByPatientAndGetVideoByIdShouldMapStoryboardJson() {
        String storyboardJson = """
                [{"sceneNumber":1,"description":"Garden","narration":"Welcome","durationSeconds":15,"visualPrompt":"sunny garden"}]
                """;
        GeneratedVideo video = GeneratedVideo.builder()
                .id(11L)
                .patientId(5L)
                .topic("Garden walk")
                .memoryType(org.techhive.assistantservice.entity.enums.MemoryType.PHOTO)
                .duration(30)
                .status(org.techhive.assistantservice.entity.enums.VideoStatus.READY)
                .videoUrl("https://cdn.example/video.mp4")
                .thumbnailUrl("https://cdn.example/thumb.jpg")
                .script("Look at the flowers")
                .storyboardJson(storyboardJson)
                .createdAt(LocalDateTime.of(2026, 5, 3, 13, 21))
                .build();
        when(videoRepository.findByPatientIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(video));
        when(videoRepository.findById(11L)).thenReturn(Optional.of(video));

        List<VideoGenerateResponse> byPatient = service.getVideosByPatient(5L);
        VideoGenerateResponse byId = service.getVideoById(11L);

        assertEquals(1, byPatient.size());
        assertEquals("https://cdn.example/thumb.jpg", byPatient.get(0).getThumbnailUrl());
        assertEquals(1, byPatient.get(0).getSceneCount());
        assertEquals("Garden", byId.getStoryboard().get(0).getDescription());
    }

    @Test
    void getVideoByIdShouldThrowWhenVideoIsMissing() {
        when(videoRepository.findById(404L)).thenReturn(Optional.empty());

        RuntimeException error = assertThrows(RuntimeException.class, () -> service.getVideoById(404L));

        assertTrue(error.getMessage().contains("Video not found with ID: 404"));
    }
}
