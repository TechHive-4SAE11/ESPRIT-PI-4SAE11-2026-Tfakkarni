package org.techhive.trackingservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "incident_entries")
@Data @NoArgsConstructor @AllArgsConstructor
public class IncidentEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_log_id", nullable = false)
    private DailyLog dailyLog;

    private String incidentType; // CHUTE, CONFUSION, AGITATION, DEAMBULATION, CRISE, AUTRE
    private String description;
    private String severity;     // LEGER, MODERE, GRAVE
    private String location;
    private String actionTaken;
    private String injuryDetails;
    private String occurredAt;
}