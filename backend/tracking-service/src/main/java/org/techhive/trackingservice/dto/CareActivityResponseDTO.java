package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.techhive.trackingservice.enums.CareActivityType;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CareActivityResponseDTO {
    private Long id;
    private String activityName;
    private CareActivityType activityType;
    private String description;
    private String frequency;
    private String duration;
    private String completionStatus;
    private LocalDateTime createdAt;
}
