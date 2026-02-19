package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarePlanResponseDTO {
    
    private Long id;
    private Long sessionId;
    private String doctorId;
    private List<CareActivityResponseDTO> activities = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
