package org.techhive.gameservice.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full game detail response including all items, used for edit view.
 */
public class MovieGameDetailResponse {
  private Long id;
  private String patientKeycloakId;
  private String title;
  private String description;
  private List<MovieItemDetail> movies;
  private LocalDateTime createdAt;

  public MovieGameDetailResponse() {
  }

  public MovieGameDetailResponse(Long id, String patientKeycloakId, String title, String description,
      List<MovieItemDetail> movies, LocalDateTime createdAt) {
    this.id = id;
    this.patientKeycloakId = patientKeycloakId;
    this.title = title;
    this.description = description;
    this.movies = movies;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getPatientKeycloakId() {
    return patientKeycloakId;
  }

  public void setPatientKeycloakId(String patientKeycloakId) {
    this.patientKeycloakId = patientKeycloakId;
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

  public List<MovieItemDetail> getMovies() {
    return movies;
  }

  public void setMovies(List<MovieItemDetail> movies) {
    this.movies = movies;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public static class MovieItemDetail {
    private Long id;
    private int tmdbId;
    private String originalTitle;
    private String posterPath;
    private String releaseDate;
    private String correctAnswer;

    public MovieItemDetail() {
    }

    public MovieItemDetail(Long id, int tmdbId, String originalTitle, String posterPath,
        String releaseDate, String correctAnswer) {
      this.id = id;
      this.tmdbId = tmdbId;
      this.originalTitle = originalTitle;
      this.posterPath = posterPath;
      this.releaseDate = releaseDate;
      this.correctAnswer = correctAnswer;
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
