package org.techhive.gameservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.techhive.gameservice.entity.Answer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerDTO {
    private Long id;

    @NotBlank(message = "Answer text is required")
    private String text;

    @NotNull(message = "isCorrect flag is required")
    @JsonProperty("isCorrect")
    private Boolean isCorrect;

    private String explanation;

    @NotNull(message = "Question ID is required")
    private Long questionId;

    // Convert Entity to DTO
    public static AnswerDTO fromEntity(Answer answer) {
        if (answer == null)
            return null;

        return AnswerDTO.builder()
                .id(answer.getId())
                .text(answer.getText())
                .isCorrect(answer.getIsCorrect())
                .explanation(answer.getExplanation())
                .questionId(answer.getQuestion() != null ? answer.getQuestion().getId() : null)
                .build();
    }

    // Convert DTO to Entity
    public Answer toEntity() {
        Answer answer = new Answer();
        answer.setId(this.id);
        answer.setText(this.text);
        answer.setIsCorrect(this.isCorrect);
        answer.setExplanation(this.explanation);
        // Question will be set in service layer
        return answer;
    }
}