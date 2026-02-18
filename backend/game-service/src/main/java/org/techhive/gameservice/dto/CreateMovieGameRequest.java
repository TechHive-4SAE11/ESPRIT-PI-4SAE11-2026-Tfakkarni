package org.techhive.gameservice.dto;

import java.util.List;

public class CreateMovieGameRequest {
  private String title;
  private String description;
  private List<MovieItemRequest> movies;

  public CreateMovieGameRequest() {
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

  public List<MovieItemRequest> getMovies() {
    return movies;
  }

  public void setMovies(List<MovieItemRequest> movies) {
    this.movies = movies;
  }

  public static class MovieItemRequest {
    private int tmdbId;
    private String originalTitle;
    private String posterPath;
    private String releaseDate;
    private String correctAnswer;

    public MovieItemRequest() {
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
