package org.techhive.analyticsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.techhive.analyticsservice.entity.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureGateResponse {
    private String patientKeycloakId;
    private AlzheimerStage stage;
    private boolean iotEnabled;
    private IotLevel iotLevel;
    private GameComplexity gameComplexity;
    private MonitoringLevel monitoringLevel;
    private EscalationLevel notificationEscalation;
    private UiMode uiMode;
    private boolean safeZoneRequired;
    private int meetingSuggestedFrequencyDays;
    private LocalDateTime computedAt;
}
