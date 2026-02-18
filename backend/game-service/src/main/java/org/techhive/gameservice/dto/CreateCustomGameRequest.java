package org.techhive.gameservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.techhive.gameservice.entity.DataPointType;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomGameRequest {
  private String title;
  private String description;
  private List<GameItemEntry> items;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GameItemEntry {
    private DataPointType dataType;
    private Long dataPointId;
  }
}
