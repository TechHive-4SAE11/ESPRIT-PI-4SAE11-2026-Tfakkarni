package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicationIntakeResponse {
    private Long id;
    private String medicationName;
    private String dosage;
    private String scheduledTime;
    private String takenAt;
    private String status;
    private String notes;
}
