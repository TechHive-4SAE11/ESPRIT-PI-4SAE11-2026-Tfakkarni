package org.techhive.gameservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.gameservice.dto.GameAttemptRequest;
import org.techhive.gameservice.dto.GameAttemptResponse;
import org.techhive.gameservice.service.GamePlayService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/games/play")
@RequiredArgsConstructor
public class GamePlayController {

    private final GamePlayService gamePlayService;

    /**
     * Get a game formatted for playing (images without correct answers, shuffled choices).
     */
    @GetMapping("/{gameId}")
    public ResponseEntity<?> getGameForPlay(@PathVariable Long gameId) {
        try {
            return ResponseEntity.ok(gamePlayService.getGameForPlay(gameId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Submit answers for a game and get the score.
     */
    @PostMapping("/{gameId}/submit")
    public ResponseEntity<?> submitAnswers(
            @PathVariable Long gameId,
            @RequestHeader("X-User-Id") String playerKeycloakId,
            @RequestBody GameAttemptRequest request) {
        try {
            GameAttemptResponse response = gamePlayService.submitAnswers(gameId, playerKeycloakId, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error submitting answers for game {}", gameId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to submit answers: " + e.getMessage()));
        }
    }
}
