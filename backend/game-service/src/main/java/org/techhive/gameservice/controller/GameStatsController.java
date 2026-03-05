package org.techhive.gameservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.gameservice.dto.GameStatsResponse;
import org.techhive.gameservice.dto.OverviewStatsResponse;
import org.techhive.gameservice.dto.ScoreAnalyticsResponse;
import org.techhive.gameservice.service.GameStatsService;

@RestController
@RequestMapping("/api/games/stats")
@RequiredArgsConstructor
public class GameStatsController {

    private final GameStatsService gameStatsService;

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
     * Get comprehensive score analytics for a patient (doctor view).
     * Aggregates attempts from ALL game types with score history for charting.
     */
    @GetMapping("/analytics/{keycloakId}")
    public ResponseEntity<ScoreAnalyticsResponse> getScoreAnalytics(@PathVariable String keycloakId) {
        return ResponseEntity.ok(gameStatsService.getScoreAnalytics(keycloakId));
    }
}
