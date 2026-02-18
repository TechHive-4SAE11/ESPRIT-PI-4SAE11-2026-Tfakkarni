package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicationIntakeLogResponse {
    private Long id;
    private Long medicationId;
    private String medicationName;
    private String dosage;
    private String frequency;
    private String takenAt;
    private String status;
    private String notes;
}
