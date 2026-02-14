package org.techhive.gameservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.gameservice.dto.GameStatsResponse;
import org.techhive.gameservice.dto.OverviewStatsResponse;
import org.techhive.gameservice.entity.GameAttempt;
import org.techhive.gameservice.repository.GameAttemptRepository;
import org.techhive.gameservice.service.GameStatsService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/games/stats")
@RequiredArgsConstructor
public class GameStatsController {

    private final GameStatsService gameStatsService;
    private final GameAttemptRepository gameAttemptRepository;

    /**
     * Get stats for a specific patient/player.
     */
    @GetMapping("/patient/{keycloakId}")
    public ResponseEntity<GameStatsResponse> getPlayerStats(@PathVariable String keycloakId) {
        return ResponseEntity.ok(gameStatsService.getPlayerStats(keycloakId));
    }

    /**
     * Get platform-wide overview stats (admin view).
     */
    @GetMapping("/overview")
    public ResponseEntity<OverviewStatsResponse> getOverviewStats() {
        return ResponseEntity.ok(gameStatsService.getOverviewStats());
    }

    /**
     * Get all attempts for a specific patient (doctor/admin view).
     */
    @GetMapping("/attempts/player/{keycloakId}")
    public ResponseEntity<List<GameAttempt>> getPlayerAttempts(@PathVariable String keycloakId) {
        return ResponseEntity.ok(gameAttemptRepository.findByPlayerKeycloakId(keycloakId));
    }

    /**
     * Get all attempts for a specific game.
     */
    @GetMapping("/attempts/game/{gameId}")
    public ResponseEntity<List<GameAttempt>> getGameAttempts(@PathVariable Long gameId) {
        return ResponseEntity.ok(gameAttemptRepository.findByMiniGameId(gameId));
    }
}
