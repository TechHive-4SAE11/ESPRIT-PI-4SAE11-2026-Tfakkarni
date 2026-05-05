package org.techhive.gameservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.techhive.gameservice.dto.*;
import org.techhive.gameservice.service.MovieGameService;
import org.techhive.gameservice.service.TmdbService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MovieGameControllerDirectTest {

  private final MovieGameService movieGameService = mock(MovieGameService.class);
  private final TmdbService tmdbService = mock(TmdbService.class);
  private final MovieGameController controller = new MovieGameController(movieGameService, tmdbService);

  @Test
  void happyPathEndpointsDelegateToServices() {
    MovieGameResponse response = new MovieGameResponse(1L, "patient-1", "Movies", "Actors", 2, LocalDateTime.now());
    MovieGameDetailResponse detail = new MovieGameDetailResponse(1L, "patient-1", "Movies", "Actors", List.of(), LocalDateTime.now());
    MovieGamePlayData playData = new MovieGamePlayData();
    MovieGameAttemptResponse attempt = new MovieGameAttemptResponse();
    attempt.setScore(1);
    attempt.setTotalQuestions(2);
    CreateMovieGameRequest createRequest = new CreateMovieGameRequest();
    EditMovieGameRequest editRequest = new EditMovieGameRequest();
    MovieGameSubmitRequest submitRequest = new MovieGameSubmitRequest();

    when(tmdbService.searchMovies("star")).thenReturn(List.of(Map.of("title", "Star")));
    when(movieGameService.createMovieGame("patient-1", createRequest)).thenReturn(response);
    when(movieGameService.getGamesByPatient("patient-1")).thenReturn(List.of(response));
    when(movieGameService.getGameDetail(1L)).thenReturn(detail);
    when(movieGameService.editGame(1L, editRequest)).thenReturn(response);
    when(movieGameService.getGameForPlay(1L)).thenReturn(playData);
    when(movieGameService.submitAnswers(1L, "player-1", submitRequest)).thenReturn(attempt);

    assertEquals(List.of(Map.of("title", "Star")), controller.searchMovies("star").getBody());
    assertEquals(201, controller.createMovieGame("patient-1", createRequest).getStatusCode().value());
    assertEquals(List.of(response), controller.getGamesByPatient("patient-1").getBody());
    assertSame(detail, controller.getGameDetail(1L).getBody());
    assertSame(response, controller.editGame(1L, editRequest).getBody());
    assertEquals(200, controller.deleteGame(1L).getStatusCode().value());
    assertSame(playData, controller.getGameForPlay(1L).getBody());
    assertSame(attempt, controller.submitAnswers(1L, "player-1", submitRequest).getBody());
    verify(movieGameService).deleteGame(1L);
  }

  @Test
  void errorPathsReturnExpectedStatuses() {
    CreateMovieGameRequest createRequest = new CreateMovieGameRequest();
    EditMovieGameRequest editRequest = new EditMovieGameRequest();
    MovieGameSubmitRequest submitRequest = new MovieGameSubmitRequest();
    when(movieGameService.createMovieGame("patient-1", createRequest)).thenThrow(new RuntimeException("create failed"));
    when(movieGameService.getGameDetail(9L)).thenThrow(new RuntimeException("missing detail"));
    when(movieGameService.editGame(9L, editRequest)).thenThrow(new RuntimeException("edit failed"));
    doThrow(new RuntimeException("delete missing")).when(movieGameService).deleteGame(9L);
    when(movieGameService.getGameForPlay(9L)).thenThrow(new RuntimeException("play missing"));
    when(movieGameService.submitAnswers(9L, "player-1", submitRequest)).thenThrow(new RuntimeException("submit failed"));

    ResponseEntity<?> createFailed = controller.createMovieGame("patient-1", createRequest);
    ResponseEntity<?> detailFailed = controller.getGameDetail(9L);
    ResponseEntity<?> editFailed = controller.editGame(9L, editRequest);
    ResponseEntity<?> deleteFailed = controller.deleteGame(9L);
    ResponseEntity<?> playFailed = controller.getGameForPlay(9L);
    ResponseEntity<?> submitFailed = controller.submitAnswers(9L, "player-1", submitRequest);

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
