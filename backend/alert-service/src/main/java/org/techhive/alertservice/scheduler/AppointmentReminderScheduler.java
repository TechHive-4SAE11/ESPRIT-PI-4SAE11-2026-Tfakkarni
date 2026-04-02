package org.techhive.alertservice.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.techhive.alertservice.service.AppointmentReminderService;

@Component
public class AppointmentReminderScheduler {

    private final AppointmentReminderService appointmentReminderService;

    public AppointmentReminderScheduler(AppointmentReminderService appointmentReminderService) {
        this.appointmentReminderService = appointmentReminderService;
    }

    @Scheduled(fixedRate = 60000)
    public void processPendingReminders() {
        appointmentReminderService.processPendingReminders();
    }
}
