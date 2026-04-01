package org.techhive.alertservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeofenceAlertResponseDTO {
  private Long id;
  private String patientId;
  private Double latitude;
  private Double longitude;
  private String safeZoneName;
  private boolean acknowledged;
  private LocalDateTime acknowledgedAt;
  private LocalDateTime createdAt;
}
