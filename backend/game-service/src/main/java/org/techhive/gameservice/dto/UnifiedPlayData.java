package org.techhive.gameservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.techhive.gameservice.entity.DataPointType;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnifiedPlayData {
  private Long gameId;
  private String title;
  private int totalQuestions;
  private int optionCount; // 2=MINIMAL, 3=SIMPLIFIED, 4=STANDARD
  private String gameComplexity; // STANDARD, SIMPLIFIED, MINIMAL
  private List<UnifiedPlayItem> items;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class UnifiedPlayItem {
    private int index;
    private DataPointType type;
    private Long itemId;

    // PHOTO fields
    private String imageBase64;
    private String imageContentType;

    // MOVIE fields
    private String posterUrl;
    private String movieTitle;

    // QUESTION fields
    private String questionText;
    private String correctAnswer; // sent only for QUESTION type (self-assess)

    // PLACE fields
    private Double latitude;
    private Double longitude;
    private String hint;

    // MCQ choices for PHOTO, MOVIE, PLACE
    private List<String> choices;
  }
}
