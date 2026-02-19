package org.techhive.gameservice.dto;

import java.util.List;

public class MovieGamePlayData {
  private Long gameId;
  private String title;
  private String description;
  private List<MovieQuestion> questions;
  private int totalQuestions;

  public MovieGamePlayData() {
  }

  public Long getGameId() {
    return gameId;
  }

  public void setGameId(Long gameId) {
    this.gameId = gameId;
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

  public List<MovieQuestion> getQuestions() {
    return questions;
  }

  public void setQuestions(List<MovieQuestion> questions) {
    this.questions = questions;
  }

  public int getTotalQuestions() {
    return totalQuestions;
  }

  public void setTotalQuestions(int totalQuestions) {
    this.totalQuestions = totalQuestions;
  }

  public static class MovieQuestion {
    private Long itemId;
    private String posterUrl;
    private String movieTitle;
    private String releaseDate;
    private List<String> choices;

    public MovieQuestion() {
    }

    public Long getItemId() {
      return itemId;
    }

    public void setItemId(Long itemId) {
      this.itemId = itemId;
    }

    public String getPosterUrl() {
      return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
      this.posterUrl = posterUrl;
    }

    public String getMovieTitle() {
      return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
      this.movieTitle = movieTitle;
    }

    public String getReleaseDate() {
      return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
      this.releaseDate = releaseDate;
    }

    public List<String> getChoices() {
      return choices;
    }

    public void setChoices(List<String> choices) {
      this.choices = choices;
    }
  }
}
