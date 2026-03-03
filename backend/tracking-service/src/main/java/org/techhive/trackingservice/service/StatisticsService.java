package org.techhive.trackingservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.techhive.trackingservice.dto.*;
import org.techhive.trackingservice.entity.*;
import org.techhive.trackingservice.repository.DailyLogRepository;
import org.techhive.trackingservice.repository.MedicationRepository;

import java.time.LocalDate;
import java.util.*;
import org.techhive.trackingservice.dto.StreakResponse;

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
            "CHUTE", "Chute",
            "CONFUSION", "Confusion",
            "AGITATION", "Agitation",
            "DEAMBULATION", "Déambulation",
            "CRISE", "Crise",
            "AUTRE", "Autre");

    private final DailyLogRepository logRepo;
    private final MedicationRepository medicationRepo;
    private final HealthScoreService healthScoreService;

    // ─────────────────────────────────────────────────────────────────────────
    // Score Trend
    // ─────────────────────────────────────────────────────────────────────────

    public ScoreTrendResponse getScoreTrend(String patientId, LocalDate start, LocalDate end) {
        List<String> dates = new ArrayList<>();
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
    // Incidents by type
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

        List<String> labels = new ArrayList<>();
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
    // Medication compliance
    // ─────────────────────────────────────────────────────────────────────────

    public MedicationComplianceResponse getMedicationCompliance(String patientId,
            LocalDate start, LocalDate end) {
        List<DailyLog> logs = logRepo
                .findByPatientKeycloakIdAndLogDateBetweenOrderByLogDateAsc(patientId, start, end);
        int taken = 0, missed = 0;
        for (DailyLog log : logs) {
            for (MedicationIntakeLog m : log.getMedicationIntakes()) {
                if ("PRIS".equals(m.getStatus()))
                    taken++;
                else
                    missed++;
            }
        }
        return MedicationComplianceResponse.builder().taken(taken).missed(missed).build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Hydration trend
    // ─────────────────────────────────────────────────────────────────────────

    public HydrationTrendResponse getHydrationTrend(String patientId,
            LocalDate start, LocalDate end) {
        List<DailyLog> logs = logRepo
                .findByPatientKeycloakIdAndLogDateBetweenOrderByLogDateAsc(patientId, start, end);

        Map<LocalDate, Integer> byDate = new LinkedHashMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1))
            byDate.put(d, 0);

        for (DailyLog log : logs) {
            int sum = log.getNutritionEntries().stream()
                    .mapToInt(n -> n.getHydrationMl() != null ? n.getHydrationMl() : 0)
                    .sum();
            byDate.merge(log.getLogDate(), sum, Integer::sum);
        }

        List<String> dates = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        byDate.forEach((d, v) -> {
            dates.add(formatDayLabel(d));
            values.add(v);
        });

        return HydrationTrendResponse.builder().dates(dates).values(values).build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Activity trend
    // ─────────────────────────────────────────────────────────────────────────

    public ActivityTrendResponse getActivityTrend(String patientId,
            LocalDate start, LocalDate end) {
        List<DailyLog> logs = logRepo
                .findByPatientKeycloakIdAndLogDateBetweenOrderByLogDateAsc(patientId, start, end);

        Map<LocalDate, Integer> byDate = new LinkedHashMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1))
            byDate.put(d, 0);

        for (DailyLog log : logs) {
            int sum = log.getActivityEntries().stream()
                    .filter(a -> "PHYSIQUE".equals(a.getActivityType()))
                    .mapToInt(a -> a.getDurationMinutes() != null ? a.getDurationMinutes() : 0)
                    .sum();
            byDate.merge(log.getLogDate(), sum, Integer::sum);
        }

        List<String> dates = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        byDate.forEach((d, v) -> {
            dates.add(formatDayLabel(d));
            values.add(v);
        });

        return ActivityTrendResponse.builder().dates(dates).values(values).build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Win Streak (Duolingo-style)
    // ─────────────────────────────────────────────────────────────────────────

    private static final int STREAK_THRESHOLD = 85;
    private static final int MAX_LIVES = 2;
    private static final int PREMIUM_STREAK_GOAL = 14;
    private static final int CALENDAR_DAYS = 14;

    private static final String[] DAYS_FR = { "Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim" };

    /**
     * Compute the Duolingo-style win streak.
     * <p>
     * Rules:
     * <ul>
     * <li>Walk backwards from yesterday — each day with score &ge; 85 adds to the
     * streak.</li>
     * <li>A day with score &lt; 85 (or missing) costs one life.</li>
     * <li>The patient starts with 2 lives; when all are consumed the streak
     * boundary is reached.</li>
     * <li>Today is treated as "in progress": it doesn't break the streak,
     * but if its score is already &ge; 85 it adds to the counter.</li>
     * </ul>
     */
    public StreakResponse getStreak(String patientId) {
        LocalDate today = LocalDate.now();

        // ── 0. Find the patient's first-ever daily log ──────────────────────
        // Days before this date are "inactive" — the feature didn't exist yet
        // for this patient, so they must NOT count as failures.
        LocalDate firstLogDate = logRepo.findFirstByPatientKeycloakIdOrderByLogDateAsc(patientId)
                .map(DailyLog::getLogDate)
                .orElse(today); // no logs at all → streak starts today

        // ── 1. Build the 14-day calendar (for the UI) ───────────────────────
        List<StreakResponse.StreakDay> calendar = new ArrayList<>(CALENDAR_DAYS);
        for (int i = 0; i < CALENDAR_DAYS; i++) {
            LocalDate d = today.minusDays(i);
            boolean active = !d.isBefore(firstLogDate);
            int pct = 0;
            if (active) {
                var resp = healthScoreService.computeDailyScore(patientId, d);
                pct = resp.getAdjustedMaxScore() > 0
                        ? (resp.getTotalScore() * 100) / resp.getAdjustedMaxScore()
                        : 0;
            }
            calendar.add(StreakResponse.StreakDay.builder()
                    .date(d.toString())
                    .score(pct)
                    .passed(active && pct >= STREAK_THRESHOLD)
                    .today(i == 0)
                    .active(active)
                    .dayLabel(DAYS_FR[d.getDayOfWeek().getValue() - 1])
                    .build());
        }

        // ── 2. Compute current streak (walk backwards from yesterday) ───────
        int streak = 0;

        // If today already qualifies, count it
        if (calendar.get(0).isActive() && calendar.get(0).isPassed()) {
            streak++;
        }

        // Walk from yesterday backwards — stop at firstLogDate (don't go further)
        int[] streakResult = walkBackStreak(patientId, today, firstLogDate, calendar);
        streak += streakResult[0];
        int livesRemaining = Math.max(0, streakResult[1]);

        return StreakResponse.builder()
                .currentStreak(streak)
                .livesRemaining(livesRemaining)
                .premiumUnlocked(streak >= PREMIUM_STREAK_GOAL)
                .last14Days(calendar)
                .build();
    }

    /**
     * Walk backwards from yesterday counting streak days, returning {streakCount,
     * livesRemaining}.
     */
    private int[] walkBackStreak(String patientId, LocalDate today, LocalDate firstLogDate,
            List<StreakResponse.StreakDay> calendar) {
        int streak = 0;
        int lives = MAX_LIVES;
        for (int i = 1; i <= 365; i++) {
            LocalDate d = today.minusDays(i);
            if (d.isBefore(firstLogDate))
                return new int[] { streak, lives };
            int pct = computeDayScore(patientId, d, i, calendar);
            if (pct >= STREAK_THRESHOLD) {
                streak++;
            } else if (--lives < 0) {
                return new int[] { streak, lives };
            }
        }
        return new int[] { streak, lives };
    }

    /** Get the percentage score for a day, reusing calendar data when available. */
    private int computeDayScore(String patientId, LocalDate day, int daysAgo,
            List<StreakResponse.StreakDay> calendar) {
        if (daysAgo < CALENDAR_DAYS) {
            return calendar.get(daysAgo).getScore();
        }
        var sc = healthScoreService.computeDailyScore(patientId, day);
        return sc.getAdjustedMaxScore() > 0
                ? (sc.getTotalScore() * 100) / sc.getAdjustedMaxScore()
                : 0;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Format court : "22 jan" / "01 fév" etc. */
    private String formatDayLabel(LocalDate d) {
        String[] MONTHS_FR = { "jan", "fév", "mar", "avr", "mai", "jun",
                "jul", "aoû", "sep", "oct", "nov", "déc" };
        return String.format("%02d %s", d.getDayOfMonth(), MONTHS_FR[d.getMonthValue() - 1]);
    }
}
