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
public class CaregiverStressHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String caregiverId;
    private Integer stressScore;
    private String stressLevel;
    private String factors;
    private String recommendation;
    private String triggeredBy;
    private LocalDateTime createdAt;
}
