package org.techhive.gameservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateGameRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 20, message = "Title must be at most 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9 ]+$", message = "Title can only contain letters, numbers, and spaces")
    private String title;

    @Size(max = 100, message = "Description must be at most 100 characters")
    private String description;

    public CreateGameRequest() {
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
}
