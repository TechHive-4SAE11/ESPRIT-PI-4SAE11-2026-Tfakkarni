package org.techhive.assistantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoGenerateResponse {
    private Long videoId;
    private Long patientId;
    private String topic;
    private String memoryType;
    private Integer duration;
    private String status;       // GENERATING, READY, FAILED
    private String videoUrl;
    private String thumbnailUrl;
    private String script;
    private List<StoryboardScene> storyboard;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoryboardScene {
        private int sceneNumber;
        private String description;
        private String narration;
        private int durationSeconds;
        private String visualPrompt;
    }
}
