package org.techhive.gameservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "quizzes")
public class Quiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String topic;
    private Integer totalScore;
    private LocalDateTime dateTaken;

    @Column(name = "caregiver_id")
    private Long caregiverId;

    /**
     * Highest difficulty level reached in this attempt (1 = Easy, 2 = Medium, 3 =
     * Hard)
     */
    @Column(name = "level_reached")
    private Integer levelReached;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL)
    private List<Question> questions;

}
