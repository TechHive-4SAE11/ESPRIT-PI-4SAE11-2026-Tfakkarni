package org.techhive.mlservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long moduleId;
    private Boolean completed;
    private Double score;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime lastActivityAt;
}