package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionRequestDTO {
    
    private Long medicalFolderId;
    private LocalDateTime sessionDate;
    private String notes;

    public Long getMedicalFolderId() { return medicalFolderId; }
    public void setMedicalFolderId(Long medicalFolderId) { this.medicalFolderId = medicalFolderId; }
    public LocalDateTime getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDateTime sessionDate) { this.sessionDate = sessionDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
