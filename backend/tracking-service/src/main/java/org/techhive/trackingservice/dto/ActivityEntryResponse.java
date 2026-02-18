package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityEntryResponse {
    private Long id;
    private String activityType;
    private String description;
    private Integer durationMinutes;
    private String intensity;
    private String notes;
    private String startTime;
}
