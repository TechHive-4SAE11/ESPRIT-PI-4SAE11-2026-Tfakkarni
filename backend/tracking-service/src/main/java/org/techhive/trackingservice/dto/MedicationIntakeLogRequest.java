package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicationIntakeLogRequest {
    private Long medicationId;
    private String takenAt;
    private String status;  // PRIS, OUBLIE, REFUSE, EN_RETARD
    private String notes;
}
