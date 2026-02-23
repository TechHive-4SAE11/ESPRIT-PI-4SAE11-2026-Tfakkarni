package org.techhive.gameservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.techhive.gameservice.dto.TagRequest;
import org.techhive.gameservice.dto.TagResponse;
import org.techhive.gameservice.entity.MemoryTag;
import org.techhive.gameservice.repository.MemoryTagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryTagService {

  private final MemoryTagRepository tagRepository;

  private static final String[][] DEFAULT_TAGS = {
      { "Family", "#3b82f6" },
      { "Childhood", "#f59e0b" },
      { "Travel", "#10b981" },
      { "Work", "#8b5cf6" },
      { "Friends", "#ec4899" },
      { "Daily Life", "#6b7280" },
      { "Health", "#ef4444" },
      { "Hobbies", "#06b6d4" }
  };

  public List<TagResponse> getTagsForPatient(String keycloakId) {
    List<MemoryTag> tags = tagRepository.findByPatientKeycloakId(keycloakId);
    if (tags.isEmpty()) {
      log.info("No tags found for patient {}, seeding defaults", keycloakId);
      tags = seedDefaults(keycloakId);
    }
    return tags.stream().map(this::toResponse).collect(Collectors.toList());
  }

  public List<TagResponse> searchTags(String keycloakId, String query) {
    return tagRepository.findByPatientKeycloakIdAndNameContainingIgnoreCase(keycloakId, query)
        .stream().map(this::toResponse).collect(Collectors.toList());
  }

  @Transactional
  public TagResponse createTag(String keycloakId, TagRequest request) {
    if (tagRepository.existsByPatientKeycloakIdAndNameIgnoreCase(keycloakId, request.getName())) {
      throw new IllegalArgumentException("Tag '" + request.getName() + "' already exists");
    }
    MemoryTag tag = new MemoryTag(keycloakId, request.getName(), request.getColor());
    tag = tagRepository.save(tag);
    log.info("Created tag '{}' for patient {}", tag.getName(), keycloakId);
    return toResponse(tag);
  }

  @Transactional
  public TagResponse updateTag(Long tagId, TagRequest request) {
    MemoryTag tag = tagRepository.findById(tagId)
        .orElseThrow(() -> new IllegalArgumentException("Tag not found: " + tagId));
    tag.setName(request.getName());
    tag.setColor(request.getColor());
    tag = tagRepository.save(tag);
    return toResponse(tag);
  }

  @Transactional
  public void deleteTag(Long tagId) {
    tagRepository.deleteById(tagId);
    log.info("Deleted tag {}", tagId);
  }

  private List<MemoryTag> seedDefaults(String keycloakId) {
    List<MemoryTag> tags = new java.util.ArrayList<>();
    for (String[] def : DEFAULT_TAGS) {
      tags.add(new MemoryTag(keycloakId, def[0], def[1]));
    }
    return tagRepository.saveAll(tags);
  }

  private TagResponse toResponse(MemoryTag tag) {
    return new TagResponse(tag.getId(), tag.getName(), tag.getColor());
  }
}
