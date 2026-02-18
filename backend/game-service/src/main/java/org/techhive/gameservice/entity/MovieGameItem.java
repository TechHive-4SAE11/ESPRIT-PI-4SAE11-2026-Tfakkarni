package org.techhive.gameservice.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "movie_game_items")
public class MovieGameItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "movie_game_id", nullable = false)
  private MovieGame movieGame;

  @Column(name = "tmdb_id", nullable = false)
  private int tmdbId;

  @Column(name = "original_title", nullable = false)
  private String originalTitle;

  @Column(name = "poster_path")
  private String posterPath;

  @Column(name = "release_date")
  private String releaseDate;

  @Column(name = "correct_answer", nullable = false)
  private String correctAnswer;

  @Column(name = "display_order")
  private int displayOrder;

  public MovieGameItem() {
  }

  public MovieGameItem(MovieGame movieGame, int tmdbId, String originalTitle, String posterPath,
      String releaseDate, String correctAnswer, int displayOrder) {
    this.movieGame = movieGame;
    this.tmdbId = tmdbId;
    this.originalTitle = originalTitle;
    this.posterPath = posterPath;
    this.releaseDate = releaseDate;
    this.correctAnswer = correctAnswer;
    this.displayOrder = displayOrder;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public MovieGame getMovieGame() {
    return movieGame;
  }

  public void setMovieGame(MovieGame movieGame) {
    this.movieGame = movieGame;
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

  public int getDisplayOrder() {
    return displayOrder;
  }

  public void setDisplayOrder(int displayOrder) {
    this.displayOrder = displayOrder;
  }
}
