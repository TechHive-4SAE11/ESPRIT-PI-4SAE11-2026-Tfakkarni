package org.techhive.gameservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.techhive.gameservice.dto.*;
import org.techhive.gameservice.service.GameService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameControllerDirectTest {

  private final GameService gameService = mock(GameService.class);
  private final GameController controller = new GameController(gameService);

  @Test
  void happyPathEndpointsDelegateToService() {
    GameResponse gameResponse = new GameResponse(1L, "patient-1", "Family", "Faces", 2, LocalDateTime.now());
    GameDetailResponse detailResponse = new GameDetailResponse();
    detailResponse.setId(1L);
    detailResponse.setTitle("Family");
    detailResponse.setDescription("Faces");
    detailResponse.setImages(List.of(new GameDetailResponse.ImageDetail(11L, "Nour", "AQID", "image/png", 0)));

    CreateGameRequest createRequest = new CreateGameRequest();
    createRequest.setTitle("Family");
    createRequest.setDescription("Faces");
    when(gameService.createGame("patient-1", createRequest)).thenReturn(gameResponse);
    when(gameService.addImages(eq(1L), anyList())).thenReturn(detailResponse);
    when(gameService.getGamesByPatient("patient-1")).thenReturn(List.of(gameResponse));
    when(gameService.getGameDetail(1L)).thenReturn(detailResponse);
    when(gameService.editGame(eq(1L), any(EditGameRequest.class))).thenReturn(detailResponse);
    when(gameService.getAllGames()).thenReturn(List.of(gameResponse));

    GameImageUpload upload = new GameImageUpload();
    upload.setName("Nour");
    upload.setImageBase64("AQID");
    upload.setContentType("image/png");

    ResponseEntity<?> created = controller.createGame("patient-1", createRequest);
    ResponseEntity<?> images = controller.addImages(1L, List.of(upload));
    ResponseEntity<List<GameResponse>> byPatient = controller.getGamesByPatient("patient-1");
    ResponseEntity<?> detail = controller.getGameDetail(1L);
    ResponseEntity<?> edited = controller.editGame(1L, new EditGameRequest());
    ResponseEntity<List<GameResponse>> allGames = controller.getAllGames();

    assertEquals(201, created.getStatusCode().value());
    assertSame(gameResponse, created.getBody());
    assertEquals(200, images.getStatusCode().value());
    assertSame(detailResponse, images.getBody());
    assertEquals(List.of(gameResponse), byPatient.getBody());
    assertSame(detailResponse, detail.getBody());
    assertSame(detailResponse, edited.getBody());
    assertEquals(List.of(gameResponse), allGames.getBody());
  }

  @Test
  void errorPathsReturnExpectedStatusesAndBodies() {
    CreateGameRequest createRequest = new CreateGameRequest();
    createRequest.setTitle("Family");
    when(gameService.createGame("patient-1", createRequest)).thenThrow(new RuntimeException("db down"));
    when(gameService.addImages(eq(9L), anyList())).thenThrow(new RuntimeException("bad image"));
    when(gameService.getGameDetail(9L)).thenThrow(new RuntimeException("missing"));
    when(gameService.editGame(eq(9L), any(EditGameRequest.class))).thenThrow(new RuntimeException("edit failed"));
    doThrow(new RuntimeException("missing delete")).when(gameService).deleteGame(9L);

    ResponseEntity<?> createFailed = controller.createGame("patient-1", createRequest);
    ResponseEntity<?> addFailed = controller.addImages(9L, List.of());
    ResponseEntity<?> detailFailed = controller.getGameDetail(9L);
    ResponseEntity<?> editFailed = controller.editGame(9L, new EditGameRequest());
    ResponseEntity<?> deleteFailed = controller.deleteGame(9L);

    assertEquals(500, createFailed.getStatusCode().value());
    assertBodyContains(createFailed, "db down");
    assertEquals(500, addFailed.getStatusCode().value());
    assertBodyContains(addFailed, "bad image");
    assertEquals(404, detailFailed.getStatusCode().value());
    assertBodyContains(detailFailed, "missing");
    assertEquals(500, editFailed.getStatusCode().value());
    assertBodyContains(editFailed, "edit failed");
    assertEquals(404, deleteFailed.getStatusCode().value());
    assertBodyContains(deleteFailed, "missing delete");
  }

  @Test
  void deleteSuccessReturnsMessage() {
    ResponseEntity<?> response = controller.deleteGame(1L);

    assertEquals(200, response.getStatusCode().value());
    assertBodyContains(response, "Game deleted successfully");
    verify(gameService).deleteGame(1L);
  }

  private void assertBodyContains(ResponseEntity<?> response, String expected) {
    assertTrue(response.getBody() instanceof Map<?, ?>);
    assertTrue(((Map<?, ?>) response.getBody()).values().stream()
        .map(String::valueOf)
        .anyMatch(value -> value.contains(expected)),
        "Expected response body to contain: " + expected + " but was " + response.getBody());
  }
}
