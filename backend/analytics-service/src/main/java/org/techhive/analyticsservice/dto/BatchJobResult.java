package org.techhive.analyticsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchJobResult {
    private String jobName;
    private String status;
    private int processedCount;
    private int errorCount;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private long durationMs;
    private String message;
}
