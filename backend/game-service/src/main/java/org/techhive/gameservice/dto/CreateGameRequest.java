package org.techhive.gameservice.dto;

public class CreateGameRequest {
    private String title;
    private String description;

    public CreateGameRequest() {
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
