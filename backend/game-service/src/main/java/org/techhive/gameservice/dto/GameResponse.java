package org.techhive.gameservice.dto;

import java.time.LocalDateTime;

public class GameResponse {
    private Long id;
    private String patientKeycloakId;
    private String title;
    private String description;
    private int imageCount;
    private LocalDateTime createdAt;

    public GameResponse() {
    }

    public GameResponse(Long id, String patientKeycloakId, String title, String description, int imageCount, LocalDateTime createdAt) {
        this.id = id;
        this.patientKeycloakId = patientKeycloakId;
        this.title = title;
        this.description = description;
        this.imageCount = imageCount;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPatientKeycloakId() { return patientKeycloakId; }
    public void setPatientKeycloakId(String patientKeycloakId) { this.patientKeycloakId = patientKeycloakId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getImageCount() { return imageCount; }
    public void setImageCount(int imageCount) { this.imageCount = imageCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
