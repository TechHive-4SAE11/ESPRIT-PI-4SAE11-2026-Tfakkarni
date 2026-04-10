package org.techhive.assistantservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDTO {
    private Long id;
    private String text;
    private Integer difficultyLevel;
    private String mediaAttachment;
    private Long quizId;
    private List<AnswerDTO> answers;
}
