package org.techhive.medicalservice.dto.audit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class PatientMedicationAuditResponse {
    @Builder.Default
    private Map<String, PatientMedicationSummaryDto> patients = new HashMap<>();
}
