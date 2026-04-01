package org.techhive.alertservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SafeZoneRequestDTO {

  @NotBlank(message = "Zone name is required")
  private String name;

  @NotNull(message = "Points are required")
  @Size(min = 3, message = "At least 3 points are required to form a polygon")
  private List<LatLngDTO> points;

  private boolean active = true;
}
