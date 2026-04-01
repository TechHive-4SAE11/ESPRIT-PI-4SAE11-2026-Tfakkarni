package org.techhive.alertservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeofenceAlertRequestDTO {

  @NotBlank(message = "Patient ID is required")
  private String patientId;

  @NotNull(message = "Latitude is required")
  private Double latitude;

  @NotNull(message = "Longitude is required")
  private Double longitude;

  private String safeZoneName;
}
