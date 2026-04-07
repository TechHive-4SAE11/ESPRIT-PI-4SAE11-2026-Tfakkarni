package org.techhive.alertservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoachingPushRequest {
    private String patientId;
    private String title;
    private String body;
    private Long goalId;
    /** e.g. STALE, REMINDER */
    private String notificationSubType;
}
