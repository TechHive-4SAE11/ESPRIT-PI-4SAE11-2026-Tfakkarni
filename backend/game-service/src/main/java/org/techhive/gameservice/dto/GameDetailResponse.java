package org.techhive.gameservice.dto;

import java.time.LocalDateTime;
import java.util.List;

public class GameDetailResponse {
    private Long id;
    private String patientKeycloakId;
    private String title;
    private String description;
    private List<ImageDetail> images;
    private LocalDateTime createdAt;

    public GameDetailResponse() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPatientKeycloakId() { return patientKeycloakId; }
    public void setPatientKeycloakId(String patientKeycloakId) { this.patientKeycloakId = patientKeycloakId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<ImageDetail> getImages() { return images; }
    public void setImages(List<ImageDetail> images) { this.images = images; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static class ImageDetail {
        private Long id;
        private String name;
        private String imageBase64;
        private String contentType;
        private int displayOrder;

        public ImageDetail() {
        }

        public ImageDetail(Long id, String name, String imageBase64, String contentType, int displayOrder) {
            this.id = id;
            this.name = name;
            this.imageBase64 = imageBase64;
            this.contentType = contentType;
            this.displayOrder = displayOrder;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getImageBase64() { return imageBase64; }
        public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        public int getDisplayOrder() { return displayOrder; }
        public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    }
}
