package org.techhive.medicalservice.dto.tracking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationLogDTO {
    private String medicationName;
    private String status;
    private LocalDateTime timestamp;
}
