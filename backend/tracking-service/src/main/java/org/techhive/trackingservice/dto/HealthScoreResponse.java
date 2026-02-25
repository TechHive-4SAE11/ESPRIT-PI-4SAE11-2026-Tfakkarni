package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Réponse de l'API Daily Health Score.
 * Contient le score total, le plafond ajusté (données disponibles), le niveau de risque,
 * le détail par catégorie et la liste des catégories non renseignées.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthScoreResponse {

    /** Score total obtenu (somme des catégories avec données). */
    private int totalScore;

    /** Plafond du score pour les catégories effectivement prises en compte (≤ 100). */
    private int adjustedMaxScore;

    private String riskLevel;
    private String colorCode;

    /** Détail par catégorie (inclut les catégories exclues avec excluded=true). */
    private List<CategoryBreakdown> breakdown;

    /** Catégories pour lesquelles la donnée n'était pas renseignée (exclues du calcul). */
    private List<String> missingCategories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryBreakdown {
        private String category;
        private int score;
        private int maxScore;
        private String rawValue;
        private String label;
        /** true si la catégorie a été exclue du calcul (donnée non renseignée). */
        private boolean excluded;
    }
}
