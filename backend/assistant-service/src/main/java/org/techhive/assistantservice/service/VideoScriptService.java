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
    private final org.techhive.assistantservice.client.MedicalServiceClient medicalServiceClient;
    private final GeneratedVideoRepository videoRepository;
    private final ObjectMapper objectMapper;
    private final VideoApiIntegrationService videoApiIntegrationService;

    /**
     * Generate a personalized video script and storyboard for memory stimulation.
     */
    public VideoGenerateResponse generateVideoScript(VideoGenerateRequest request) {
        log.info("Generating video script: patient={}, topic={}, type={}, duration={}s",
                request.getPatientId(), request.getTopic(), request.getMemoryType(), request.getDuration());

        // 1. Gather patient context (weak topics from quiz history + medical folder)
        List<String> weakTopics = fetchWeakTopics(request.getPatientId());
        String medicalContext = fetchMedicalContext(request.getPatientId());

        // 2. Generate script + storyboard via OpenAI
        String aiResponse = callOpenAIForVideoScript(request, weakTopics, medicalContext);
        log.debug("OpenAI video script response: {}", aiResponse);

        // 3. Parse the response
        Map<String, Object> parsed = parseVideoScriptResponse(aiResponse);

        // 4. Build storyboard scenes
        List<StoryboardScene> storyboard = buildStoryboard(parsed);

        // 5. Persist the video record (initially as SCRIPT_ONLY)
        String scriptText = (String) parsed.getOrDefault("script", "");
        String aiTitle = (String) parsed.getOrDefault("title", "Topic Auto-généré");
        
        String finalTopic = (request.getTopic() != null && !request.getTopic().trim().isEmpty()) 
                ? request.getTopic() 
                : aiTitle;

        GeneratedVideo video = GeneratedVideo.builder()
                .patientId(request.getPatientId())
                .topic(finalTopic)
                .memoryType(MemoryType.valueOf(request.getMemoryType()))
                .duration(request.getDuration())
                .status(VideoStatus.SCRIPT_ONLY)
                .script(scriptText)
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
                .videoUrl(saved.getVideoUrl())
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
    
    private String fetchMedicalContext(Long patientId) {
        try {
            List<org.techhive.assistantservice.dto.MedicalFolderDTO> folders = medicalServiceClient.getMedicalFolderByPatient(String.valueOf(patientId));
            if (folders == null || folders.isEmpty()) return "";
            
            org.techhive.assistantservice.dto.MedicalFolderDTO folder = folders.get(0);
            
            StringBuilder sb = new StringBuilder();
            if (folder.getDiagnosis() != null) sb.append("Diagnosis: ").append(folder.getDiagnosis()).append("\n");
            if (folder.getEvolution() != null) sb.append("Evolution: ").append(folder.getEvolution()).append("\n");
            if (folder.getTreatments() != null) sb.append("Treatments: ").append(folder.getTreatments()).append("\n");
            if (folder.getRecommendations() != null) sb.append("Recommendations: ").append(folder.getRecommendations()).append("\n");
            return sb.toString();
        } catch (Exception e) {
            log.warn("Could not fetch medical folder for patient {}: {}", patientId, e.getMessage());
            return "";
        }
    }

    private String callOpenAIForVideoScript(VideoGenerateRequest request, List<String> weakTopics, String medicalContext) {
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
        if (request.getInterests() != null && !request.getInterests().trim().isEmpty()) patientContext += "Interests: " + request.getInterests() + "\n";
        if (!weakTopics.isEmpty()) patientContext += "Weak cognitive areas from Quiz history: " + String.join(", ", weakTopics) + "\n";
        if (!medicalContext.isEmpty()) patientContext += "Medical Context:\n" + medicalContext + "\n";

        String targetTopic = (request.getTopic() != null && !request.getTopic().trim().isEmpty()) 
                ? request.getTopic() 
                : "Auto-determined optimal therapeutic topic based on the patient's medical and cognitive profile";

        String prompt = String.format("""
                You are an expert in creating therapeutic video content for Alzheimer's and dementia patients.
                
                Create a video script about "%s" that is %d seconds long.
                If the topic is "Auto-determined...", you MUST heavily rely on the patient context below to invent a soothing, therapeutic journey (e.g. if they like cooking or have cognitive issues with short-term recall).
                
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
                - IMPORTANT: You MUST generate a complete 'storyboard' array regardless of the Video Type (PHOTO, STORY, or EXERCISE). It must never be empty.
                
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
                """, targetTopic, request.getDuration(), memoryTypeDescription,
                patientContext.isEmpty() ? "No specific patient context available." : patientContext);

        try {
            ChatClient chatClient = chatClientBuilder.build();
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("OpenAI API call failed for video script ({}), using fallback", e.getMessage());
            return generateFallbackVideoScript(request);
        }
    }

    /**
     * Fallback: generates a realistic video script JSON without calling OpenAI.
     */
    private String generateFallbackVideoScript(VideoGenerateRequest request) {
        String topic = request.getTopic() != null ? request.getTopic() : "Childhood memories";
        int duration = request.getDuration() != null ? request.getDuration() : 60;
        String memoryType = request.getMemoryType() != null ? request.getMemoryType() : "PHOTO";
        int sceneDuration = duration / 4;

        String script = switch (memoryType) {
            case "STORY" -> String.format("""
                {
                  "script": "Welcome to this memory journey about %s. Close your eyes for a moment and think back to a time that brings you joy. Picture the faces of the people you love, the places that feel like home. Remember the warmth of a sunny afternoon, the sound of laughter filling a room. These memories are treasures that stay with us forever. Let us explore them together, one gentle step at a time.",
                  "title": "Memory Journey: %s",
                  "storyboard": [
                    {"sceneNumber": 1, "description": "A warm sunrise over a peaceful garden with blooming flowers", "narration": "Let us begin our journey together. Take a deep breath and relax.", "durationSeconds": %d, "visualPrompt": "Warm golden sunrise over a beautiful flower garden, soft light, peaceful atmosphere"},
                    {"sceneNumber": 2, "description": "A cozy living room with family photos on the mantle", "narration": "Think about the people who matter most to you. Their smiles, their voices.", "durationSeconds": %d, "visualPrompt": "Cozy living room with vintage family photographs, warm lighting, comfortable furniture"},
                    {"sceneNumber": 3, "description": "A table set for a family meal with delicious food", "narration": "Remember the meals shared together? The flavors, the conversations, the laughter.", "durationSeconds": %d, "visualPrompt": "Beautiful family dinner table with home-cooked meal, warm candlelight, inviting atmosphere"},
                    {"sceneNumber": 4, "description": "A peaceful evening scene with stars in the sky", "narration": "These precious memories are always with you. Carry them in your heart.", "durationSeconds": %d, "visualPrompt": "Peaceful night sky full of stars, gentle moonlight, calming and serene atmosphere"}
                  ],
                  "emotionalTone": "warm and encouraging",
                  "cognitiveGoals": ["episodic memory recall", "emotional connection", "sensory stimulation"]
                }
                """, topic, topic, sceneDuration, sceneDuration, sceneDuration, sceneDuration);
            case "EXERCISE" -> String.format("""
                {
                  "script": "Welcome to this cognitive exercise session about %s. We will go through some fun activities designed to stimulate your memory. Take your time with each exercise — there is no rush. Let us start with something simple and work our way up. Remember, every effort you make strengthens your mind!",
                  "title": "Memory Exercise: %s",
                  "storyboard": [
                    {"sceneNumber": 1, "description": "A colorful introduction screen with the exercise title", "narration": "Welcome! Today we will exercise our memory together. Are you ready?", "durationSeconds": %d, "visualPrompt": "Bright colorful title card with brain icon, encouraging and friendly design"},
                    {"sceneNumber": 2, "description": "Three common objects displayed for memorization", "narration": "Look at these three objects carefully. Try to remember their names and colors.", "durationSeconds": %d, "visualPrompt": "Three everyday objects (apple, blue cup, yellow flower) on white background, clear and simple"},
                    {"sceneNumber": 3, "description": "A pattern recognition challenge with simple shapes", "narration": "Now, can you spot the pattern? Which shape comes next in the sequence?", "durationSeconds": %d, "visualPrompt": "Simple shape pattern sequence (circle, square, triangle, circle, square, question mark)"},
                    {"sceneNumber": 4, "description": "A congratulations screen with encouraging message", "narration": "Excellent work! You did a wonderful job. Keep exercising your mind every day!", "durationSeconds": %d, "visualPrompt": "Celebratory congratulations screen with gold stars and confetti, warm and encouraging"}
                  ],
                  "emotionalTone": "encouraging and supportive",
                  "cognitiveGoals": ["visual memory", "pattern recognition", "object recall"]
                }
                """, topic, topic, sceneDuration, sceneDuration, sceneDuration, sceneDuration);
            default -> String.format("""
                {
                  "script": "Welcome to this personalized memory experience about %s. This photo journey is designed to bring comfort and stimulate your cherished memories. Each image has been carefully chosen to evoke positive feelings and familiar scenes. Take your time looking at each photo and let the memories flow naturally.",
                  "title": "Photo Memories: %s",
                  "storyboard": [
                    {"sceneNumber": 1, "description": "A beautiful landscape with green hills and a blue sky", "narration": "Look at this beautiful view. Does it remind you of a place you have visited?", "durationSeconds": %d, "visualPrompt": "Beautiful pastoral landscape with rolling green hills, blue sky with white clouds, peaceful scene"},
                    {"sceneNumber": 2, "description": "A garden full of colorful flowers in bloom", "narration": "These flowers are so vibrant. Can you name the colors you see?", "durationSeconds": %d, "visualPrompt": "Lush garden with roses, sunflowers, and tulips in full bloom, bright and colorful"},
                    {"sceneNumber": 3, "description": "A kitchen scene with freshly baked cookies", "narration": "Imagine the smell of fresh cookies. Who used to bake your favorite treats?", "durationSeconds": %d, "visualPrompt": "Warm kitchen with freshly baked cookies on a tray, cozy and homey atmosphere"},
                    {"sceneNumber": 4, "description": "A sunset over a calm lake with reflections", "narration": "As we end our journey, remember that these beautiful moments are always yours to keep.", "durationSeconds": %d, "visualPrompt": "Golden sunset reflecting on a peaceful lake, warm tones, serene and calming"}
                  ],
                  "emotionalTone": "warm and nostalgic",
                  "cognitiveGoals": ["visual recognition", "sensory memory activation", "positive emotional recall"]
                }
                """, topic, topic, sceneDuration, sceneDuration, sceneDuration, sceneDuration);
        };

        return script;
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
                .sceneCount(storyboard.size())
                .createdAt(video.getCreatedAt())
                .build();
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (JsonProcessingException e) { return "[]"; }
    }

    private String cleanJson(String response) {
        try {
            int startInd = response.indexOf("```json");
            if (startInd != -1) {
                int endInd = response.lastIndexOf("```");
                if (endInd > startInd) {
                    return response.substring(startInd + 7, endInd).trim();
                }
            } else {
                startInd = response.indexOf("```");
                if (startInd != -1) {
                    int endInd = response.lastIndexOf("```");
                    if (endInd > startInd) {
                        return response.substring(startInd + 3, endInd).trim();
                    }
                }
            }
            int firstBrace = response.indexOf("{");
            int lastBrace = response.lastIndexOf("}");
            if (firstBrace != -1 && lastBrace > firstBrace) {
                return response.substring(firstBrace, lastBrace + 1).trim();
            }
        } catch (Exception e) {
            log.warn("Error cleaning JSON: {}", e.getMessage());
        }
        return response.trim();
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(value.toString()); } catch (NumberFormatException e) { return 0; }
    }
}
