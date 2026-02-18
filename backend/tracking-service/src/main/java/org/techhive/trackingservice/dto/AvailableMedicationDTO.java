package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailableMedicationDTO {
    private Long id;
    private String medicationName;
    private String dosage;
    private String frequency;
    private String instructions;
}
