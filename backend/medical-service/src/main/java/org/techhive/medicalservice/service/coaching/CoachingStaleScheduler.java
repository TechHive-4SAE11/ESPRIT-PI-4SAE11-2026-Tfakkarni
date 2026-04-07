package org.techhive.medicalservice.service.coaching;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.medicalservice.entity.MedicalFolder;
import org.techhive.medicalservice.entity.coaching.CoachingGoal;
import org.techhive.medicalservice.entity.coaching.CoachingGoalStatus;
import org.techhive.medicalservice.repository.CoachingGoalRepository;
import org.techhive.medicalservice.repository.CoachingProgressRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Reminds patients when an active goal has had no progress logged for {@code coaching.stale-days}.
 * Throttled by {@link CoachingGoal#getLastStaleNotificationAt()} vs {@code coaching.stale-notification-cooldown-hours}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CoachingStaleScheduler {

    private final CoachingGoalRepository coachingGoalRepository;
    private final CoachingProgressRepository coachingProgressRepository;
    private final CoachingNotificationService coachingNotificationService;

    @Value("${coaching.stale-days:7}")
    private int staleDays;

    @Value("${coaching.stale-notification-cooldown-hours:168}")
    private int staleNotificationCooldownHours;

    /**
     * Runtime toggle for demos.
     * false: normal behavior
     * true: sends stale reminders in a fast cycle (every scheduler run, throttled to ~5 min).
     */
    private volatile boolean demoMode = false;

    @Scheduled(cron = "${coaching.scheduler.stale-cron:0 0 9 * * ?}")
    @Transactional
    public void notifyStaleGoals() {
        int sent = notifyStaleGoalsInternal();
        if (sent > 0) {
            log.info("Coaching stale reminders sent (scheduled): {}", sent);
        }
    }

    /** Manual trigger endpoint can call this for testing scheduler/FCM flow quickly. */
    @Transactional
    public int runNowForTest() {
        int sent = notifyStaleGoalsInternal();
        log.info("Coaching stale reminders sent (manual): {}", sent);
        return sent;
    }

    public boolean isDemoMode() {
        return demoMode;
    }

    public void setDemoMode(boolean demoMode) {
        this.demoMode = demoMode;
        log.info("Coaching scheduler mode switched to {}", demoMode ? "DEMO" : "NORMAL");
    }

    private int notifyStaleGoalsInternal() {
        List<CoachingGoal> active = coachingGoalRepository.findByStatus(CoachingGoalStatus.ACTIVE);
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        int sent = 0;
        for (CoachingGoal goal : active) {
            int effectiveStaleDays = effectiveStaleDays(goal);
            LocalDate cutoff = today.minusDays(effectiveStaleDays);
            var latestOpt = coachingProgressRepository.findLatestProgressDate(goal.getId());
            boolean stale = latestOpt.isEmpty() || latestOpt.get().isBefore(cutoff);
            if (!stale) {
                continue;
            }
            LocalDateTime cooldownBefore = now.minus(effectiveCooldownAmount(goal), effectiveCooldownUnit());
            if (goal.getLastStaleNotificationAt() != null && goal.getLastStaleNotificationAt().isAfter(cooldownBefore)) {
                continue;
            }
            MedicalFolder folder = goal.getMedicalFolder();
            String patientId = folder.getPatientId();
            coachingNotificationService.notifyUser(
                    patientId,
                    folder,
                    goal,
                    "STALE_PROGRESS",
                    "Rappel coaching",
                    "Pensez à enregistrer votre suivi : " + goal.getGoalTitle());
            goal.setLastStaleNotificationAt(now);
            coachingGoalRepository.save(goal);
            sent++;
        }
        return sent;
    }

    private int effectiveStaleDays(CoachingGoal goal) {
        if (demoMode) {
            return 0;
        }
        Integer targetDays = goal.getTargetDays();
        if (targetDays != null && targetDays > 0) {
            return targetDays;
        }
        return staleDays;
    }

    private long effectiveCooldownAmount(CoachingGoal goal) {
        if (demoMode) {
            return 5L;
        }
        Integer targetDays = goal.getTargetDays();
        if (targetDays != null && targetDays > 0) {
            return targetDays;
        }
        return staleNotificationCooldownHours;
    }

    private ChronoUnit effectiveCooldownUnit() {
        return demoMode ? ChronoUnit.MINUTES : ChronoUnit.HOURS;
    }
}
