package org.techhive.gameservice.dto;

import java.util.List;

public class PlaceQuizResponse {

  private Long correctPlaceId;
  private String correctName;
  private Double latitude;
  private Double longitude;
  private String hint;
  private List<String> choices;

  public PlaceQuizResponse() {
  }

  public Long getCorrectPlaceId() {
    return correctPlaceId;
  }

  public void setCorrectPlaceId(Long correctPlaceId) {
    this.correctPlaceId = correctPlaceId;
  }

  public String getCorrectName() {
    return correctName;
  }

  public void setCorrectName(String correctName) {
    this.correctName = correctName;
  }

  public Double getLatitude() {
    return latitude;
  }

  public void setLatitude(Double latitude) {
    this.latitude = latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public void setLongitude(Double longitude) {
    this.longitude = longitude;
  }

  public String getHint() {
    return hint;
  }

  public void setHint(String hint) {
    this.hint = hint;
  }

  public List<String> getChoices() {
    return choices;
  }

  public void setChoices(List<String> choices) {
    this.choices = choices;
  }
}
