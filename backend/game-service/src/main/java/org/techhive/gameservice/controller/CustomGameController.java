package org.techhive.gameservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.techhive.gameservice.dto.*;
import org.techhive.gameservice.service.CustomGameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/games/custom")
@RequiredArgsConstructor
public class CustomGameController {

    private final CustomGameService gameService;

    // ===================== CRUD =====================

    @PostMapping("/{keycloakId}")
    public ResponseEntity<CustomGameResponse> createGame(
            @PathVariable String keycloakId,
            @Valid @RequestBody CreateCustomGameRequest request) {
        return ResponseEntity.ok(gameService.createGame(keycloakId, request));
    }

    @PutMapping("/{gameId}")
    public ResponseEntity<CustomGameResponse> editGame(
            @PathVariable Long gameId,
            @Valid @RequestBody EditCustomGameRequest request) {
        return ResponseEntity.ok(gameService.updateGame(gameId, request));
    }

    @GetMapping("/patient/{keycloakId}")
    public ResponseEntity<List<CustomGameResponse>> getGames(@PathVariable String keycloakId) {
        return ResponseEntity.ok(gameService.getGamesForPatient(keycloakId));
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<CustomGameDetailResponse> getGameDetail(@PathVariable Long gameId) {
        return ResponseEntity.ok(gameService.getGameDetail(gameId));
    }

    @DeleteMapping("/{gameId}")
    public ResponseEntity<Void> deleteGame(@PathVariable Long gameId) {
        gameService.deleteGame(gameId);
        return ResponseEntity.noContent().build();
    }

    // ===================== PLAY =====================

    @GetMapping("/play/{gameId}")
    public ResponseEntity<UnifiedPlayData> getPlayData(@PathVariable Long gameId) {
        return ResponseEntity.ok(gameService.getPlayData(gameId));
    }

    @GetMapping("/play/random/{keycloakId}")
    public ResponseEntity<UnifiedPlayData> getRandomPlayData(
            @PathVariable String keycloakId,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(gameService.getRandomPlayData(keycloakId, limit));
    }

    @PostMapping("/play/submit")
    public ResponseEntity<UnifiedPlayResult> submitResults(
            @RequestHeader("X-User-Id") String playerKeycloakId,
            @RequestBody UnifiedSubmitRequest request) {
        return ResponseEntity.ok(gameService.submitResults(playerKeycloakId, request));
    }

    // ===================== STATS =====================

    @GetMapping("/stats/{keycloakId}")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable String keycloakId) {
        return ResponseEntity.ok(gameService.getStats(keycloakId));
    }
}
