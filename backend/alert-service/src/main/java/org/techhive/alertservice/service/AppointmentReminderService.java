package org.techhive.alertservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.techhive.alertservice.dto.AppointmentReminderRequestDTO;
import org.techhive.alertservice.dto.AppointmentReminderResponseDTO;
import org.techhive.alertservice.entity.AppointmentReminder;
import org.techhive.alertservice.entity.ReminderStatus;
import org.techhive.alertservice.repository.AppointmentReminderRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AppointmentReminderService {

    private final AppointmentReminderRepository repository;
    private final WebClient userServiceClient;

    public AppointmentReminderService(AppointmentReminderRepository repository,
                                      @Qualifier("userServiceClient") WebClient userServiceClient) {
        this.repository = repository;
        this.userServiceClient = userServiceClient;
    }

    public AppointmentReminderResponseDTO create(AppointmentReminderRequestDTO dto) {
        AppointmentReminder entity = toEntity(dto);
        entity.setStatus(ReminderStatus.PENDING);
        entity.setSent(false);
        return toResponseDTO(repository.save(entity));
    }

    public AppointmentReminderResponseDTO getById(Long id) {
        AppointmentReminder entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("AppointmentReminder not found with id: " + id));
        return toResponseDTO(entity);
    }

    public List<AppointmentReminderResponseDTO> getByAppointmentId(Long appointmentId) {
        return repository.findByAppointmentId(appointmentId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<AppointmentReminderResponseDTO> getAll() {
        return repository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public AppointmentReminderResponseDTO update(Long id, AppointmentReminderRequestDTO dto) {
        AppointmentReminder existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("AppointmentReminder not found with id: " + id));
        updateEntityFromDto(existing, dto);
        return toResponseDTO(repository.save(existing));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("AppointmentReminder not found with id: " + id);
        }
        repository.deleteById(id);
    }

    public List<AppointmentReminderResponseDTO> getPendingReminders() {
        return repository.findPendingReminders(LocalDateTime.now()).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void processPendingReminders() {
        List<AppointmentReminder> pending = repository.findPendingReminders(LocalDateTime.now());
        LocalDateTime now = LocalDateTime.now();
        for (AppointmentReminder reminder : pending) {
            try {
                // Skip inactive patients — their account is still active but they
                // are not currently using the platform so reminders are suppressed.
                if (!isNotificationsEnabled(reminder.getPatientId())) {
                    log.debug("🔕 Skipping appointment reminder for inactive patient {}", reminder.getPatientId());
                    continue;
                }
                // Simulate sending (e.g. would call SMS/Email/Push gateway)
                reminder.setSent(true);
                reminder.setSentAt(now);
                reminder.setStatus(ReminderStatus.SENT);
                repository.save(reminder);
            } catch (Exception e) {
                reminder.setStatus(ReminderStatus.FAILED);
                repository.save(reminder);
            }
        }
    }

    /**
     * Checks whether push notifications are enabled for a patient.
     * Defaults to {@code true} on any user-service error (fail-open).
     */
    @SuppressWarnings("unchecked")
    private boolean isNotificationsEnabled(String patientId) {
        try {
            Map<String, Object> response = userServiceClient.get()
                    .uri("/api/users/{id}/notifications-enabled", patientId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (response == null) return true;
            Object enabled = response.get("enabled");
            return enabled == null || Boolean.TRUE.equals(enabled);
        } catch (Exception e) {
            log.warn("Could not check notifications status for patient {} — defaulting to enabled: {}", patientId, e.getMessage());
            return true;
        }
    }

    private AppointmentReminder toEntity(AppointmentReminderRequestDTO dto) {
        AppointmentReminder e = new AppointmentReminder();
        e.setAppointmentId(dto.getAppointmentId());
        e.setPatientId(dto.getPatientId());
        e.setReminderType(dto.getReminderType());
        e.setReminderTime(dto.getReminderTime());
        e.setChannel(dto.getChannel());
        e.setPatientPhone(dto.getPatientPhone());
        e.setPatientEmail(dto.getPatientEmail());
        e.setMessage(dto.getMessage());
        return e;
    }

    private void updateEntityFromDto(AppointmentReminder e, AppointmentReminderRequestDTO dto) {
        e.setAppointmentId(dto.getAppointmentId());
        e.setPatientId(dto.getPatientId());
        e.setReminderType(dto.getReminderType());
        e.setReminderTime(dto.getReminderTime());
        e.setChannel(dto.getChannel());
        e.setPatientPhone(dto.getPatientPhone());
        e.setPatientEmail(dto.getPatientEmail());
        e.setMessage(dto.getMessage());
    }

    private AppointmentReminderResponseDTO toResponseDTO(AppointmentReminder e) {
        AppointmentReminderResponseDTO dto = new AppointmentReminderResponseDTO();
        dto.setId(e.getId());
        dto.setAppointmentId(e.getAppointmentId());
        dto.setPatientId(e.getPatientId());
        dto.setReminderType(e.getReminderType());
        dto.setReminderTime(e.getReminderTime());
        dto.setChannel(e.getChannel());
        dto.setPatientPhone(e.getPatientPhone());
        dto.setPatientEmail(e.getPatientEmail());
        dto.setMessage(e.getMessage());
        dto.setSent(e.isSent());
        dto.setSentAt(e.getSentAt());
        dto.setStatus(e.getStatus());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        return dto;
    }
}
