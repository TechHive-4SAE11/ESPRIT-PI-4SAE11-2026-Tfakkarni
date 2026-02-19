package org.techhive.gameservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class GameImageUpload {

    @NotBlank(message = "Image name is required")
    @Size(max = 20, message = "Image name must be at most 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9 ]+$", message = "Image name can only contain letters, numbers, and spaces")
    private String name;

    @NotBlank(message = "Image data is required")
    private String imageBase64;

    @NotBlank(message = "Content type is required")
    private String contentType;

    public GameImageUpload() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
