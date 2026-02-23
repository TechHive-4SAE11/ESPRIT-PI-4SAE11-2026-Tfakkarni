package org.techhive.gameservice.dto;

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
public class CreatePhotoRequest {

  @NotBlank(message = "Photo name is required")
  @Size(max = 20, message = "Photo name must be at most 20 characters")
  @Pattern(regexp = "^[a-zA-Z0-9 ]+$", message = "Photo name can only contain letters, numbers, and spaces")
  private String name;

  @NotBlank(message = "Image data is required")
  private String imageBase64;

  @NotBlank(message = "Content type is required")
  private String contentType;

  private List<Long> tagIds;
}
