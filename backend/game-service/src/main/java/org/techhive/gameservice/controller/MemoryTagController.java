package org.techhive.gameservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.techhive.gameservice.dto.TagRequest;
import org.techhive.gameservice.dto.TagResponse;
import org.techhive.gameservice.service.MemoryTagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/games/tags")
@RequiredArgsConstructor
public class MemoryTagController {

  private final MemoryTagService tagService;

  @GetMapping("/{keycloakId}")
  public ResponseEntity<List<TagResponse>> getTags(@PathVariable String keycloakId) {
    return ResponseEntity.ok(tagService.getTagsForPatient(keycloakId));
  }

  @GetMapping("/{keycloakId}/search")
  public ResponseEntity<List<TagResponse>> searchTags(
      @PathVariable String keycloakId,
      @RequestParam String query) {
    return ResponseEntity.ok(tagService.searchTags(keycloakId, query));
  }

  @PostMapping("/{keycloakId}")
  public ResponseEntity<TagResponse> createTag(
      @PathVariable String keycloakId,
      @RequestBody TagRequest request) {
    return ResponseEntity.ok(tagService.createTag(keycloakId, request));
  }

  @PutMapping("/{tagId}")
  public ResponseEntity<TagResponse> updateTag(
      @PathVariable Long tagId,
      @RequestBody TagRequest request) {
    return ResponseEntity.ok(tagService.updateTag(tagId, request));
  }

  @DeleteMapping("/{tagId}")
  public ResponseEntity<Void> deleteTag(@PathVariable Long tagId) {
    tagService.deleteTag(tagId);
    return ResponseEntity.noContent().build();
  }
}
