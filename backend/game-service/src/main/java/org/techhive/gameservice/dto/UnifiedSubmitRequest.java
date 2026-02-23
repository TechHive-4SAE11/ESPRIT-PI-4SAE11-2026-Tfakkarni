package org.techhive.gameservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.techhive.gameservice.entity.DataPointType;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedSubmitRequest {
  private Long gameId; // null for random games
  private int score;
  private int totalQuestions;
  private Integer durationSeconds;
  private List<AnswerEntry> answers;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AnswerEntry {
    private DataPointType type;
    private Long itemId;
    private String selectedAnswer;
    /** Only for QUESTION type: patient self-assessed this as correct */
    private Boolean selfAssessedCorrect;
  }
}
