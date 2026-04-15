package org.techhive.medicalservice.dto.audit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PatientMedicationSummaryDto {
    private int totalActiveMedications;
    private int distinctActiveMedications;
    @Builder.Default
    private List<String> activeMedicationNames = new ArrayList<>();
}
