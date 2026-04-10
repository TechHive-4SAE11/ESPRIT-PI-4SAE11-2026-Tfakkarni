package org.techhive.analyticsservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.techhive.analyticsservice.dto.GameStatsResponse;
import org.techhive.analyticsservice.dto.ScoreAnalyticsResponse;
import org.techhive.analyticsservice.dto.DataPointPerformanceDTO;

import java.util.List;
import java.util.Map;

@FeignClient(name = "game-service", fallback = GameServiceClientFallback.class)
public interface GameServiceClient {

    @GetMapping("/api/games/stats/patient/{keycloakId}")
    GameStatsResponse getPlayerStats(@PathVariable("keycloakId") String keycloakId);

    @GetMapping("/api/games/stats/analytics/{keycloakId}")
    ScoreAnalyticsResponse getScoreAnalytics(@PathVariable("keycloakId") String keycloakId);

    @GetMapping("/api/games/data/performance/{keycloakId}")
    List<DataPointPerformanceDTO> getPerformanceData(@PathVariable("keycloakId") String keycloakId);

    @GetMapping("/api/games/tags/{keycloakId}")
    List<Map<String, Object>> getPatientTags(@PathVariable("keycloakId") String keycloakId);
}
