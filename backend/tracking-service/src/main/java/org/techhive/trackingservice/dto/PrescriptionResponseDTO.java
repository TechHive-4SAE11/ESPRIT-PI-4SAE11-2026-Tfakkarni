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
public class PrescriptionResponseDTO {
    
    private Long id;
    private Long sessionId;
    private List<MedicationResponseDTO> medications = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
