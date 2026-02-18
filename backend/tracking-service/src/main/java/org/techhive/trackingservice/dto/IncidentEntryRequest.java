package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidentEntryRequest {
    private String incidentType;
    private String description;
    private String severity;
    private String location;
    private String actionTaken;
    private String injuryDetails;
    private String occurredAt;
}
