package org.techhive.alertservice.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SafeZoneResponseDTO {
  private Long id;
  private String patientId;
  private String name;
  private List<LatLngDTO> points;
  private boolean active;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
