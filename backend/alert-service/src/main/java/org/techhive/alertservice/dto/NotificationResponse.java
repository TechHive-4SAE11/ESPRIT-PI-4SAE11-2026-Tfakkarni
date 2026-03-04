package org.techhive.alertservice.dto;

import java.util.List;

/**
 * Response wrapper for notifications endpoint
 */
public class NotificationResponse {
    private int totalNotifications;
    private int unreadCount;
    private List<MedicationNotificationDTO> notifications;
    private String date;
    private String message;

    // Default constructor
    public NotificationResponse() {
    }

    // All-args constructor
    public NotificationResponse(int totalNotifications, int unreadCount, List<MedicationNotificationDTO> notifications,
                                String date, String message) {
        this.totalNotifications = totalNotifications;
        this.unreadCount = unreadCount;
        this.notifications = notifications;
        this.date = date;
        this.message = message;
    }

    // Getters and Setters
    public int getTotalNotifications() {
        return totalNotifications;
    }

    public void setTotalNotifications(int totalNotifications) {
        this.totalNotifications = totalNotifications;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public List<MedicationNotificationDTO> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<MedicationNotificationDTO> notifications) {
        this.notifications = notifications;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int totalNotifications;
        private int unreadCount;
        private List<MedicationNotificationDTO> notifications;
        private String date;
        private String message;

        public Builder totalNotifications(int totalNotifications) {
            this.totalNotifications = totalNotifications;
            return this;
        }

        public Builder unreadCount(int unreadCount) {
            this.unreadCount = unreadCount;
            return this;
        }

        public Builder notifications(List<MedicationNotificationDTO> notifications) {
            this.notifications = notifications;
            return this;
        }

        public Builder date(String date) {
            this.date = date;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public NotificationResponse build() {
            return new NotificationResponse(totalNotifications, unreadCount, notifications, date, message);
        }
    }
}
