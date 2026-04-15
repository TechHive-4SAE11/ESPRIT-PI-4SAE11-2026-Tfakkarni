package org.techhive.mlservice.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AppointmentResponseDTO {
    private Long id;
    private String title;
    private String description;
    private String patientId;
    private String doctorId;
    private String status;
    private Integer minutesLate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}