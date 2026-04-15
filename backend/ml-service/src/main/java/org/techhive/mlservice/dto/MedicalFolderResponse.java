package org.techhive.mlservice.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MedicalFolderResponse {
    private Long id;
    private String patientId;
    private String doctorId;
    private String bloodType;
    private Double height;
    private Double weight;
    private String allergies;
    private String antecedents;
    private String symptomes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
