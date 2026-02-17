package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicationResponseDTO {
    private Long id;
    private String medicationName;
    private String dosage;
    private String frequency;
    private String duration;
    private String instructions;
    private LocalDateTime createdAt;
}
