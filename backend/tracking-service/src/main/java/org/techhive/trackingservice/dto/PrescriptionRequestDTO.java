package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionRequestDTO {
    
    @NotNull(message = "Session ID is required")
    private Long sessionId;
    
    @NotEmpty(message = "At least one medication is required")
    @Valid
    private List<MedicationRequestDTO> medications = new ArrayList<>();

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public List<MedicationRequestDTO> getMedications() { return medications; }
    public void setMedications(List<MedicationRequestDTO> medications) { this.medications = medications; }
}
