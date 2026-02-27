package org.techhive.gameservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.techhive.gameservice.entity.Quiz;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizDTO {
    private Long id;

    @NotBlank(message = "Quiz topic is required")
    private String topic;

    @NotNull(message = "Total score must not be null")
    @Min(value = 0, message = "Total score cannot be negative")
    private Integer totalScore;

    @PastOrPresent(message = "Date taken cannot be in the future")
    private LocalDateTime dateTaken;

    @NotNull(message = "Caregiver ID is required")
    private Long caregiverId;

    private List<QuestionDTO> questions; // Optionnel, pour les questions associées

    // Convert Entity to DTO
    public static QuizDTO fromEntity(Quiz quiz) {
        if (quiz == null)
            return null;

        QuizDTOBuilder builder = QuizDTO.builder()
                .id(quiz.getId())
                .topic(quiz.getTopic())
                .totalScore(quiz.getTotalScore())
                .dateTaken(quiz.getDateTaken())
                .caregiverId(quiz.getCaregiverId());

        if (quiz.getQuestions() != null && !quiz.getQuestions().isEmpty()) {
            builder.questions(quiz.getQuestions().stream()
                    .map(QuestionDTO::fromEntity)
                    .toList());
        }

        return builder.build();
    }

    // Convert DTO to Entity
    public Quiz toEntity() {
        Quiz quiz = new Quiz();
        quiz.setId(this.id);
        quiz.setTopic(this.topic);
        quiz.setTotalScore(this.totalScore);
        quiz.setDateTaken(this.dateTaken);
        quiz.setCaregiverId(this.caregiverId);
        return quiz;
    }
}