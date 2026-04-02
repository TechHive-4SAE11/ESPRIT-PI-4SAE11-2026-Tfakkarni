package org.techhive.alertservice.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.alertservice.dto.AppointmentReminderRequestDTO;
import org.techhive.alertservice.dto.AppointmentReminderResponseDTO;
import org.techhive.alertservice.service.AppointmentReminderService;

import java.util.List;

@RestController
@RequestMapping("/api/alert/appointment-reminders")
public class AppointmentReminderController {

    private final AppointmentReminderService service;

    public AppointmentReminderController(AppointmentReminderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AppointmentReminderResponseDTO> create(@Valid @RequestBody AppointmentReminderRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentReminderResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<AppointmentReminderResponseDTO>> list(
            @RequestParam(required = false) Long appointmentId) {
        if (appointmentId != null) {
            return ResponseEntity.ok(service.getByAppointmentId(appointmentId));
        }
        return ResponseEntity.ok(service.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppointmentReminderResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentReminderRequestDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pending")
    public ResponseEntity<List<AppointmentReminderResponseDTO>> getPendingReminders() {
        return ResponseEntity.ok(service.getPendingReminders());
    }
}
