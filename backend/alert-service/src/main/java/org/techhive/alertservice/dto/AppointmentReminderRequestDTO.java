package org.techhive.alertservice.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.techhive.alertservice.entity.ReminderChannel;
import org.techhive.alertservice.entity.ReminderType;

import java.time.LocalDateTime;

public class AppointmentReminderRequestDTO {

    @NotNull(message = "appointmentId is required")
    private Long appointmentId;

    @NotBlank(message = "patientId is required")
    private String patientId;

    @NotNull(message = "reminderType is required")
    private ReminderType reminderType;

    @NotNull(message = "reminderTime is required")
    @Future(message = "reminderTime must be in the future")
    private LocalDateTime reminderTime;

    @NotNull(message = "channel is required")
    private ReminderChannel channel;

    private String patientPhone;

    private String patientEmail;

    private String message;

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public ReminderType getReminderType() {
        return reminderType;
    }

    public void setReminderType(ReminderType reminderType) {
        this.reminderType = reminderType;
    }

    public LocalDateTime getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(LocalDateTime reminderTime) {
        this.reminderTime = reminderTime;
    }

    public ReminderChannel getChannel() {
        return channel;
    }

    public void setChannel(ReminderChannel channel) {
        this.channel = channel;
    }

    public String getPatientPhone() {
        return patientPhone;
    }

    public void setPatientPhone(String patientPhone) {
        this.patientPhone = patientPhone;
    }

    public String getPatientEmail() {
        return patientEmail;
    }

    public void setPatientEmail(String patientEmail) {
        this.patientEmail = patientEmail;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
