package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CareActivityRequestDTO {
    private String activityName;
    private String description;
    private String frequency;
    private String duration;
}
