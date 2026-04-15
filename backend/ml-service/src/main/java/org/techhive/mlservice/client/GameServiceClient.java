package org.techhive.mlservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.techhive.mlservice.dto.GameStatsResponse;

@FeignClient(name = "game-service", url = "http://localhost:18082")
public interface GameServiceClient {
    @GetMapping("/api/games/custom/stats/{keycloakId}")
    GameStatsResponse getGameStats(@PathVariable("keycloakId") String keycloakId);
}
