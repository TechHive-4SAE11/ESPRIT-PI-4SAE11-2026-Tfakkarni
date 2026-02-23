package org.techhive.gameservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.gameservice.dto.*;
import org.techhive.gameservice.service.PersonalQuestionService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/games/personal")
@RequiredArgsConstructor
public class PersonalQuestionController {

  private final PersonalQuestionService personalQuestionService;

  // ─── CRUD ─────────────────────────────────────────────────

  @PostMapping
  public ResponseEntity<?> createGame(
      @RequestHeader("X-User-Id") String patientKeycloakId,
      @Valid @RequestBody CreatePersonalQuestionGameRequest request) {
    try {
      PersonalQuestionGameResponse game = personalQuestionService.createGame(patientKeycloakId, request);
      return ResponseEntity.status(HttpStatus.CREATED).body(game);
    } catch (Exception e) {
      log.error("Error creating personal question game", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Failed to create personal question game: " + e.getMessage()));
    }
  }

  @GetMapping("/patient/{keycloakId}")
  public ResponseEntity<List<PersonalQuestionGameResponse>> getGamesByPatient(@PathVariable String keycloakId) {
    return ResponseEntity.ok(personalQuestionService.getGamesByPatient(keycloakId));
  }

  @GetMapping("/{gameId}")
  public ResponseEntity<?> getGameDetail(@PathVariable Long gameId) {
    try {
      return ResponseEntity.ok(personalQuestionService.getGameDetail(gameId));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", e.getMessage()));
    }
  }

  @PutMapping("/{gameId}")
  public ResponseEntity<?> editGame(
      @PathVariable Long gameId,
      @Valid @RequestBody EditPersonalQuestionGameRequest request) {
    try {
      PersonalQuestionGameResponse game = personalQuestionService.editGame(gameId, request);
      return ResponseEntity.ok(game);
    } catch (Exception e) {
      log.error("Error editing personal question game {}", gameId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Failed to edit personal question game: " + e.getMessage()));
    }
  }

  @DeleteMapping("/{gameId}")
  public ResponseEntity<?> deleteGame(@PathVariable Long gameId) {
    try {
      personalQuestionService.deleteGame(gameId);
      return ResponseEntity.ok(Map.of("message", "Personal question game deleted successfully"));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", e.getMessage()));
    }
  }

  // ─── Play ─────────────────────────────────────────────────

  @GetMapping("/play/{gameId}")
  public ResponseEntity<?> getGameForPlay(@PathVariable Long gameId) {
    try {
      return ResponseEntity.ok(personalQuestionService.getGameForPlay(gameId));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", e.getMessage()));
    }
  }

  @PostMapping("/play/{gameId}/submit")
  public ResponseEntity<?> submitResults(
      @PathVariable Long gameId,
      @RequestHeader("X-User-Id") String playerKeycloakId,
      @RequestBody PersonalQuestionSubmitRequest request) {
    try {
      PersonalQuestionAttemptResponse response = personalQuestionService.submitResults(
          gameId, playerKeycloakId, request);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error submitting personal question results for game {}", gameId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Failed to submit results: " + e.getMessage()));
    }
  }
}
