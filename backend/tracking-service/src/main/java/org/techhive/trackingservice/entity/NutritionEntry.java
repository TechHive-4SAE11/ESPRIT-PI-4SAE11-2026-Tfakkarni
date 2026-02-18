package org.techhive.trackingservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "nutrition_entries")
@Data @NoArgsConstructor @AllArgsConstructor
public class NutritionEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_log_id", nullable = false)
    private DailyLog dailyLog;

    private String mealType;      // BREAKFAST, LUNCH, DINNER, SNACK
    private String description;
    private String quantity;      // COMPLET, DEMI, PEU, RIEN
    private String appetite;      // BON, MOYEN, FAIBLE
    private Integer hydrationMl;
    private String notes;
    private String entryTime;     // HH:mm
}