package org.techhive.gameservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagRequest {

  @NotBlank(message = "Tag name is required")
  @Size(min = 3, max = 10, message = "Tag name must be between 3 and 10 characters")
  @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Tag name must contain only letters and numbers")
  private String name;

  @NotBlank(message = "Tag color is required")
  private String color;
}
