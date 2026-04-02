package org.techhive.alertservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.techhive.alertservice.entity.AppointmentReminder;
import org.techhive.alertservice.entity.ReminderStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentReminderRepository extends JpaRepository<AppointmentReminder, Long> {

    List<AppointmentReminder> findByAppointmentId(Long appointmentId);

    List<AppointmentReminder> findByStatus(ReminderStatus status);

    List<AppointmentReminder> findByReminderTimeBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT r FROM AppointmentReminder r WHERE r.sent = false AND r.reminderTime <= :now")
    List<AppointmentReminder> findPendingReminders(@Param("now") LocalDateTime now);
}
