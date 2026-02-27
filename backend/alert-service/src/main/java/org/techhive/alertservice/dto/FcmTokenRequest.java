package org.techhive.alertservice.dto;

/**
 * Request body for registering a Firebase Cloud Messaging token for a user
 */
public class FcmTokenRequest {
    private String patientId;
    private String fcmToken;

    // Default constructor
    public FcmTokenRequest() {
    }

    // All-args constructor
    public FcmTokenRequest(String patientId, String fcmToken) {
        this.patientId = patientId;
        this.fcmToken = fcmToken;
    }

    // Getters and Setters
    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}
