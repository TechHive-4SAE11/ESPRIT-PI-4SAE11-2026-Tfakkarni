package org.techhive.userservice.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.techhive.userservice.entity.User;
import org.techhive.userservice.service.UserService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job that mutes push notifications for Alzheimer patients who have
 * been inactive for longer than the configured threshold (default: 30 days).
 *
 * IMPORTANT: Accounts are NOT disabled — patients can still log in.
 * Only the {@code notificationsEnabled} flag is set to {@code false} so the
 * alert-service skips pushing reminders to inactive patients.
 *
 * Notifications are automatically re-enabled the next time the patient
 * interacts with the platform (PATCH /api/users/{keycloakId}/activity).
 *
 * Runs daily at 02:00 AM (configurable via application.yml).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PatientInactivityScheduler {

  private final UserService userService;

  /** Number of days without activity before notifications are muted. */
  @Value("${patient.inactivity.threshold-days:30}")
  private int inactivityThresholdDays;

  @Scheduled(cron = "${patient.inactivity.cron:0 0 2 * * *}")
  public void muteInactivePatientNotifications() {
    LocalDateTime threshold = LocalDateTime.now().minusDays(inactivityThresholdDays);
    log.info("[PatientInactivityScheduler] Running — muting notifications for patients inactive since before {}",
        threshold);

    List<User> inactivePatients = userService.findInactivePatients(threshold);

    if (inactivePatients.isEmpty()) {
      log.info("[PatientInactivityScheduler] No inactive patients found.");
      return;
    }

    log.info("[PatientInactivityScheduler] Found {} inactive patient(s) — muting notifications.",
        inactivePatients.size());

    for (User patient : inactivePatients) {
      try {
        if (patient.isNotificationsEnabled()) {
          patient.setNotificationsEnabled(false);
          userService.save(patient);
          log.info(
              "[PatientInactivityScheduler] Muted notifications for patient keycloakId={} ({} {}) — last active: {}",
              patient.getKeycloakId(),
              patient.getFirstName(),
              patient.getLastName(),
              patient.getLastActiveAt() != null ? patient.getLastActiveAt() : "never");
        }
      } catch (Exception e) {
        log.error("[PatientInactivityScheduler] Failed to mute patient keycloakId={}: {}",
            patient.getKeycloakId(), e.getMessage(), e);
      }
    }

    log.info("[PatientInactivityScheduler] Completed.");
  }
}
