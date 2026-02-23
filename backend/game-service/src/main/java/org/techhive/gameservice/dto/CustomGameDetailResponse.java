package org.techhive.gameservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.techhive.gameservice.entity.DataPointType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomGameDetailResponse {
  private Long id;
  private String title;
  private String description;
  private Set<DataPointType> itemTypes;
  private List<DataPointSummary> items;
  private LocalDateTime createdAt;
}
