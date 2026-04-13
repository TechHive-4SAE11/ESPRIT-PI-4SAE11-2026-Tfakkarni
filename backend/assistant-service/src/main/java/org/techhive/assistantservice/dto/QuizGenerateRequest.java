package org.techhive.assistantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizGenerateRequest {

    @NotBlank(message = "Topic is required")
    private String topic;

    @NotNull(message = "Number of questions is required")
    @Min(value = 1, message = "Must have at least 1 question")
    @Max(value = 20, message = "Maximum 20 questions")
    private Integer numberOfQuestions;

    @NotNull(message = "Difficulty level is required")
    @Min(value = 1, message = "Difficulty must be between 1 and 3")
    @Max(value = 3, message = "Difficulty must be between 1 and 3")
    private Integer difficultyLevel;

    @NotNull(message = "Caregiver ID is required")
    private Long caregiverId;

    private String customContext;  // contexte personnalisé du patient
}
