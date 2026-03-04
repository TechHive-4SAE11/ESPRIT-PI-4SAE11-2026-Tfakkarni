/** Réponse API - évolution du score santé */
export interface ScoreTrendResponse {
  dates: string[];
  scores: number[];
}

/** Réponse API - incidents par type */
export interface IncidentStatsResponse {
  labels: string[];
  values: number[];
}

/** Réponse API - observance médicaments */
export interface MedicationComplianceResponse {
  taken: number;
  missed: number;
}

/** Réponse API - tendance hydratation */
export interface HydrationTrendResponse {
  dates: string[];
  values: number[];
}

/** Réponse API - tendance activité physique */
export interface ActivityTrendResponse {
  dates: string[];
  values: number[];
}

/** Réponse API - win streak (Duolingo-style) */
export interface StreakResponse {
  currentStreak: number;
  livesRemaining: number;
  premiumUnlocked: boolean;
  last14Days: StreakDay[];
}

export interface StreakDay {
  date: string;
  score: number;
  passed: boolean;
  today: boolean;
  dayLabel: string;
  /** True if the day is within the streak tracking period (on or after first daily log). */
  active: boolean;
}
