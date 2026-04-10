package org.techhive.assistantservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizDTO {
    private Long id;
    private String topic;
    private Integer totalScore;
    private LocalDateTime dateTaken;
    private Long caregiverId;
    private Integer levelReached;
    private List<QuestionDTO> questions;
}
