package org.techhive.gameservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePhotoRequest {
  private String name;
  private String imageBase64;
  private String contentType;
  private List<Long> tagIds;
}
