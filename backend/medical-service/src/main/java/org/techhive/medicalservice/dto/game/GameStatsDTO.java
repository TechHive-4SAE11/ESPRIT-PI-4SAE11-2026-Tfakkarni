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
    private String patientId;
    private Integer gamesPlayed;
    private Double averageScore;
    private List<GameAttemptDTO> recentAttempts;
}
