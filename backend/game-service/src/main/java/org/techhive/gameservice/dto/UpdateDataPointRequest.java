package org.techhive.gameservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic update DTO used for all data point types.
 * Only non-null fields are applied (patch semantics).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDataPointRequest {
  /** Name/label (PHOTO & PLACE) */
  private String name;
  /** Hint (PLACE only) */
  private String hint;
  /** Latitude (PLACE only) */
  private Double latitude;
  /** Longitude (PLACE only) */
  private Double longitude;
  /** Movie character name / question answer (MOVIE & QUESTION) */
  private String correctAnswer;
  /** Question text (QUESTION only) */
  private String questionText;
  /** Tag IDs to replace */
  private List<Long> tagIds;
}
