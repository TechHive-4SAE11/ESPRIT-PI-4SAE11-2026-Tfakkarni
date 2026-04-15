package org.techhive.iotservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IotAlertRequestDTO {
    private String patientId;
    private String alertType; // ELEVATED_BPM, LOW_BPM
    private Integer value;    // BPM reading
    private String message;
}
