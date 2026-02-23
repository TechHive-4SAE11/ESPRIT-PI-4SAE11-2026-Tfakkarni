package org.techhive.trackingservice.service;

/**
 * Pondération des catégories du Score Santé Quotidien – TOTAL = 100 pts.
 *
 *  Hydratation   → 25 pts
 *  Médicaments   → 35 pts  (observance = priorité n°1 pour Alzheimer)
 *  Activité       → 25 pts
 *  Incidents      → 15 pts
 *  ─────────────────────────
 *  TOTAL MAX      → 100 pts
 */
public final class HealthScoreWeights {

    public static final int MAX_HYDRATION   = 25;
    public static final int MAX_MEDICATIONS = 35;
    public static final int MAX_ACTIVITY    = 25;
    public static final int MAX_INCIDENTS   = 15;

    public static final int TOTAL_MAX =
            MAX_HYDRATION + MAX_MEDICATIONS + MAX_ACTIVITY + MAX_INCIDENTS; // 100

    private HealthScoreWeights() {}
}
