package org.techhive.trackingservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.techhive.trackingservice.dto.HealthScoreResponse;
import org.techhive.trackingservice.dto.HealthScoreResponse.CategoryBreakdown;
import org.techhive.trackingservice.entity.DailyLog;
import org.techhive.trackingservice.repository.DailyLogRepository;
import org.techhive.trackingservice.repository.MedicationRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.techhive.trackingservice.service.HealthScoreWeights.*;

/**
 * Calcul du Score Santé Quotidien (sur 100 pts).
 *
 *  Hydratation   /25  → cible ≥ 1500 ml/j
 *  Médicaments   /35  → observance = prise de tous les médicaments prescrits
 *  Activité       /25  → cible ≥ 30 min d'activité physique
 *  Incidents      /15  → pénalité par incident signalé
 *
 * Le score est toujours sur 100 : on ramène (totalScore / TOTAL_MAX) * 100.
 * Si TOTAL_MAX == 100 le ratio vaut directement le score.
 */
@Service
@RequiredArgsConstructor
public class HealthScoreService {

    private final DailyLogRepository    logRepo;
    private final MedicationRepository  medicationRepo;

    // ─────────────────────────────────────────────────────────────────────────

    public HealthScoreResponse computeDailyScore(String patientKeycloakId, LocalDate date) {

        DailyLog log = logRepo.findByPatientKeycloakIdAndLogDate(patientKeycloakId, date)
                .orElse(null);

        List<CategoryBreakdown> breakdown = new ArrayList<>();
        int totalScore = 0;

        // ── Hydratation (/25) ────────────────────────────────────────────────
        int hydrationMl    = log != null ? sumHydration(log)         : 0;
        int hydrationScore = computeHydrationScore(hydrationMl);
        totalScore += hydrationScore;
        breakdown.add(CategoryBreakdown.builder()
                .category("HYDRATATION")
                .score(hydrationScore)
                .maxScore(MAX_HYDRATION)
                .rawValue(hydrationMl + " ml")
                .label("Hydratation")
                .excluded(false)
                .build());

        // ── Médicaments (/35) ────────────────────────────────────────────────
        int expectedMeds    = countExpectedMeds(patientKeycloakId);
        int takenMeds       = log != null ? countTakenMedications(log) : 0;
        int medicationScore = computeMedicationScore(expectedMeds, takenMeds);
        totalScore += medicationScore;
        breakdown.add(CategoryBreakdown.builder()
                .category("MEDICATIONS")
                .score(medicationScore)
                .maxScore(MAX_MEDICATIONS)
                .rawValue(takenMeds + "/" + expectedMeds + " pris")
                .label("Médicaments")
                .excluded(false)
                .build());

        // ── Activité (/25) ───────────────────────────────────────────────────
        int activityMinutes = log != null ? sumPhysicalActivityMinutes(log) : 0;
        int activityScore   = computeActivityScore(activityMinutes);
        totalScore += activityScore;
        breakdown.add(CategoryBreakdown.builder()
                .category("ACTIVITY")
                .score(activityScore)
                .maxScore(MAX_ACTIVITY)
                .rawValue(activityMinutes + " min")
                .label("Activité physique")
                .excluded(false)
                .build());

        // ── Incidents (/15) ──────────────────────────────────────────────────
        int incidentCount = log != null ? log.getIncidentEntries().size() : 0;
        int incidentScore = computeIncidentScore(incidentCount);
        totalScore += incidentScore;
        breakdown.add(CategoryBreakdown.builder()
                .category("INCIDENTS")
                .score(incidentScore)
                .maxScore(MAX_INCIDENTS)
                .rawValue(incidentCount + " incident(s)")
                .label("Incidents")
                .excluded(false)
                .build());

        // ── Pourcentage & niveau de risque ───────────────────────────────────
        // TOTAL_MAX == 100 → totalScore IS already the percentage
        int percentage = TOTAL_MAX == 100 ? totalScore : (totalScore * 100) / TOTAL_MAX;

        return HealthScoreResponse.builder()
                .totalScore(totalScore)
                .adjustedMaxScore(TOTAL_MAX)
                .riskLevel(riskLevel(percentage))
                .colorCode(colorCode(percentage))
                .breakdown(breakdown)
                .missingCategories(List.of())
                .build();
    }

    // ── Agrégateurs ───────────────────────────────────────────────────────────

    int sumHydration(DailyLog log) {
        return log.getNutritionEntries().stream()
                .mapToInt(n -> n.getHydrationMl() != null ? n.getHydrationMl() : 0)
                .sum();
    }

    int countExpectedMeds(String keycloakId) {
        return medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(keycloakId).size();
    }

    int countTakenMedications(DailyLog log) {
        return (int) log.getMedicationIntakes().stream()
                .filter(m -> "PRIS".equals(m.getStatus()))
                .count();
    }

    int sumPhysicalActivityMinutes(DailyLog log) {
        return log.getActivityEntries().stream()
                .filter(a -> "PHYSIQUE".equals(a.getActivityType()))
                .mapToInt(a -> a.getDurationMinutes() != null ? a.getDurationMinutes() : 0)
                .sum();
    }

    // ── Règles de scoring ─────────────────────────────────────────────────────

    /** Hydratation /25 : ≥1500→25, ≥1200→20, ≥800→13, ≥400→7, <400→2 */
    int computeHydrationScore(int ml) {
        if (ml >= 1500) return MAX_HYDRATION;    // 25
        if (ml >= 1200) return 20;
        if (ml >= 800)  return 13;
        if (ml >= 400)  return 7;
        return 2;
    }

    /** Médicaments /35 : 100%→35, ≥80%→27, ≥60%→18, ≥40%→10, <40%→3. 0 prescriptions→35. */
    int computeMedicationScore(int expected, int taken) {
        if (expected == 0) return MAX_MEDICATIONS;  // 35 – aucune prescription
        int pct = (taken * 100) / expected;
        if (pct >= 100) return MAX_MEDICATIONS;
        if (pct >= 80)  return 27;
        if (pct >= 60)  return 18;
        if (pct >= 40)  return 10;
        return 3;
    }

    /** Activité /25 : ≥30 min→25, ≥20 min→18, ≥10 min→10, <10 min→3 */
    int computeActivityScore(int minutes) {
        if (minutes >= 30) return MAX_ACTIVITY;     // 25
        if (minutes >= 20) return 18;
        if (minutes >= 10) return 10;
        return 3;
    }

    /** Incidents /15 : 0→15, 1→11, 2→6, ≥3→0 */
    int computeIncidentScore(int count) {
        if (count == 0) return MAX_INCIDENTS;       // 15
        if (count == 1) return 11;
        if (count == 2) return 6;
        return 0;
    }

    /** Niveau de risque — seuils sur le % (= totalScore puisque /100) */
    String riskLevel(int pct) {
        if (pct >= 85) return "Excellent";
        if (pct >= 65) return "Stable";
        if (pct >= 45) return "Risque moyen";
        return "Risque élevé";
    }

    String colorCode(int pct) {
        if (pct >= 65) return "#22c55e"; // green
        if (pct >= 45) return "#f97316"; // orange
        return "#ef4444";                // red
    }
}
