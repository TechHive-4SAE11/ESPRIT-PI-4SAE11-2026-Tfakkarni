package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicationRequestDTO {
    private String medicationName;
    private String dosage;
    private String frequency;
    private String duration;
    private String instructions;
}
