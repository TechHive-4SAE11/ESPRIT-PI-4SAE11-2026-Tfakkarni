package org.techhive.gameservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuestionMemoryRequest {

  @NotBlank(message = "Question text is required")
  @Size(max = 500, message = "Question text must be at most 500 characters")
  private String questionText;

  @NotBlank(message = "Correct answer is required")
  @Size(max = 500, message = "Correct answer must be at most 500 characters")
  private String correctAnswer;

  private List<Long> tagIds;
}
