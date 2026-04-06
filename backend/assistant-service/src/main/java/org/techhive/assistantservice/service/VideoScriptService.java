package org.techhive.assistantservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.techhive.assistantservice.client.GameServiceClient;
import org.techhive.assistantservice.dto.VideoGenerateRequest;
import org.techhive.assistantservice.dto.VideoGenerateResponse;
import org.techhive.assistantservice.dto.VideoGenerateResponse.StoryboardScene;
import org.techhive.assistantservice.entity.GeneratedVideo;
import org.techhive.assistantservice.entity.enums.MemoryType;
import org.techhive.assistantservice.entity.enums.VideoStatus;
import org.techhive.assistantservice.repository.GeneratedVideoRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoScriptService {

    private final ChatClient.Builder chatClientBuilder;
    private final GameServiceClient gameServiceClient;
    private final GeneratedVideoRepository videoRepository;
    private final ObjectMapper objectMapper;

    /**
     * Generate a personalized video script and storyboard for memory stimulation.
     */
    public VideoGenerateResponse generateVideoScript(VideoGenerateRequest request) {
        log.info("Generating video script: patient={}, topic={}, type={}, duration={}s",
                request.getPatientId(), request.getTopic(), request.getMemoryType(), request.getDuration());

        // 1. Gather patient context (weak topics from quiz history)
        List<String> weakTopics = fetchWeakTopics(request.getPatientId());

        // 2. Generate script + storyboard via OpenAI
        String aiResponse = callOpenAIForVideoScript(request, weakTopics);
        log.debug("OpenAI video script response: {}", aiResponse);

        // 3. Parse the response
        Map<String, Object> parsed = parseVideoScriptResponse(aiResponse);

        // 4. Build storyboard scenes
        List<StoryboardScene> storyboard = buildStoryboard(parsed);

        // 5. Persist the video record
        GeneratedVideo video = GeneratedVideo.builder()
                .patientId(request.getPatientId())
                .topic(request.getTopic())
                .memoryType(MemoryType.valueOf(request.getMemoryType()))
                .duration(request.getDuration())
                .status(VideoStatus.READY)
                .script((String) parsed.getOrDefault("script", ""))
                .storyboardJson(toJson(storyboard))
                .patientName(request.getPatientName())
                .patientAge(request.getPatientAge())
                .interests(request.getInterests())
                .build();

        GeneratedVideo saved = videoRepository.save(video);
        log.info("Saved generated video with ID: {}", saved.getId());

        return VideoGenerateResponse.builder()
                .videoId(saved.getId())
                .patientId(saved.getPatientId())
                .topic(saved.getTopic())
                .memoryType(saved.getMemoryType().name())
                .duration(saved.getDuration())
                .status(saved.getStatus().name())
                .script(saved.getScript())
                .storyboard(storyboard)
                .createdAt(saved.getCreatedAt())
                .build();
    }

    /**
     * Get all videos for a patient.
     */
    public List<VideoGenerateResponse> getVideosByPatient(Long patientId) {
        List<GeneratedVideo> videos = videoRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        return videos.stream().map(this::toResponse).toList();
    }

    /**
     * Get a specific video.
     */
    public VideoGenerateResponse getVideoById(Long videoId) {
        GeneratedVideo video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found with ID: " + videoId));
        return toResponse(video);
    }

    private List<String> fetchWeakTopics(Long patientId) {
        try {
            return gameServiceClient.getWeakTopicsByCaregiver(patientId);
        } catch (Exception e) {
            log.warn("Could not fetch weak topics for patient {}: {}", patientId, e.getMessage());
            return new ArrayList<>();
        }
    }

    private String callOpenAIForVideoScript(VideoGenerateRequest request, List<String> weakTopics) {
        String memoryTypeDescription = switch (request.getMemoryType()) {
            case "PHOTO" -> """
                    A PHOTO-type video: Create a narrated slideshow script.
                    Describe scenes showing familiar objects, places, or activities.
                    Each scene should trigger positive memories and recognition.
                    Include warm, reassuring narration that guides the viewer.""";
            case "STORY" -> """
                    A STORY-type video: Create a personalized short story script.
                    The story should involve familiar themes (childhood, family events, daily routines).
                    Use simple, clear language with emotional connections.
                    Include pauses for the viewer to recall related memories.""";
            case "EXERCISE" -> """
                    An EXERCISE-type video: Create guided memory exercises.
                    Include association games, recall challenges, and pattern recognition.
                    Start simple and gradually increase complexity.
                    Provide encouragement and hints when needed.""";
            default -> "A general memory stimulation video.";
        };

        String patientContext = "";
        if (request.getPatientName() != null) patientContext += "Patient name: " + request.getPatientName() + "\n";
        if (request.getPatientAge() != null) patientContext += "Patient age: " + request.getPatientAge() + "\n";
        if (request.getInterests() != null) patientContext += "Interests: " + request.getInterests() + "\n";
        if (!weakTopics.isEmpty()) patientContext += "Weak cognitive areas: " + String.join(", ", weakTopics) + "\n";

        String prompt = String.format("""
                You are an expert in creating therapeutic video content for Alzheimer's and dementia patients.
                
                Create a video script about "%s" that is %d seconds long.
                
                Video Type:
                %s
                
                Patient Context:
                %s
                
                Requirements:
                - The content must be warm, reassuring, and cognitively stimulating
                - Use simple, clear language
                - Include sensory descriptions (colors, sounds, textures)
                - Pace the content appropriately for the target duration
                - If the patient has weak cognitive areas, subtly incorporate exercises for those areas
                
                IMPORTANT: Respond ONLY with valid JSON, no additional text.
                Format:
                {
                  "script": "Full narration script for the video",
                  "title": "Video title",
                  "storyboard": [
                    {
                      "sceneNumber": 1,
                      "description": "Visual description of the scene",
                      "narration": "What the narrator says",
                      "durationSeconds": 15,
                      "visualPrompt": "AI image generation prompt for this scene"
                    }
                  ],
                  "emotionalTone": "warm and encouraging",
                  "cognitiveGoals": ["goal1", "goal2"]
                }
                """, request.getTopic(), request.getDuration(), memoryTypeDescription,
                patientContext.isEmpty() ? "No specific patient context available." : patientContext);

        ChatClient chatClient = chatClientBuilder.build();
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseVideoScriptResponse(String aiResponse) {
        try {
            String cleaned = cleanJson(aiResponse);
            return objectMapper.readValue(cleaned, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse video script response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse AI video script. Please try again.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<StoryboardScene> buildStoryboard(Map<String, Object> parsed) {
        List<StoryboardScene> scenes = new ArrayList<>();
        List<Map<String, Object>> storyboardData = (List<Map<String, Object>>) parsed.get("storyboard");
        if (storyboardData != null) {
            for (Map<String, Object> scene : storyboardData) {
                scenes.add(StoryboardScene.builder()
                        .sceneNumber(toInt(scene.get("sceneNumber")))
                        .description((String) scene.get("description"))
                        .narration((String) scene.get("narration"))
                        .durationSeconds(toInt(scene.get("durationSeconds")))
                        .visualPrompt((String) scene.get("visualPrompt"))
                        .build());
            }
        }
        return scenes;
    }

    private VideoGenerateResponse toResponse(GeneratedVideo video) {
        List<StoryboardScene> storyboard = new ArrayList<>();
        if (video.getStoryboardJson() != null) {
            try {
                storyboard = objectMapper.readValue(video.getStoryboardJson(),
                        new TypeReference<List<StoryboardScene>>() {});
            } catch (JsonProcessingException e) {
                log.warn("Could not parse storyboard JSON for video {}", video.getId());
            }
        }

        return VideoGenerateResponse.builder()
                .videoId(video.getId())
                .patientId(video.getPatientId())
                .topic(video.getTopic())
                .memoryType(video.getMemoryType().name())
                .duration(video.getDuration())
                .status(video.getStatus().name())
                .videoUrl(video.getVideoUrl())
                .thumbnailUrl(video.getThumbnailUrl())
                .script(video.getScript())
                .storyboard(storyboard)
                .createdAt(video.getCreatedAt())
                .build();
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (JsonProcessingException e) { return "[]"; }
    }

    private String cleanJson(String response) {
        String cleaned = response.trim();
        if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
        else if (cleaned.startsWith("```")) cleaned = cleaned.substring(3);
        if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
        return cleaned.trim();
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(value.toString()); } catch (NumberFormatException e) { return 0; }
    }
}
