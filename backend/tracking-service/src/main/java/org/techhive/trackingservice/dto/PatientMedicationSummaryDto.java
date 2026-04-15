package org.techhive.trackingservice.dto;

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
public class PatientMedicationSummaryDto {
    /** All ACTIVE medication rows (same drug on multiple sessions counts multiple times). */
    private int totalActiveMedications;
    /** Distinct trimmed lower-case medication names. */
    private int distinctActiveMedications;
    /** One entry per active row (lower-case), used for interaction checks. */
    @Builder.Default
    private List<String> activeMedicationNames = new ArrayList<>();
}
