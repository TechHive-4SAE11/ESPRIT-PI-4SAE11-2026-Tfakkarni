package org.techhive.medicalservice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.techhive.medicalservice.client.GameServiceClient;
import org.techhive.medicalservice.dto.PatientBadgeDto;
import org.techhive.medicalservice.entity.PatientBadge;
import org.techhive.medicalservice.repository.PatientBadgeRepository;
import org.techhive.medicalservice.service.PatientBadgeService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Moteur de règles pour l'attribution de badges aux patients,
 * basé sur les données de jeu récupérées depuis game-service.
 *
 * Badges supportés :
 * - MEMORY_STAR       → 100% sur une partie
 * - THREE_DAY_STREAK  → a joué 3 jours consécutifs
 * - IMPROVEMENT_BADGE → score actuel > moyenne des 3 dernières tentatives
 * - FAST_RECALL       → ≥80% avec durée ≤60s
 * - FOCUS_CHAMPION    → score parfait (score = totalQuestions) sur un quiz
 */
@Service
@Slf4j
public class PatientBadgeServiceImpl implements PatientBadgeService {

    private final PatientBadgeRepository badgeRepository;
    private final GameServiceClient gameServiceClient;
    private final ObjectMapper objectMapper;

    public PatientBadgeServiceImpl(
            PatientBadgeRepository badgeRepository,
            GameServiceClient gameServiceClient,
            ObjectMapper objectMapper) {
        this.badgeRepository = badgeRepository;
        this.gameServiceClient = gameServiceClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<PatientBadgeDto> getBadgesForPatient(String patientId) {
        return badgeRepository.findByPatientIdOrderByAwardedAtDesc(patientId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<PatientBadgeDto> evaluateAndAwardBadges(String patientId) {
        log.info("🎯 Évaluation des badges pour le patient: {}", patientId);

        // 0. Nettoyage des doublons existants en base
        try {
            badgeRepository.cleanupDuplicates();
        } catch (Exception e) {
            log.warn("Impossible de nettoyer les doublons: {}", e.getMessage());
        }

        // 1. Récupérer les analytics du game-service via Feign
        JsonNode analytics;
        try {
            analytics = gameServiceClient.getPatientAnalytics(patientId);
            log.debug("Données analytics reçues du game-service: {}", analytics);
        } catch (Exception e) {
            log.error("❌ Erreur lors de l'appel Feign au game-service pour {}: {}", patientId, e.getMessage());
            return Collections.emptyList();
        }

        if (analytics == null || !analytics.has("scoreHistory")) {
            log.warn("⚠️ Aucune donnée de jeu trouvée pour le patient {}", patientId);
            return Collections.emptyList();
        }

        // 2. Parser le scoreHistory
        JsonNode historyNode = analytics.path("scoreHistory");
        if (!historyNode.isArray() || historyNode.isEmpty()) {
            log.info("ℹ️ Historique de jeu vide pour le patient {}", patientId);
            return Collections.emptyList();
        }

        List<AttemptData> attempts = new ArrayList<>();
        for (JsonNode node : historyNode) {
            attempts.add(new AttemptData(
                    node.path("attemptId").asLong(0),
                    node.path("gameType").asText(""),
                    node.path("gameTitle").asText(""),
                    node.path("score").asInt(0),
                    node.path("totalQuestions").asInt(0),
                    node.path("percentage").asDouble(0),
                    node.has("durationSeconds") && !node.path("durationSeconds").isNull()
                            ? node.path("durationSeconds").asInt()
                            : null,
                    node.path("completedAt").asText("")
            ));
        }

        log.info("📊 Analyse de {} tentatives de jeu...", attempts.size());

        // Trier par date (du plus ancien au plus récent)
        attempts.sort(Comparator.comparing(a -> a.completedAt));

        List<PatientBadge> newBadges = new ArrayList<>();

        // 3. Évaluer chaque règle
        evaluateFirstGame(patientId, attempts, newBadges);
        evaluateMemoryStar(patientId, attempts, newBadges);
        evaluateThreeDayStreak(patientId, attempts, newBadges);
        evaluateImprovementBadge(patientId, attempts, newBadges);
        evaluateFastRecall(patientId, attempts, newBadges);
        evaluateFocusChampion(patientId, attempts, newBadges);

        // 4. Sauvegarder les nouveaux badges
        if (!newBadges.isEmpty()) {
            badgeRepository.saveAll(newBadges);
            log.info("🏆 {} nouveau(x) badge(s) attribué(s) au patient {}", newBadges.size(), patientId);
        } else {
            log.info("✅ Aucun nouveau badge à attribuer pour le patient {}", patientId);
        }

        return newBadges.stream().map(this::toDto).collect(Collectors.toList());
    }

    // ─── FIRST_GAME: A joué au moins une fois ────────────────────────

    private void evaluateFirstGame(String patientId, List<AttemptData> attempts, List<PatientBadge> newBadges) {
        if (!attempts.isEmpty()) {
            if (!badgeRepository.existsByPatientIdAndBadgeCode(patientId, "FIRST_GAME")) {
                newBadges.add(PatientBadge.builder()
                        .patientId(patientId)
                        .badgeCode("FIRST_GAME")
                        .badgeTitle("First Steps 👣")
                        .description("A complété son tout premier jeu cognitif !")
                        .sourceGameType(attempts.get(0).gameType)
                        .sourceAttemptId(attempts.get(0).attemptId)
                        .build());
            }
        }
    }

    // ─── MEMORY_STAR: 100% sur une partie ────────────────────────────

    private void evaluateMemoryStar(String patientId, List<AttemptData> attempts, List<PatientBadge> newBadges) {
        // Un seul badge de ce type par patient
        if (badgeRepository.existsByPatientIdAndBadgeCode(patientId, "MEMORY_STAR")) return;

        for (AttemptData a : attempts) {
            if (a.percentage >= 100.0 && a.attemptId > 0) {
                newBadges.add(PatientBadge.builder()
                        .patientId(patientId)
                        .badgeCode("MEMORY_STAR")
                        .badgeTitle("Memory Star 🧠")
                        .description("Score parfait de 100% sur un jeu cognitif")
                        .sourceGameType(a.gameType)
                        .sourceAttemptId(a.attemptId)
                        .build());
                return; // On s'arrête dès qu'on en a créé un
            }
        }
    }

    // ─── THREE_DAY_STREAK: 3 jours consécutifs ──────────────────────

    private void evaluateThreeDayStreak(String patientId, List<AttemptData> attempts, List<PatientBadge> newBadges) {
        if (badgeRepository.existsByPatientIdAndBadgeCode(patientId, "THREE_DAY_STREAK")) {
            return; // Déjà attribué
        }

        Set<LocalDate> playDates = new TreeSet<>();
        for (AttemptData a : attempts) {
            try {
                // Remplacement de 'Z' et nettoyage pour supporter plusieurs formats ISO
                String dateStr = a.completedAt.contains(".") 
                    ? a.completedAt.substring(0, a.completedAt.lastIndexOf(".")) 
                    : a.completedAt.replace("Z", "");
                LocalDateTime dt = LocalDateTime.parse(dateStr);
                playDates.add(dt.toLocalDate());
            } catch (Exception e) {
                log.debug("Erreur lors du parsing de la date {}: {}", a.completedAt, e.getMessage());
            }
        }

        List<LocalDate> sorted = new ArrayList<>(playDates);
        int streak = 1;
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).minusDays(1).equals(sorted.get(i - 1))) {
                streak++;
                if (streak >= 3) {
                    newBadges.add(PatientBadge.builder()
                            .patientId(patientId)
                            .badgeCode("THREE_DAY_STREAK")
                            .badgeTitle("3-Day Streak 🔄")
                            .description("A joué des jeux cognitifs pendant 3 jours consécutifs")
                            .sourceGameType(null)
                            .sourceAttemptId(null)
                            .build());
                    return;
                }
            } else {
                streak = 1;
            }
        }
    }

