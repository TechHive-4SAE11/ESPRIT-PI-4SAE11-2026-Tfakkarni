package org.techhive.gameservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionResponseDTO {
    private boolean correct;
    private Long quizId;
    private Long questionId;
    private Long answerId;
    private String explanation;
    private String feedback;
}