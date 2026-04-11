package org.techhive.medicalservice.client;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.cloud.openfeign.FeignClient;
import java.util.List;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@FeignClient(name = "game-service", path = "/api/game")
public interface GameServiceClient {

    @GetMapping("/stats/patient/{patientId}")
    GameStatsDTO getPatientGameStats(@PathVariable("patientId") String patientId);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class GameStatsDTO {
        private String patientId;
        private Integer gamesPlayed;
        private Double averageScore;
        private List<GameAttemptDTO> recentAttempts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class GameAttemptDTO {
        private String gameName;
        private Integer score;
        private java.time.LocalDateTime playedAt;
    }
}
