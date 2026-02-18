package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.techhive.trackingservice.enums.CareActivityType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CareActivityRequestDTO {
    private String activityName;
    private CareActivityType activityType;
    private String description;
    private String frequency;
    private String duration;
}
