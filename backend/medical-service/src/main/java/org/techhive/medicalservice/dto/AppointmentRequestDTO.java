package org.techhive.medicalservice.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import org.techhive.medicalservice.entity.AppointmentType;

public class AppointmentRequestDTO {
    @NotBlank(message = "Le titre est obligatoire")
    private String title;
    
    private String description;
    
    @NotNull(message = "L'ID du patient est obligatoire")
    private String patientId;
    
    private String doctorId;
    
    @NotNull(message = "La date de début est obligatoire")
    @Future(message = "La date de début doit être dans le futur")
    private LocalDateTime startTime;
    
    @NotNull(message = "La date de fin est obligatoire")
    private LocalDateTime endTime;
    
    @NotNull(message = "Le type de rendez-vous est obligatoire")
    private AppointmentType type;
    
    private String notes;
    
    // Getters et Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    
    public AppointmentType getType() { return type; }
    public void setType(AppointmentType type) { this.type = type; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

