package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicalFolderResponseDTO {

    private Long id;
    private String patientId;
    private String doctorId;
    private String bloodType;
    private Double height;
    private Double weight;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
