package org.techhive.medicalservice.dto.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameStatsDTO {
    private String playerKeycloakId;
    private Integer totalGamesPlayed;
    private Double averageScore;
    private Integer bestScore;
    private Integer totalAttempts;
    private List<GameAttemptDTO> recentAttempts;
}
