package org.techhive.iotservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureGateResponse {
    private String patientKeycloakId;
    private String stage;
    private boolean iotEnabled;
    private String iotLevel;
    private String gameComplexity;
    private String monitoringLevel;
    private String uiMode;
    private boolean safeZoneRequired;
}
