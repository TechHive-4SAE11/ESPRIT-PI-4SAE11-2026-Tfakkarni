package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionRequestDTO {
    
    private Long sessionId;
    private List<MedicationRequestDTO> medications = new ArrayList<>();
}
