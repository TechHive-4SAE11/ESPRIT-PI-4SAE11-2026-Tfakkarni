package org.techhive.alertservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IotAlertResponseDTO {

    private Long id;
    private String patientId;
    private String alertType;
    private int value;
    private String message;
    private boolean acknowledged;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime createdAt;
}
