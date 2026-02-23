package org.techhive.gameservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EditGameRequest {

  @NotBlank(message = "Title is required")
  @Size(min = 3, max = 20, message = "Title must be between 3 and 20 characters")
  @Pattern(regexp = "^[a-zA-Z0-9 ]+$", message = "Title can only contain letters, numbers, and spaces")
  private String title;

  @Size(max = 100, message = "Description must be at most 100 characters")
  private String description;

  @Valid
  private List<EditImageEntry> images;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class EditImageEntry {
    /** Null for new images, non-null for existing (to keep or rename) */
    private Long id;

    @NotBlank(message = "Image name is required")
    @Size(max = 20, message = "Image name must be at most 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9 ]+$", message = "Image name can only contain letters, numbers, and spaces")
    private String name;

    /** Required only for NEW images (id == null) */
    private String imageBase64;
    /** Required only for NEW images (id == null) */
    private String contentType;
  }
}
