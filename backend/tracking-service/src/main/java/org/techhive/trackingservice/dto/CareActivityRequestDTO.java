package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.techhive.trackingservice.enums.CareActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CareActivityRequestDTO {
    @NotBlank(message = "Please enter the activity name")
    private String activityName;

    @NotNull(message = "Please select an activity type (PHYSICAL, COGNITIVE, SOCIAL, or DAILY_LIVING)")
    private CareActivityType activityType;

    @NotBlank(message = "Please provide a description of the activity")
    private String description;

    private String frequency;
    private String duration;
}
