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
public class UnifiedPlayResult {
  private Long attemptId;
  private int score;
  private int totalQuestions;
  private double percentage;
  private Integer durationSeconds;
  private LocalDateTime completedAt;
  private List<ItemResult> results;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ItemResult {
    private DataPointType type;
    private Long itemId;
    private boolean correct;
    private String correctAnswer;
    private String selectedAnswer;
    private String label;
  }
}
