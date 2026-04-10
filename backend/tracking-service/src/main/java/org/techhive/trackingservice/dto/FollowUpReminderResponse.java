package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowUpReminderResponse {
    private Long id;
    private String patientKeycloakId;
    private String patientName;
    private LocalDate reminderDate;
    private String message;
    private String missingCategories;
    private boolean read;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
