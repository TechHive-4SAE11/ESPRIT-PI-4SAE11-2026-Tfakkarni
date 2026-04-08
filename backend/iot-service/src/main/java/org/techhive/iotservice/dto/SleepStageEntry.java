package org.techhive.iotservice.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SleepStageEntry {
    private LocalDateTime timestamp;
    private int bpm;
    private String stage; // AWAKE, LIGHT, DEEP, REM
}