    // ─── IMPROVEMENT_BADGE: score actuel > moyenne des 3 derniers ────

    private void evaluateImprovementBadge(String patientId, List<AttemptData> attempts, List<PatientBadge> newBadges) {
        if (badgeRepository.existsByPatientIdAndBadgeCode(patientId, "IMPROVEMENT_BADGE")) return;
        if (attempts.size() < 4) return; // besoin d'au moins 4 tentatives

        AttemptData latest = attempts.get(attempts.size() - 1);
        double avgPrev3 = attempts.subList(attempts.size() - 4, attempts.size() - 1).stream()
                .mapToDouble(a -> a.percentage)
                .average().orElse(0);

        if (latest.percentage > avgPrev3 && latest.attemptId > 0) {
            newBadges.add(PatientBadge.builder()
                    .patientId(patientId)
                    .badgeCode("IMPROVEMENT_BADGE")
                    .badgeTitle("Improved Patient 📈")
                    .description(String.format("Progrès constaté : %.0f%% (moyenne précédente : %.0f%%)",
                            latest.percentage, avgPrev3))
                    .sourceGameType(latest.gameType)
                    .sourceAttemptId(latest.attemptId)
                    .build());
        }
    }

    // ─── FAST_RECALL: ≥80% avec durée ≤60s ─────────────────────────

