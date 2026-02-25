package org.techhive.trackingservice.service;

/**
 * Pondération des catégories du Score Santé Quotidien – TOTAL = 100 pts.
 *
 * L'observance médicamenteuse est LA priorité absolue pour les patients
 * Alzheimer. Elle représente 75 % du score total.
 * Les 25 % restants sont répartis équitablement entre les autres catégories.
 *
 *  Médicaments   → 75 pts  (observance = priorité ABSOLUE pour Alzheimer)
 *  Hydratation   →  9 pts
 *  Activité      →  9 pts
 *  Incidents     →  7 pts
 *  ─────────────────────────
 *  TOTAL MAX     → 100 pts
 */
public final class HealthScoreWeights {

    /** Médicaments : 75 % du score — priorité absolue (observance Alzheimer). */
    public static final int MAX_MEDICATIONS = 75;

    /** Hydratation : 9 % du score. */
    public static final int MAX_HYDRATION   = 9;

    /** Activité physique : 9 % du score. */
    public static final int MAX_ACTIVITY    = 9;

    /** Incidents : 7 % du score (pénalités). */
    public static final int MAX_INCIDENTS   = 7;

    /** Plafond total = 100 pts. */
    public static final int TOTAL_MAX =
            MAX_MEDICATIONS + MAX_HYDRATION + MAX_ACTIVITY + MAX_INCIDENTS; // 100

    private HealthScoreWeights() {}
}
