package org.techhive.gameservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.techhive.gameservice.dto.*;
import org.techhive.gameservice.service.PersonalQuestionService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PersonalQuestionControllerDirectTest {

  private final PersonalQuestionService personalQuestionService = mock(PersonalQuestionService.class);
  private final PersonalQuestionController controller = new PersonalQuestionController(personalQuestionService);

  @Test
  void happyPathEndpointsDelegateToService() {
    PersonalQuestionGameResponse response = new PersonalQuestionGameResponse(1L, "patient-1", "Family", "Questions", 2, LocalDateTime.now());
    PersonalQuestionGameDetailResponse detail = new PersonalQuestionGameDetailResponse(1L, "patient-1", "Family", "Questions", List.of(), LocalDateTime.now());
    PersonalQuestionPlayData playData = new PersonalQuestionPlayData();
    PersonalQuestionAttemptResponse attempt = new PersonalQuestionAttemptResponse();
    CreatePersonalQuestionGameRequest createRequest = new CreatePersonalQuestionGameRequest();
    EditPersonalQuestionGameRequest editRequest = new EditPersonalQuestionGameRequest();
    PersonalQuestionSubmitRequest submitRequest = new PersonalQuestionSubmitRequest();

    when(personalQuestionService.createGame("patient-1", createRequest)).thenReturn(response);
    when(personalQuestionService.getGamesByPatient("patient-1")).thenReturn(List.of(response));
    when(personalQuestionService.getGameDetail(1L)).thenReturn(detail);
    when(personalQuestionService.editGame(1L, editRequest)).thenReturn(response);
    when(personalQuestionService.getGameForPlay(1L)).thenReturn(playData);
    when(personalQuestionService.submitResults(1L, "player-1", submitRequest)).thenReturn(attempt);

    assertEquals(201, controller.createGame("patient-1", createRequest).getStatusCode().value());
    assertEquals(List.of(response), controller.getGamesByPatient("patient-1").getBody());
    assertSame(detail, controller.getGameDetail(1L).getBody());
    assertSame(response, controller.editGame(1L, editRequest).getBody());
    assertEquals(200, controller.deleteGame(1L).getStatusCode().value());
    assertSame(playData, controller.getGameForPlay(1L).getBody());
    assertSame(attempt, controller.submitResults(1L, "player-1", submitRequest).getBody());
    verify(personalQuestionService).deleteGame(1L);
  }

  @Test
  void errorPathsReturnExpectedStatuses() {
    CreatePersonalQuestionGameRequest createRequest = new CreatePersonalQuestionGameRequest();
    EditPersonalQuestionGameRequest editRequest = new EditPersonalQuestionGameRequest();
    PersonalQuestionSubmitRequest submitRequest = new PersonalQuestionSubmitRequest();
    when(personalQuestionService.createGame("patient-1", createRequest)).thenThrow(new RuntimeException("create failed"));
    when(personalQuestionService.getGameDetail(9L)).thenThrow(new RuntimeException("missing detail"));
    when(personalQuestionService.editGame(9L, editRequest)).thenThrow(new RuntimeException("edit failed"));
    doThrow(new RuntimeException("delete missing")).when(personalQuestionService).deleteGame(9L);
    when(personalQuestionService.getGameForPlay(9L)).thenThrow(new RuntimeException("play missing"));
    when(personalQuestionService.submitResults(9L, "player-1", submitRequest)).thenThrow(new RuntimeException("submit failed"));

    ResponseEntity<?> createFailed = controller.createGame("patient-1", createRequest);
    ResponseEntity<?> detailFailed = controller.getGameDetail(9L);
    ResponseEntity<?> editFailed = controller.editGame(9L, editRequest);
    ResponseEntity<?> deleteFailed = controller.deleteGame(9L);
    ResponseEntity<?> playFailed = controller.getGameForPlay(9L);
    ResponseEntity<?> submitFailed = controller.submitResults(9L, "player-1", submitRequest);

    assertEquals(500, createFailed.getStatusCode().value());
    assertBodyContains(createFailed, "create failed");
    assertEquals(404, detailFailed.getStatusCode().value());
    assertBodyContains(detailFailed, "missing detail");
    assertEquals(500, editFailed.getStatusCode().value());
    assertBodyContains(editFailed, "edit failed");
    assertEquals(404, deleteFailed.getStatusCode().value());
    assertBodyContains(deleteFailed, "delete missing");
    assertEquals(404, playFailed.getStatusCode().value());
    assertBodyContains(playFailed, "play missing");
    assertEquals(500, submitFailed.getStatusCode().value());
    assertBodyContains(submitFailed, "submit failed");
  }

  private void assertBodyContains(ResponseEntity<?> response, String expected) {
    assertTrue(response.getBody() instanceof Map<?, ?>);
    assertTrue(((Map<?, ?>) response.getBody()).values().stream()
        .map(String::valueOf)
        .anyMatch(value -> value.contains(expected)),
        "Expected response body to contain: " + expected + " but was " + response.getBody());
  }
}
