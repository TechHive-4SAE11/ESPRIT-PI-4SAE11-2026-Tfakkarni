/**
 * Modèle pour le Score Santé Quotidien (Daily Health Score).
 * Données entièrement fournies par l'API — aucune logique de calcul côté frontend.
 */
export interface HealthScoreCategoryBreakdown {
  category: string;
  score: number;
  maxScore: number;
  rawValue: string;
  label: string;
  /** true si la catégorie a été exclue du calcul (donnée non renseignée). */
  excluded: boolean;
}

export interface HealthScoreResponse {
  totalScore: number;
  /** Plafond du score pour les catégories prises en compte (≤ 100). */
  adjustedMaxScore: number;
  riskLevel: string;
  colorCode: string;
  breakdown: HealthScoreCategoryBreakdown[];
  /** Catégories absentes (exclues du calcul). */
  missingCategories: string[];
}
