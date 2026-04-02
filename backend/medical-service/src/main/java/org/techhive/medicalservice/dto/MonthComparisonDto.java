package org.techhive.medicalservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthComparisonDto {
    private long thisMonthDiagnostics;
    private long lastMonthDiagnostics;
    private long thisMonthFolders;
    private long lastMonthFolders;
}
