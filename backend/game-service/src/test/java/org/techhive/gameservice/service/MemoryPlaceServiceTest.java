package org.techhive.gameservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.gameservice.dto.CreatePlaceRequest;
import org.techhive.gameservice.dto.PlaceQuizResponse;
import org.techhive.gameservice.dto.PlaceResponse;
import org.techhive.gameservice.entity.MemoryPlace;
import org.techhive.gameservice.repository.MemoryPlaceRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemoryPlaceServiceTest {

    @Mock
    private MemoryPlaceRepository memoryPlaceRepository;

    @InjectMocks
    private MemoryPlaceService memoryPlaceService;

    @Test
    void createPlacePersistsPatientPlaceAndMapsResponse() {
        CreatePlaceRequest request = placeRequest("Home", 36.8065, 10.1815, "near the blue door");
        when(memoryPlaceRepository.save(any(MemoryPlace.class))).thenAnswer(invocation -> {
            MemoryPlace place = invocation.getArgument(0);
            place.setId(4L);
            place.setCreatedAt(LocalDateTime.parse("2026-01-01T10:15:30"));
            return place;
        });

        PlaceResponse response = memoryPlaceService.createPlace("patient-1", request);

        assertEquals(4L, response.getId());
        assertEquals("Home", response.getName());
        assertEquals(36.8065, response.getLatitude());
        assertEquals("near the blue door", response.getHint());
        verify(memoryPlaceRepository).save(argThat(place ->
                "patient-1".equals(place.getPatientKeycloakId()) && "Home".equals(place.getName())));
    }

    @Test
    void editPlaceThrowsWhenPlaceDoesNotExist() {
        when(memoryPlaceRepository.findById(44L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> memoryPlaceService.editPlace(44L, placeRequest("Clinic", 36.8, 10.1, "hint")));

        assertEquals("Place not found: 44", ex.getMessage());
        verify(memoryPlaceRepository, never()).save(any());
    }

    @Test
    void editPlaceUpdatesAllEditableFields() {
        MemoryPlace existing = new MemoryPlace("Old", 1.0, 2.0, "old", "patient-1");
        existing.setId(5L);
        when(memoryPlaceRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(memoryPlaceRepository.save(any(MemoryPlace.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlaceResponse response = memoryPlaceService.editPlace(5L, placeRequest("Park", 35.0, 9.0, "fountain"));

        assertEquals("Park", response.getName());
        assertEquals(35.0, response.getLatitude());
        assertEquals(9.0, response.getLongitude());
        assertEquals("fountain", response.getHint());
    }

    @Test
    void generateQuizRequiresAtLeastThreePlaces() {
        when(memoryPlaceRepository.countByPatientKeycloakId("patient-1")).thenReturn(2L);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> memoryPlaceService.generateQuiz("patient-1"));

        assertTrue(ex.getMessage().contains("Need at least 3 saved places"));
    }

    @Test
    void generateQuizUsesRandomPlacesAndBuildsChoices() {
        when(memoryPlaceRepository.countByPatientKeycloakId("patient-1")).thenReturn(3L);
        MemoryPlace correct = place(1L, "Home", 36.8, 10.1, "door");
        MemoryPlace wrongOne = place(2L, "Park", 36.7, 10.2, "trees");
        MemoryPlace wrongTwo = place(3L, "Clinic", 36.9, 10.3, "doctor");
        when(memoryPlaceRepository.findRandomByPatientKeycloakId("patient-1", 3))
                .thenReturn(List.of(correct, wrongOne, wrongTwo));

        PlaceQuizResponse quiz = memoryPlaceService.generateQuiz("patient-1");

        assertEquals(1L, quiz.getCorrectPlaceId());
        assertEquals("Home", quiz.getCorrectName());
        assertEquals(3, quiz.getChoices().size());
        assertTrue(quiz.getChoices().containsAll(List.of("Home", "Park", "Clinic")));
    }

    @Test
    void deletePlaceDelegatesToRepository() {
        memoryPlaceService.deletePlace(8L);
        verify(memoryPlaceRepository).deleteById(8L);
    }

    private static CreatePlaceRequest placeRequest(String name, Double latitude, Double longitude, String hint) {
        CreatePlaceRequest request = new CreatePlaceRequest();
        request.setName(name);
        request.setLatitude(latitude);
        request.setLongitude(longitude);
        request.setHint(hint);
        return request;
    }

    private static MemoryPlace place(Long id, String name, Double latitude, Double longitude, String hint) {
        MemoryPlace place = new MemoryPlace(name, latitude, longitude, hint, "patient-1");
        place.setId(id);
        return place;
    }
}
