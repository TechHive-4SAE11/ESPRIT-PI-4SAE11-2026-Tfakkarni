package org.techhive.gameservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EditGameRequest {
  private String title;
  private String description;
  private List<EditImageEntry> images;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class EditImageEntry {
    /** Null for new images, non-null for existing (to keep or rename) */
    private Long id;
    private String name;
    /** Required only for NEW images (id == null) */
    private String imageBase64;
    /** Required only for NEW images (id == null) */
    private String contentType;
  }
}
