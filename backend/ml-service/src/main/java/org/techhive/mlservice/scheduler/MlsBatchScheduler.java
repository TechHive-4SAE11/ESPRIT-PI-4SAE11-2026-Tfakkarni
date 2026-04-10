package org.techhive.mlservice.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.mlservice.entity.UserProgress;
import org.techhive.mlservice.repository.UserProgressRepository;
import org.techhive.mlservice.service.FAQAnalyticsService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MlsBatchScheduler {

    private final FAQAnalyticsService faqAnalyticsService;
    private final UserProgressRepository userProgressRepository;

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void analyzeFAQs() {
        log.info("Starting night batch: FAQ Analysis");
        // 1. Récupère toutes les questions du chat des dernières 24h
        // 2. Groupe les questions par similarité (Spring AI)
        // 3. Met à jour FAQAnalytics (incrémente frequency)
        faqAnalyticsService.analyzeFAQs();
        log.info("Finished night batch: FAQ Analysis");
    }

    @Scheduled(cron = "0 0 9 * * MON")
    @Transactional
    public void sendTrainingReminders() {
        log.info("Starting Monday morning batch: Training reminders");
        // 1. Récupère les aidants inactifs (pas de progression depuis 14 jours)
        LocalDateTime inactiveThreshold = LocalDateTime.now().minusDays(14);
        
        List<UserProgress> allProgress = userProgressRepository.findAll();
        List<Long> inactiveUserIds = allProgress.stream()
                .filter(p -> p.getLastActivityAt() != null && p.getLastActivityAt().isBefore(inactiveThreshold))
                .map(UserProgress::getUserId)
                .distinct()
                .collect(Collectors.toList());

        // 2. Pour chaque aidant, génère un rappel personnalisé
        // 3. Envoie via alert-service (notification)
        for (Long userId : inactiveUserIds) {
            log.info("Sending training reminder to inactive user: {}", userId);
            // Integration with alert-service logic...
        }
        log.info("Finished Monday morning batch: Training reminders");
    }
}
