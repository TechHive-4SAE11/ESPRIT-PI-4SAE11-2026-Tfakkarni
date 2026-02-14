package org.techhive.gameservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.gameservice.dto.*;
import org.techhive.gameservice.service.GameService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    /**
     * Create a new minigame. The patient's keycloakId is passed as a header
     * (set by the frontend from the JWT subject).
     */
    @PostMapping
    public ResponseEntity<?> createGame(
            @RequestHeader("X-User-Id") String patientKeycloakId,
            @RequestBody CreateGameRequest request) {
        try {
            GameResponse game = gameService.createGame(patientKeycloakId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(game);
        } catch (Exception e) {
            log.error("Error creating game", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create game: " + e.getMessage()));
        }
    }

    /**
     * Upload images to an existing game.
     */
    @PostMapping("/{gameId}/images")
    public ResponseEntity<?> addImages(
            @PathVariable Long gameId,
            @RequestBody List<GameImageUpload> uploads) {
        try {
            GameDetailResponse detail = gameService.addImages(gameId, uploads);
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            log.error("Error uploading images to game {}", gameId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload images: " + e.getMessage()));
        }
    }

    /**
     * List all games for a specific patient.
     */
    @GetMapping("/patient/{keycloakId}")
    public ResponseEntity<List<GameResponse>> getGamesByPatient(@PathVariable String keycloakId) {
        return ResponseEntity.ok(gameService.getGamesByPatient(keycloakId));
    }

    /**
     * Get full game detail (management view, includes images).
     */
    @GetMapping("/{gameId}")
    public ResponseEntity<?> getGameDetail(@PathVariable Long gameId) {
        try {
            return ResponseEntity.ok(gameService.getGameDetail(gameId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete a minigame.
     */
    @DeleteMapping("/{gameId}")
    public ResponseEntity<?> deleteGame(@PathVariable Long gameId) {
        try {
            gameService.deleteGame(gameId);
            return ResponseEntity.ok(Map.of("message", "Game deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * List all games (admin view).
     */
    @GetMapping("/all")
    public ResponseEntity<List<GameResponse>> getAllGames() {
        return ResponseEntity.ok(gameService.getAllGames());
    }
}
