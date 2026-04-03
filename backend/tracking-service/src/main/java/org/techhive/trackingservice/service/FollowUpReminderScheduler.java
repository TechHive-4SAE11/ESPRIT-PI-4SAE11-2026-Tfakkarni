package org.techhive.trackingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler that triggers the daily follow-up reminder check every evening at 22:00.
 *
 * <p>Business need: At the end of each day, the platform must verify that the
 * caregiver (helper) has fully completed the patient's daily follow-up log —
 * covering nutrition, medication intake, and physical activity. If any category
 * is missing, a reminder record is persisted in the database so the caregiver
 * is notified the next time they open the dashboard.</p>
 *
 * <p>This service satisfies the requirement of a scheduled job that
 * <strong>modifies database state</strong> by inserting {@code follow_up_reminders}
 * rows for patients whose daily log is incomplete.</p>
 *
 * <p>Cron expression: {@code 0 0 22 * * *} — every day at 22:00 (server time).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FollowUpReminderScheduler {

    private final FollowUpReminderService followUpReminderService;

    /**
     * Runs every day at 22:00.
     * Delegates to {@link FollowUpReminderService#checkAndCreateReminders()}.
     */
    @Scheduled(cron = "${followup.scheduler.cron:0 0 22 * * *}")
    public void runDailyFollowUpCheck() {
        log.info("==================================================");
        log.info("[FollowUpReminderScheduler] Daily follow-up check triggered at 22:00");
        log.info("==================================================");

        try {
            int remindersCreated = followUpReminderService.checkAndCreateReminders();
            log.info("[FollowUpReminderScheduler] Done — {} reminder(s) created", remindersCreated);
        } catch (Exception e) {
            log.error("[FollowUpReminderScheduler] Error during daily follow-up check", e);
        }
    }
}
