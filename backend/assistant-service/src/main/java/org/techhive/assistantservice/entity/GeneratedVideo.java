package org.techhive.assistantservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.techhive.assistantservice.entity.enums.MemoryType;
import org.techhive.assistantservice.entity.enums.VideoStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "generated_videos")
public class GeneratedVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemoryType memoryType;

    @Column(nullable = false)
    private Integer duration;  // in seconds

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoStatus status;

    @Column(length = 1000)
    private String videoUrl;

    @Column(length = 1000)
    private String thumbnailUrl;

    @Column(columnDefinition = "TEXT")
    private String script;

    @Column(columnDefinition = "TEXT")
    private String storyboardJson;  // JSON array of storyboard scenes

    private String patientName;
    private Integer patientAge;

    @Column(length = 500)
    private String interests;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
