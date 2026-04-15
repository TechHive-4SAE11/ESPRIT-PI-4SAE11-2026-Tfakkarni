package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientMedicationAuditResponse {
    @Builder.Default
    private Map<String, PatientMedicationSummaryDto> patients = new HashMap<>();
}
