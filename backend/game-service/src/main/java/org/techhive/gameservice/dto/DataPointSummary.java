package org.techhive.gameservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.techhive.gameservice.entity.DataPointType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataPointSummary {
  private Long id;
  private DataPointType type;
  private String label;
  private String subtitle;
  private List<TagResponse> tags;
  private LocalDateTime createdAt;
  /** Only for PHOTO — base64 thumbnail */
  private String imagePreview;
  /** Only for MOVIE — poster URL */
  private String posterPath;
}
