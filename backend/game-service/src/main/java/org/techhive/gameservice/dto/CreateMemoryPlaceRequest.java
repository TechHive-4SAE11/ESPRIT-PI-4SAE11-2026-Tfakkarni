package org.techhive.gameservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateMemoryPlaceRequest {
  private String name;
  private Double latitude;
  private Double longitude;
  private String hint;
  private List<Long> tagIds;
}
