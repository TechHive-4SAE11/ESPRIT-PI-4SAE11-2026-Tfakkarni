package org.techhive.medicalservice.dto.tracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationComplianceDTO {
    private String medicationName;
    private String startDate;
    private String endDate;
    private int taken;
    private int missed;
}
