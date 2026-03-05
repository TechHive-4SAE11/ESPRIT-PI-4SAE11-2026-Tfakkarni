package org.techhive.gameservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.techhive.gameservice.entity.Question;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDTO {
    private Long id;

    @NotBlank(message = "Question text is required")
    private String text;

    @Min(value = 1, message = "Difficulty level must be at least 1")
    private Integer difficultyLevel;

    private String mediaAttachment;

    @NotNull(message = "Quiz ID is required")
    private Long quizId;

    private List<AnswerDTO> answers; // Optionnel, pour les réponses associées

    // Convert Entity to DTO
    public static QuestionDTO fromEntity(Question question) {
        if (question == null) return null;

        QuestionDTOBuilder builder = QuestionDTO.builder()
                .id(question.getId())
                .text(question.getText())
                .difficultyLevel(question.getDifficultyLevel())
                .mediaAttachment(question.getMediaAttachment());

        if (question.getQuiz() != null) {
            builder.quizId(question.getQuiz().getId());
        }

        if (question.getAnswers() != null && !question.getAnswers().isEmpty()) {
            builder.answers(question.getAnswers().stream()
                    .map(AnswerDTO::fromEntity)
                    .toList());
        }

        return builder.build();
    }

    // Convert DTO to Entity
    public Question toEntity() {
        Question question = new Question();
        question.setId(this.id);
        question.setText(this.text);
        question.setDifficultyLevel(this.difficultyLevel);
        question.setMediaAttachment(this.mediaAttachment);
        // Quiz will be set in service layer
        return question;
    }
}