    private void evaluateFastRecall(String patientId, List<AttemptData> attempts, List<PatientBadge> newBadges) {
        if (badgeRepository.existsByPatientIdAndBadgeCode(patientId, "FAST_RECALL")) return;

        for (AttemptData a : attempts) {
            if (a.percentage >= 80.0 && a.durationSeconds != null && a.durationSeconds > 0 && a.durationSeconds <= 60 && a.attemptId > 0) {
                newBadges.add(PatientBadge.builder()
                        .patientId(patientId)
                        .badgeCode("FAST_RECALL")
                        .badgeTitle("Fast Recall ⚡")
                        .description(String.format("Réflexes rapides : %.0f%% en moins de 60s", a.percentage))
                        .sourceGameType(a.gameType)
                        .sourceAttemptId(a.attemptId)
                        .build());
                return;
            }
        }
    }

    // ─── FOCUS_CHAMPION: score parfait (score = totalQuestions) ──────

    private void evaluateFocusChampion(String patientId, List<AttemptData> attempts, List<PatientBadge> newBadges) {
        if (badgeRepository.existsByPatientIdAndBadgeCode(patientId, "FOCUS_CHAMPION")) return;

        for (AttemptData a : attempts) {
            if (a.score > 0 && a.score == a.totalQuestions && a.attemptId > 0) {
                newBadges.add(PatientBadge.builder()
                        .patientId(patientId)
                        .badgeCode("FOCUS_CHAMPION")
                        .badgeTitle("Focus Champion 🎯")
                        .description("Maîtrise totale : aucune erreur sur un quiz")
                        .sourceGameType(a.gameType)
                        .sourceAttemptId(a.attemptId)
                        .build());
                return;
            }
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────

    private PatientBadgeDto toDto(PatientBadge entity) {
        return PatientBadgeDto.builder()
                .id(entity.getId())
                .patientId(entity.getPatientId())
                .badgeCode(entity.getBadgeCode())
                .badgeTitle(entity.getBadgeTitle())
                .description(entity.getDescription())
                .awardedAt(entity.getAwardedAt())
                .sourceGameType(entity.getSourceGameType())
                .sourceAttemptId(entity.getSourceAttemptId())
                .build();
    }

    /** Structure interne pour manipuler les données des tentatives */
    private static class AttemptData {
        final long attemptId;
        final String gameType;
        final String gameTitle;
        final int score;
        final int totalQuestions;
        final double percentage;
        final Integer durationSeconds;
        final String completedAt;

        AttemptData(long attemptId, String gameType, String gameTitle,
                    int score, int totalQuestions, double percentage,
                    Integer durationSeconds, String completedAt) {
            this.attemptId = attemptId;
            this.gameType = gameType;
            this.gameTitle = gameTitle;
            this.score = score;
            this.totalQuestions = totalQuestions;
            this.percentage = percentage;
            this.durationSeconds = durationSeconds;
            this.completedAt = completedAt;
        }
    }
}
