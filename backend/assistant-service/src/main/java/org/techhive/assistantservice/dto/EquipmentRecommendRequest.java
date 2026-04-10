package org.techhive.assistantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentRecommendRequest {

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotBlank(message = "Condition is required")
    private String condition;  // MOBILITY, RESPIRATORY, CARDIAC, etc.

    @NotBlank(message = "Severity is required")
    private String severity;   // MILD, MODERATE, SEVERE
}
