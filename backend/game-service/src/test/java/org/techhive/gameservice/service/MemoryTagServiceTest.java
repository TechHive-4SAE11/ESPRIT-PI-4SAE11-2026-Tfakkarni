package org.techhive.gameservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.gameservice.dto.TagRequest;
import org.techhive.gameservice.dto.TagResponse;
import org.techhive.gameservice.entity.MemoryTag;
import org.techhive.gameservice.repository.MemoryTagRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemoryTagServiceTest {

    @Mock
    private MemoryTagRepository tagRepository;

    @InjectMocks
    private MemoryTagService memoryTagService;

    @Test
    void getTagsForPatientSeedsDefaultTagsWhenPatientHasNone() {
        when(tagRepository.findByPatientKeycloakId("patient-1")).thenReturn(List.of());
        when(tagRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<MemoryTag> tags = invocation.getArgument(0);
            long id = 1L;
            for (MemoryTag tag : tags) {
                tag.setId(id++);
            }
            return tags;
        });

        List<TagResponse> responses = memoryTagService.getTagsForPatient("patient-1");

        assertEquals(8, responses.size());
        assertEquals("Family", responses.get(0).getName());
        verify(tagRepository).saveAll(argThat(tags -> {
            int count = 0;
            for (MemoryTag ignored : tags) {
                count++;
            }
            return count == 8;
        }));
    }

    @Test
    void createTagRejectsDuplicateNameForPatient() {
        when(tagRepository.existsByPatientKeycloakIdAndNameIgnoreCase("patient-1", "Family"))
                .thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> memoryTagService.createTag("patient-1", new TagRequest("Family", "#3b82f6")));

        assertTrue(ex.getMessage().contains("already exists"));
        verify(tagRepository, never()).save(any());
    }

    @Test
    void createTagPersistsAndMapsResponse() {
        when(tagRepository.existsByPatientKeycloakIdAndNameIgnoreCase("patient-1", "Travel"))
                .thenReturn(false);
        when(tagRepository.save(any(MemoryTag.class))).thenAnswer(invocation -> {
            MemoryTag tag = invocation.getArgument(0);
            tag.setId(12L);
            return tag;
        });

        TagResponse response = memoryTagService.createTag("patient-1", new TagRequest("Travel", "#10b981"));

        assertEquals(12L, response.getId());
        assertEquals("Travel", response.getName());
        assertEquals("#10b981", response.getColor());
    }

    @Test
    void updateTagThrowsWhenMissing() {
        when(tagRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> memoryTagService.updateTag(99L, new TagRequest("Health", "#ef4444")));

        assertEquals("Tag not found: 99", ex.getMessage());
    }

    @Test
    void searchTagsMapsRepositoryResults() {
        MemoryTag tag = new MemoryTag("patient-1", "Friends", "#ec4899");
        tag.setId(7L);
        when(tagRepository.findByPatientKeycloakIdAndNameContainingIgnoreCase("patient-1", "fri"))
                .thenReturn(List.of(tag));

        List<TagResponse> responses = memoryTagService.searchTags("patient-1", "fri");

        assertEquals(1, responses.size());
        assertEquals("Friends", responses.get(0).getName());
    }
}
