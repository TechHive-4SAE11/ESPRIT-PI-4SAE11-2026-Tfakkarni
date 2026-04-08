package org.techhive.iotservice.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeartbeatReadingDTO {
    private Long id;
    private String patientId;
    private int bpm;
    private LocalDateTime timestamp;
}
