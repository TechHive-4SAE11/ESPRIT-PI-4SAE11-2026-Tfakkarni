package org.techhive.alertservice.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.techhive.alertservice.dto.MedicationNotificationDTO;

import java.util.List;

/**
 * Service for sending Firebase Cloud Messaging push notifications.
 * Gracefully handles cases where Firebase is not configured.
 */
@Slf4j
@Service
public class FirebasePushService {

    @Autowired(required = false)
    private FirebaseMessaging firebaseMessaging;

    /**
     * Send a medication reminder push notification to a specific device
     */
    public boolean sendMedicationReminder(String fcmToken, MedicationNotificationDTO notification) {
        if (firebaseMessaging == null) {
            log.warn("Firebase Messaging not configured — skipping push for medication: {}",
                    notification.getMedicationName());
            return false;
        }

        if (fcmToken == null || fcmToken.isBlank()) {
            log.warn("No FCM token available — skipping push for medication: {}",
                    notification.getMedicationName());
            return false;
        }

        try {
            String body = buildNotificationBody(notification);

            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle("💊 Rappel Médicament — " + notification.getMedicationName())
                            .setBody(body)
                            .build())
                    .putData("type", "MEDICATION_REMINDER")
                    .putData("medicationId", String.valueOf(notification.getMedicationId()))
                    .putData("medicationName", notification.getMedicationName())
                    .putData("dosage", notification.getDosage() != null ? notification.getDosage() : "")
                    .putData("frequency", notification.getFrequency() != null ? notification.getFrequency() : "")
                    .putData("instructions", notification.getInstructions() != null ? notification.getInstructions() : "")
                    .putData("notificationId", notification.getId())
                    .build();

            String messageId = firebaseMessaging.send(message);
            log.info("✅ Push notification sent for medication {}: messageId={}",
                    notification.getMedicationName(), messageId);
            return true;

        } catch (Exception e) {
            log.error("❌ Failed to send push notification for medication {}: {}",
                    notification.getMedicationName(), e.getMessage());
            return false;
        }
    }

    /**
     * Send reminders for all medications at once
     */
    public int sendBulkMedicationReminders(String fcmToken, List<MedicationNotificationDTO> notifications) {
        if (firebaseMessaging == null || fcmToken == null || fcmToken.isBlank()) {
            log.warn("Cannot send bulk reminders — Firebase or FCM token not available");
            return 0;
        }

        int sent = 0;
        for (MedicationNotificationDTO notification : notifications) {
            if (sendMedicationReminder(fcmToken, notification)) {
                sent++;
            }
        }
        log.info("📬 Sent {}/{} push notifications", sent, notifications.size());
        return sent;
    }

    /**
     * Send a summary notification about today's medications
     */
    public boolean sendDailySummary(String fcmToken, int medicationCount) {
        if (firebaseMessaging == null || fcmToken == null || fcmToken.isBlank()) {
            return false;
        }

        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle("📋 Médicaments du jour")
                            .setBody("Vous avez " + medicationCount + " médicament(s) à prendre aujourd'hui. N'oubliez pas !")
                            .build())
                    .putData("type", "DAILY_SUMMARY")
                    .putData("medicationCount", String.valueOf(medicationCount))
                    .build();

            firebaseMessaging.send(message);
            log.info("✅ Daily summary push sent: {} medications", medicationCount);
            return true;

        } catch (Exception e) {
            log.error("❌ Failed to send daily summary push: {}", e.getMessage());
            return false;
        }
    }

    private String buildNotificationBody(MedicationNotificationDTO n) {
        StringBuilder body = new StringBuilder();
        if (n.getDosage() != null && !n.getDosage().isBlank()) {
            body.append("Dosage: ").append(n.getDosage());
        }
        if (n.getFrequency() != null && !n.getFrequency().isBlank()) {
            if (!body.isEmpty()) body.append(" • ");
            body.append(n.getFrequency());
        }
        if (n.getInstructions() != null && !n.getInstructions().isBlank()) {
            if (!body.isEmpty()) body.append("\n");
            body.append(n.getInstructions());
        }
        if (body.isEmpty()) {
            body.append("Il est temps de prendre votre médicament.");
        }
        return body.toString();
    }
}
