package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyLogResponse {
    private Long id;
    private String patientKeycloakId;
    private LocalDate logDate;
    private String globalNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<NutritionEntryResponse> nutritionEntries;
    private List<MedicationIntakeLogResponse> medicationIntakes;
    private List<ActivityEntryResponse> activityEntries;
    private List<IncidentEntryResponse> incidentEntries;
}
