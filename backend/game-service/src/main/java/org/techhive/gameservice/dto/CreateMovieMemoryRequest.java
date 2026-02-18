package org.techhive.gameservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateMovieMemoryRequest {
  private int tmdbId;
  private String originalTitle;
  private String posterPath;
  private String releaseDate;
  private String correctAnswer;
  private List<Long> tagIds;
}
