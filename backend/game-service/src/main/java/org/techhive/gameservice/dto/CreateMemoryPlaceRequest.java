package org.techhive.gameservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateMemoryPlaceRequest {

  @NotBlank(message = "Place name is required")
  @Size(max = 20, message = "Place name must be at most 20 characters")
  @Pattern(regexp = "^[a-zA-Z0-9 ]+$", message = "Place name can only contain letters, numbers, and spaces")
  private String name;

  @NotNull(message = "Latitude is required")
  private Double latitude;

  @NotNull(message = "Longitude is required")
  private Double longitude;

  @Size(max = 100, message = "Hint must be at most 100 characters")
  private String hint;

  private List<Long> tagIds;
}
