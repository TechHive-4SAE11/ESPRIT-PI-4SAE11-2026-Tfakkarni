package org.techhive.medicalservice.dto;

import java.time.LocalDateTime;

public class CalendarStatusDTO {
    private boolean connected;
    private String googleEmail;
    private LocalDateTime lastSync;
    private int syncedAppointments;

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public String getGoogleEmail() {
        return googleEmail;
    }

    public void setGoogleEmail(String googleEmail) {
        this.googleEmail = googleEmail;
    }

    public LocalDateTime getLastSync() {
        return lastSync;
    }

    public void setLastSync(LocalDateTime lastSync) {
        this.lastSync = lastSync;
    }

    public int getSyncedAppointments() {
        return syncedAppointments;
    }

    public void setSyncedAppointments(int syncedAppointments) {
        this.syncedAppointments = syncedAppointments;
    }
}
