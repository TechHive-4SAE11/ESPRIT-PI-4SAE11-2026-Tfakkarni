package org.techhive.trackingservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.techhive.trackingservice.dto.*;
import org.techhive.trackingservice.entity.*;
import org.techhive.trackingservice.repository.DailyLogRepository;
import org.techhive.trackingservice.repository.MedicationRepository;

import java.time.LocalDate;
import java.util.*;

/**
 * Service statistiques patient.
 *
 * Toutes les méthodes acceptent désormais (startDate, endDate) pour
 * permettre des périodes calendaires exactes (ex. 1er janvier → 31 janvier).
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private static final Map<String, String> INCIDENT_LABELS = Map.of(
            "CHUTE",        "Chute",
            "CONFUSION",    "Confusion",
            "AGITATION",    "Agitation",
            "DEAMBULATION", "Déambulation",
            "CRISE",        "Crise",
            "AUTRE",        "Autre"
    );

    private final DailyLogRepository   logRepo;
    private final MedicationRepository medicationRepo;
    private final HealthScoreService   healthScoreService;

    // ─────────────────────────────────────────────────────────────────────────
    //  Score Trend
    // ─────────────────────────────────────────────────────────────────────────

    public ScoreTrendResponse getScoreTrend(String patientId, LocalDate start, LocalDate end) {
        List<String>  dates  = new ArrayList<>();
        List<Integer> scores = new ArrayList<>();

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            dates.add(formatDayLabel(d));
            var resp = healthScoreService.computeDailyScore(patientId, d);
            // TOTAL_MAX == 100 → score IS the percentage directly
            int pct = resp.getAdjustedMaxScore() > 0
                    ? (resp.getTotalScore() * 100) / resp.getAdjustedMaxScore()
                    : 0;
            scores.add(pct);
        }
        return ScoreTrendResponse.builder().dates(dates).scores(scores).build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Incidents by type
    // ─────────────────────────────────────────────────────────────────────────

    public IncidentStatsResponse getIncidentTypes(String patientId, LocalDate start, LocalDate end) {
        List<DailyLog> logs = logRepo
                .findByPatientKeycloakIdAndLogDateBetweenOrderByLogDateAsc(patientId, start, end);

        Map<String, Integer> byType = new LinkedHashMap<>();
        for (DailyLog log : logs) {
            for (IncidentEntry e : log.getIncidentEntries()) {
                String type = e.getIncidentType() != null ? e.getIncidentType() : "AUTRE";
                byType.merge(type, 1, Integer::sum);
            }
        }

        List<String>  labels = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        byType.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> {
                    labels.add(INCIDENT_LABELS.getOrDefault(entry.getKey(), entry.getKey()));
                    values.add(entry.getValue());
                });

        return IncidentStatsResponse.builder().labels(labels).values(values).build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Medication compliance
    // ─────────────────────────────────────────────────────────────────────────

    public MedicationComplianceResponse getMedicationCompliance(String patientId,
                                                                 LocalDate start, LocalDate end) {
        List<DailyLog> logs = logRepo
                .findByPatientKeycloakIdAndLogDateBetweenOrderByLogDateAsc(patientId, start, end);
        int taken = 0, missed = 0;
        for (DailyLog log : logs) {
            for (MedicationIntakeLog m : log.getMedicationIntakes()) {
                if ("PRIS".equals(m.getStatus())) taken++;
                else missed++;
            }
        }
        return MedicationComplianceResponse.builder().taken(taken).missed(missed).build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Hydration trend
    // ─────────────────────────────────────────────────────────────────────────

    public HydrationTrendResponse getHydrationTrend(String patientId,
                                                      LocalDate start, LocalDate end) {
        List<DailyLog> logs = logRepo
                .findByPatientKeycloakIdAndLogDateBetweenOrderByLogDateAsc(patientId, start, end);

        Map<LocalDate, Integer> byDate = new LinkedHashMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) byDate.put(d, 0);

        for (DailyLog log : logs) {
            int sum = log.getNutritionEntries().stream()
                    .mapToInt(n -> n.getHydrationMl() != null ? n.getHydrationMl() : 0)
                    .sum();
            byDate.merge(log.getLogDate(), sum, Integer::sum);
        }

        List<String>  dates  = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        byDate.forEach((d, v) -> { dates.add(formatDayLabel(d)); values.add(v); });

        return HydrationTrendResponse.builder().dates(dates).values(values).build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Activity trend
    // ─────────────────────────────────────────────────────────────────────────

    public ActivityTrendResponse getActivityTrend(String patientId,
                                                   LocalDate start, LocalDate end) {
        List<DailyLog> logs = logRepo
                .findByPatientKeycloakIdAndLogDateBetweenOrderByLogDateAsc(patientId, start, end);

        Map<LocalDate, Integer> byDate = new LinkedHashMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) byDate.put(d, 0);

        for (DailyLog log : logs) {
            int sum = log.getActivityEntries().stream()
                    .filter(a -> "PHYSIQUE".equals(a.getActivityType()))
                    .mapToInt(a -> a.getDurationMinutes() != null ? a.getDurationMinutes() : 0)
                    .sum();
            byDate.merge(log.getLogDate(), sum, Integer::sum);
        }

        List<String>  dates  = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        byDate.forEach((d, v) -> { dates.add(formatDayLabel(d)); values.add(v); });

        return ActivityTrendResponse.builder().dates(dates).values(values).build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Format court : "22 jan" / "01 fév" etc. */
    private String formatDayLabel(LocalDate d) {
        String[] MONTHS_FR = { "jan","fév","mar","avr","mai","jun",
                "jul","aoû","sep","oct","nov","déc" };
        return String.format("%02d %s", d.getDayOfMonth(), MONTHS_FR[d.getMonthValue() - 1]);
    }
}
