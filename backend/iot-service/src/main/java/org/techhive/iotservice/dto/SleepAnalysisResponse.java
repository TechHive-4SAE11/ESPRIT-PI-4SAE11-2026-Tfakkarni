package org.techhive.iotservice.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SleepAnalysisResponse {
    private String patientId;
    private LocalDate date;
    private List<SleepStageEntry> timeline;
    private SleepSummary summary;
    private List<String> insights;
}
