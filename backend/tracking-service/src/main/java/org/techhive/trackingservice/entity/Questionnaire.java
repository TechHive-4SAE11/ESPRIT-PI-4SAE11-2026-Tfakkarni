package org.techhive.trackingservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Questionnaire {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @OneToMany(mappedBy = "questionnaire", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions;
}
