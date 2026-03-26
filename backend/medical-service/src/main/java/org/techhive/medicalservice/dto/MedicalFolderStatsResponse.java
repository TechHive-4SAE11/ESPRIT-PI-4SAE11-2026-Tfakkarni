package org.techhive.medicalservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalFolderStatsResponse {

    private long total;
    private long thisMonth;
    private long thisWeek;
    private long patientCount;
}
