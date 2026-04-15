package org.techhive.alertservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IotAlertRequestDTO {

    @NotBlank(message = "Patient ID is required")
    private String patientId;

    @NotBlank(message = "Alert type is required")
    private String alertType; // ELEVATED_BPM, LOW_BPM

    @NotNull(message = "BPM value is required")
    private Integer value;

    private String message;
}
