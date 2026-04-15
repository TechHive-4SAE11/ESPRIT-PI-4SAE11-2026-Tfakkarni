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
 * Calcul du Score Sante Quotidien (sur 100 pts).
 *
 * L'observance medicamenteuse est la priorite absolue (75 % du score).
 *
 *  Medicaments   /75  -> observance = prise de tous les medicaments prescrits
 *  Hydratation    /9  -> cible >= 1500 ml/j
 *  Activite        /9  -> cible >= 30 min d'activite physique
 *  Incidents       /7  -> penalite par incident signale
 *
 * Le score est toujours normalise sur 100.
 */
@Service
@RequiredArgsConstructor
public class HealthScoreService {

    private final DailyLogRepository    logRepo;
    private final MedicationRepository  medicationRepo;

    // -------------------------------------------------------------------------

    public HealthScoreResponse computeDailyScore(String patientKeycloakId, LocalDate date) {

        DailyLog log = logRepo.findFirstByPatientKeycloakIdAndLogDate(patientKeycloakId, date)
                .orElse(null);

        List<CategoryBreakdown> breakdown = new ArrayList<>();
        int totalScore = 0;

        // -- Medicaments (/75) -- PRIORITE ABSOLUE ----------------------------
        int expectedMeds    = countExpectedMeds(patientKeycloakId);
        int takenMeds       = log != null ? countTakenMedications(log) : 0;
        int medicationScore = computeMedicationScore(expectedMeds, takenMeds);
        totalScore += medicationScore;
        breakdown.add(CategoryBreakdown.builder()
                .category("MEDICATIONS")
                .score(medicationScore)
                .maxScore(MAX_MEDICATIONS)
                .rawValue(takenMeds + "/" + expectedMeds + " pris")
                .label("Medicaments")
                .excluded(false)
                .build());

        // -- Hydratation (/9) -------------------------------------------------
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

        // -- Activite (/9) ----------------------------------------------------
        int activityMinutes = log != null ? sumPhysicalActivityMinutes(log) : 0;
        int activityScore   = computeActivityScore(activityMinutes);
        totalScore += activityScore;
        breakdown.add(CategoryBreakdown.builder()
                .category("ACTIVITY")
                .score(activityScore)
                .maxScore(MAX_ACTIVITY)
                .rawValue(activityMinutes + " min")
                .label("Activite physique")
                .excluded(false)
                .build());

        // -- Incidents (/7) ---------------------------------------------------
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

        // -- Pourcentage & niveau de risque -----------------------------------
        // TOTAL_MAX == 100 -> totalScore IS already the percentage
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

    // -- Aggregators ----------------------------------------------------------

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

    // -- Scoring rules --------------------------------------------------------

    /**
     * Medicaments /75 : priorite ABSOLUE.
     * 100%->75, >=80%->56, >=60%->38, >=40%->19, <40%->5.
     * 0 prescriptions actives -> 75 (pas de penalite).
     */
    int computeMedicationScore(int expected, int taken) {
        if (expected == 0) return MAX_MEDICATIONS;  // 75 - aucune prescription
        int pct = (taken * 100) / expected;
        if (pct >= 100) return MAX_MEDICATIONS;     // 75
        if (pct >= 80)  return 56;                  // ~75%
        if (pct >= 60)  return 38;                  // ~50%
        if (pct >= 40)  return 19;                  // ~25%
        return 5;                                   // non-observance
    }

    /** Hydratation /9 : >=1500->9, >=1200->7, >=800->5, >=400->3, <400->1 */
    int computeHydrationScore(int ml) {
        if (ml >= 1500) return MAX_HYDRATION;       // 9
        if (ml >= 1200) return 7;
        if (ml >= 800)  return 5;
        if (ml >= 400)  return 3;
        return 1;
    }

    /** Activite /9 : >=30 min->9, >=20 min->7, >=10 min->4, <10 min->1 */
    int computeActivityScore(int minutes) {
        if (minutes >= 30) return MAX_ACTIVITY;     // 9
        if (minutes >= 20) return 7;
        if (minutes >= 10) return 4;
        return 1;
    }

    /** Incidents /7 : 0->7, 1->5, 2->2, >=3->0 */
    int computeIncidentScore(int count) {
        if (count == 0) return MAX_INCIDENTS;        // 7
        if (count == 1) return 5;
        if (count == 2) return 2;
        return 0;
    }

    /** Niveau de risque - seuils sur le % (= totalScore puisque /100) */
    String riskLevel(int pct) {
        if (pct >= 85) return "Excellent";
        if (pct >= 65) return "Stable";
        if (pct >= 45) return "Risque moyen";
        return "Risque eleve";
    }

    String colorCode(int pct) {
        if (pct >= 65) return "#22c55e"; // green
        if (pct >= 45) return "#f97316"; // orange
        return "#ef4444";                // red
    }
}
