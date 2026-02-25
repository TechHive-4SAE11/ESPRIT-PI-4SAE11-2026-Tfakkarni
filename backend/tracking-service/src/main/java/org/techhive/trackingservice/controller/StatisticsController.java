package org.techhive.trackingservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.trackingservice.dto.*;
import org.techhive.trackingservice.service.StatisticsService;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * API Statistiques.
 *
 * Paramètres de période (pour tous les endpoints) :
 *   ?startDate=2025-01-01&endDate=2025-01-31   → période calendaire exacte
 *   ?period=current_month                       → mois courant complet
 *   ?period=previous_month                      → mois précédent complet
 *   ?days=7  (ou ?days=30)                      → fenêtre glissante (rétro-compat)
 *
 * Si aucun paramètre n'est fourni, on retourne les 7 derniers jours.
 */
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/{patientId}/score-trend")
    public ResponseEntity<ScoreTrendResponse> getScoreTrend(
            @PathVariable String patientId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String period,
            @RequestParam(defaultValue = "7") int days) {

        var range = resolveRange(startDate, endDate, period, days);
        return ResponseEntity.ok(statisticsService.getScoreTrend(patientId, range[0], range[1]));
    }

    @GetMapping("/{patientId}/incident-types")
    public ResponseEntity<IncidentStatsResponse> getIncidentTypes(
            @PathVariable String patientId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String period,
            @RequestParam(defaultValue = "30") int days) {

        var range = resolveRange(startDate, endDate, period, days);
        return ResponseEntity.ok(statisticsService.getIncidentTypes(patientId, range[0], range[1]));
    }

    @GetMapping("/{patientId}/medication-compliance")
    public ResponseEntity<MedicationComplianceResponse> getMedicationCompliance(
            @PathVariable String patientId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String period,
            @RequestParam(defaultValue = "30") int days) {

        var range = resolveRange(startDate, endDate, period, days);
        return ResponseEntity.ok(statisticsService.getMedicationCompliance(patientId, range[0], range[1]));
    }

    @GetMapping("/{patientId}/hydration-trend")
    public ResponseEntity<HydrationTrendResponse> getHydrationTrend(
            @PathVariable String patientId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String period,
            @RequestParam(defaultValue = "7") int days) {

        var range = resolveRange(startDate, endDate, period, days);
        return ResponseEntity.ok(statisticsService.getHydrationTrend(patientId, range[0], range[1]));
    }

    @GetMapping("/{patientId}/activity-trend")
    public ResponseEntity<ActivityTrendResponse> getActivityTrend(
            @PathVariable String patientId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String period,
            @RequestParam(defaultValue = "7") int days) {

        var range = resolveRange(startDate, endDate, period, days);
        return ResponseEntity.ok(statisticsService.getActivityTrend(patientId, range[0], range[1]));
    }

    // ── Résolution de la période ──────────────────────────────────────────

    /**
     * Priorité : startDate+endDate > period > days
     */
    private LocalDate[] resolveRange(String startDate, String endDate,
                                      String period, int days) {
        // 1. Dates explicites
        if (startDate != null && endDate != null) {
            return new LocalDate[]{ LocalDate.parse(startDate), LocalDate.parse(endDate) };
        }

        LocalDate today = LocalDate.now();

        // 2. Période nommée
        if (period != null) {
            return switch (period) {
                case "current_month" -> {
                    YearMonth ym = YearMonth.from(today);
                    yield new LocalDate[]{ ym.atDay(1), ym.atEndOfMonth() };
                }
                case "previous_month" -> {
                    YearMonth ym = YearMonth.from(today).minusMonths(1);
                    yield new LocalDate[]{ ym.atDay(1), ym.atEndOfMonth() };
                }
                case "current_week" -> {
                    LocalDate mon = today.minusDays(today.getDayOfWeek().getValue() - 1);
                    yield new LocalDate[]{ mon, mon.plusDays(6) };
                }
                default -> rollingDays(today, Math.max(1, Math.min(365, days)));
            };
        }

        // 3. Fenêtre glissante (rétro-compat)
        return rollingDays(today, Math.max(1, Math.min(365, days)));
    }

    private LocalDate[] rollingDays(LocalDate today, int days) {
        return new LocalDate[]{ today.minusDays(days - 1), today };
    }
}
