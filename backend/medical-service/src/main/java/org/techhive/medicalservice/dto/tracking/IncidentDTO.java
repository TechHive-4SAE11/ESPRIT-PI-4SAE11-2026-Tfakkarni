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
public class IncidentDTO {
    private String type;
    private String description;
    private LocalDateTime occurredAt;
}
