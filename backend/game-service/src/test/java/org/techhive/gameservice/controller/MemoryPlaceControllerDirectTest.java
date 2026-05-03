package org.techhive.gameservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.techhive.gameservice.dto.CreatePlaceRequest;
import org.techhive.gameservice.dto.PlaceQuizResponse;
import org.techhive.gameservice.dto.PlaceResponse;
import org.techhive.gameservice.service.MemoryPlaceService;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MemoryPlaceControllerDirectTest {

  private final MemoryPlaceService memoryPlaceService = mock(MemoryPlaceService.class);
  private final MemoryPlaceController controller = new MemoryPlaceController(memoryPlaceService);

  @Test
  void happyPathEndpointsDelegateToService() {
    CreatePlaceRequest request = new CreatePlaceRequest();
    PlaceResponse place = new PlaceResponse(1L, "Bardo", 36.8, 10.1, "museum", "2026-05-03");
    PlaceQuizResponse quiz = new PlaceQuizResponse();
    quiz.setCorrectPlaceId(1L);
    quiz.setCorrectName("Bardo");
    quiz.setChoices(List.of("Bardo", "Carthage"));

    when(memoryPlaceService.createPlace("patient-1", request)).thenReturn(place);
    when(memoryPlaceService.getPlacesByPatient("patient-1")).thenReturn(List.of(place));
    when(memoryPlaceService.generateQuiz("patient-1")).thenReturn(quiz);
    when(memoryPlaceService.editPlace(1L, request)).thenReturn(place);

    assertEquals(201, controller.createPlace("patient-1", request).getStatusCode().value());
    assertEquals(List.of(place), controller.getPlacesByPatient("patient-1").getBody());
    assertSame(quiz, controller.getPlaceQuiz("patient-1").getBody());
    assertSame(place, controller.editPlace(1L, request).getBody());
    assertEquals(204, controller.deletePlace(1L).getStatusCode().value());
    verify(memoryPlaceService).deletePlace(1L);
  }

  @Test
  void errorPathsReturnExpectedStatuses() {
    CreatePlaceRequest request = new CreatePlaceRequest();
    when(memoryPlaceService.createPlace("patient-1", request)).thenThrow(new RuntimeException("create failed"));
    when(memoryPlaceService.generateQuiz("patient-1")).thenThrow(new RuntimeException("not enough places"));
    when(memoryPlaceService.editPlace(9L, request)).thenThrow(new RuntimeException("edit failed"));
    doThrow(new RuntimeException("delete failed")).when(memoryPlaceService).deletePlace(9L);

    ResponseEntity<?> createFailed = controller.createPlace("patient-1", request);
    ResponseEntity<?> quizFailed = controller.getPlaceQuiz("patient-1");
    ResponseEntity<?> editFailed = controller.editPlace(9L, request);
    ResponseEntity<?> deleteFailed = controller.deletePlace(9L);

    assertEquals(500, createFailed.getStatusCode().value());
    assertBodyContains(createFailed, "create failed");
    assertEquals(400, quizFailed.getStatusCode().value());
    assertBodyContains(quizFailed, "not enough places");
    assertEquals(500, editFailed.getStatusCode().value());
    assertBodyContains(editFailed, "edit failed");
    assertEquals(500, deleteFailed.getStatusCode().value());
    assertBodyContains(deleteFailed, "delete failed");
  }

  private void assertBodyContains(ResponseEntity<?> response, String expected) {
    assertTrue(response.getBody() instanceof Map<?, ?>);
    assertTrue(((Map<?, ?>) response.getBody()).values().stream()
        .map(String::valueOf)
        .anyMatch(value -> value.contains(expected)),
        "Expected response body to contain: " + expected + " but was " + response.getBody());
  }
}
