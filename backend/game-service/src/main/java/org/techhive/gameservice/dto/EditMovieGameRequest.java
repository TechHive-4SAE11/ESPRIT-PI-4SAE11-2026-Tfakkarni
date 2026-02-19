package org.techhive.gameservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public class EditMovieGameRequest {

  @NotBlank(message = "Title is required")
  @Size(min = 3, max = 20, message = "Title must be between 3 and 20 characters")
  @Pattern(regexp = "^[a-zA-Z0-9 ]+$", message = "Title can only contain letters, numbers, and spaces")
  private String title;

  @Size(max = 100, message = "Description must be at most 100 characters")
  private String description;

  @NotEmpty(message = "At least one movie is required")
  @Valid
  private List<MovieItemEntry> movies;

  public EditMovieGameRequest() {
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<MovieItemEntry> getMovies() {
    return movies;
  }

  public void setMovies(List<MovieItemEntry> movies) {
    this.movies = movies;
  }

  /**
   * Each entry can either reference an existing item (by id) or be a brand-new
   * movie (id null).
   * Existing items not present in the list will be removed (orphanRemoval).
   */
  public static class MovieItemEntry {
    private Long id; // null for new items
    private int tmdbId;
    private String originalTitle;
    private String posterPath;
    private String releaseDate;

    @NotBlank(message = "Character name (correct answer) is required")
    @Size(max = 20, message = "Character name must be at most 20 characters")
    private String correctAnswer;

    public MovieItemEntry() {
    }

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public int getTmdbId() {
      return tmdbId;
    }

    public void setTmdbId(int tmdbId) {
      this.tmdbId = tmdbId;
    }

    public String getOriginalTitle() {
      return originalTitle;
    }

    public void setOriginalTitle(String originalTitle) {
      this.originalTitle = originalTitle;
    }

    public String getPosterPath() {
      return posterPath;
    }

    public void setPosterPath(String posterPath) {
      this.posterPath = posterPath;
    }

    public String getReleaseDate() {
      return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
      this.releaseDate = releaseDate;
    }

    public String getCorrectAnswer() {
      return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
      this.correctAnswer = correctAnswer;
    }
  }
}
