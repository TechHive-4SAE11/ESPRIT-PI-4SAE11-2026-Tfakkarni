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
public class CreateMovieMemoryRequest {
  private int tmdbId;

  @NotBlank(message = "Original title is required")
  private String originalTitle;

  private String posterPath;
  private String releaseDate;

  @NotBlank(message = "Character name (correct answer) is required")
  @Size(max = 20, message = "Character name must be at most 20 characters")
  private String correctAnswer;

  private List<Long> tagIds;
}
