package org.techhive.gameservice.dto;

import java.util.List;

public class EditMovieGameRequest {
  private String title;
  private String description;
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
