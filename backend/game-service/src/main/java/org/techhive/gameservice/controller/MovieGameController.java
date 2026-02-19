package org.techhive.gameservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.gameservice.dto.*;
import org.techhive.gameservice.service.MovieGameService;
import org.techhive.gameservice.service.TmdbService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/games/movies")
@RequiredArgsConstructor
public class MovieGameController {

  private final MovieGameService movieGameService;
  private final TmdbService tmdbService;

  // ─── TMDB Search Proxy ────────────────────────────────────

  @GetMapping("/tmdb/search")
  public ResponseEntity<List<Map<String, Object>>> searchMovies(@RequestParam String query) {
    return ResponseEntity.ok(tmdbService.searchMovies(query));
  }

  // ─── Movie Game CRUD ──────────────────────────────────────

  @PostMapping
  public ResponseEntity<?> createMovieGame(
      @RequestHeader("X-User-Id") String patientKeycloakId,
      @Valid @RequestBody CreateMovieGameRequest request) {
    try {
      MovieGameResponse game = movieGameService.createMovieGame(patientKeycloakId, request);
      return ResponseEntity.status(HttpStatus.CREATED).body(game);
    } catch (Exception e) {
      log.error("Error creating movie game", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Failed to create movie game: " + e.getMessage()));
    }
  }

  @GetMapping("/patient/{keycloakId}")
  public ResponseEntity<List<MovieGameResponse>> getGamesByPatient(@PathVariable String keycloakId) {
    return ResponseEntity.ok(movieGameService.getGamesByPatient(keycloakId));
  }

  @GetMapping("/{gameId}")
  public ResponseEntity<?> getGameDetail(@PathVariable Long gameId) {
    try {
      return ResponseEntity.ok(movieGameService.getGameDetail(gameId));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", e.getMessage()));
    }
  }

  @PutMapping("/{gameId}")
  public ResponseEntity<?> editGame(
      @PathVariable Long gameId,
      @Valid @RequestBody EditMovieGameRequest request) {
    try {
      MovieGameResponse game = movieGameService.editGame(gameId, request);
      return ResponseEntity.ok(game);
    } catch (Exception e) {
      log.error("Error editing movie game {}", gameId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Failed to edit movie game: " + e.getMessage()));
    }
  }

  @DeleteMapping("/{gameId}")
  public ResponseEntity<?> deleteGame(@PathVariable Long gameId) {
    try {
      movieGameService.deleteGame(gameId);
      return ResponseEntity.ok(Map.of("message", "Movie game deleted successfully"));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", e.getMessage()));
    }
  }

  // ─── Movie Game Play ──────────────────────────────────────

  @GetMapping("/play/{gameId}")
  public ResponseEntity<?> getGameForPlay(@PathVariable Long gameId) {
    try {
      return ResponseEntity.ok(movieGameService.getGameForPlay(gameId));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", e.getMessage()));
    }
  }

  @PostMapping("/play/{gameId}/submit")
  public ResponseEntity<?> submitAnswers(
      @PathVariable Long gameId,
      @RequestHeader("X-User-Id") String playerKeycloakId,
      @RequestBody MovieGameSubmitRequest request) {
    try {
      MovieGameAttemptResponse response = movieGameService.submitAnswers(gameId, playerKeycloakId, request);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error submitting movie game answers for game {}", gameId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Failed to submit answers: " + e.getMessage()));
    }
  }
}
