package org.techhive.assistantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoGenerateRequest {

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotBlank(message = "Topic is required")
    private String topic;

    @NotBlank(message = "Memory type is required")
    private String memoryType;  // PHOTO, STORY, EXERCISE

    @NotNull(message = "Duration is required")
    @Min(value = 30, message = "Minimum duration is 30 seconds")
    @Max(value = 120, message = "Maximum duration is 120 seconds")
    private Integer duration;

    // Optional patient context
    private String patientName;
    private Integer patientAge;
    private String interests;
}
