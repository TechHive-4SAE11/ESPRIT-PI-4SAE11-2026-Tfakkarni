package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicationRequestDTO {
    @NotBlank(message = "Medication name is required")
    private String medicationName;
    @NotBlank(message = "Dosage is required")
    private String dosage;
    @NotBlank(message = "Frequency is required")
    private String frequency;
    @NotBlank(message = "Duration is required")
    private String duration;
    private String instructions;
}
