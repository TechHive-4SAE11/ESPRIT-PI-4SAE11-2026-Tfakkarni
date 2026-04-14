package org.techhive.analyticsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionResponseDTO {
    private Long id;
    private Long sessionId;
    private String doctorId;
    private List<MedicationResponseDTO> medications;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
