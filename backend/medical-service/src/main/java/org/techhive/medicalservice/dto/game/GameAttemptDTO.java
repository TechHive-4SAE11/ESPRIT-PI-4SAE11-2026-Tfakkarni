package org.techhive.medicalservice.dto.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameAttemptDTO {
    private String gameName;
    private Integer score;
    private LocalDateTime playedAt;
}
