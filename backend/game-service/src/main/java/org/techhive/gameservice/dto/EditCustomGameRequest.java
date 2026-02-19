package org.techhive.gameservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.techhive.gameservice.entity.DataPointType;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EditCustomGameRequest {

  @NotBlank(message = "Title is required")
  @Size(max = 20, message = "Title must be at most 20 characters")
  @Pattern(regexp = "^[a-zA-Z0-9 ]+$", message = "Title can only contain letters, numbers, and spaces")
  private String title;

  @Size(max = 100, message = "Description must be at most 100 characters")
  private String description;

  @NotEmpty(message = "At least one item is required")
  @Valid
  private List<GameItemEntry> items;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GameItemEntry {
    @NotNull(message = "Data type is required")
    private DataPointType dataType;

    @NotNull(message = "Data point ID is required")
    private Long dataPointId;
  }
}
