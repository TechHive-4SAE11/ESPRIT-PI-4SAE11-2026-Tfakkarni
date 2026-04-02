package org.techhive.medicalservice.controller;

import org.techhive.medicalservice.client.AlertServiceClient;
import org.techhive.medicalservice.dto.AppointmentRequestDTO;
import org.techhive.medicalservice.dto.AppointmentResponseDTO;
import org.techhive.medicalservice.dto.RecurringAppointmentRequestDTO;
import org.techhive.medicalservice.dto.ReminderRequestDTO;
import org.techhive.medicalservice.dto.ReminderResponseDTO;
import org.techhive.medicalservice.exception.AppointmentNotFoundException;
import org.techhive.medicalservice.exception.AppointmentOverlapException;
import org.techhive.medicalservice.exception.InvalidAppointmentException;
import org.techhive.medicalservice.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/medical/appointments")
@CrossOrigin(origins = "http://localhost:4200")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AlertServiceClient alertServiceClient;

    public AppointmentController(AppointmentService appointmentService, AlertServiceClient alertServiceClient) {
        this.appointmentService = appointmentService;
        this.alertServiceClient = alertServiceClient;
    }

    @PostMapping("/recurring")
    public ResponseEntity<List<AppointmentResponseDTO>> createRecurringAppointments(
            @Valid @RequestBody RecurringAppointmentRequestDTO request
    ) {
        List<AppointmentResponseDTO> created = appointmentService.createRecurringAppointments(
                request.getAppointmentRequest(),
                request.getFrequency(),
                request.getNumberOfOccurrences()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{appointmentId}/reminders")
    public ResponseEntity<ReminderResponseDTO> createReminder(
            @PathVariable Long appointmentId,
            @Valid @RequestBody ReminderRequestDTO request) {
        request.setAppointmentId(appointmentId);
        ReminderResponseDTO created = alertServiceClient.createReminder(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{appointmentId}/reminders")
    public ResponseEntity<List<ReminderResponseDTO>> getRemindersByAppointment(@PathVariable Long appointmentId) {
        List<ReminderResponseDTO> reminders = alertServiceClient.getRemindersByAppointment(appointmentId);
        return ResponseEntity.ok(reminders);
    }

    @GetMapping("/reminders/{reminderId}")
    public ResponseEntity<ReminderResponseDTO> getReminderById(@PathVariable Long reminderId) {
        ReminderResponseDTO reminder = alertServiceClient.getReminderById(reminderId);
        return ResponseEntity.ok(reminder);
    }

    @PutMapping("/reminders/{reminderId}")
    public ResponseEntity<ReminderResponseDTO> updateReminder(
            @PathVariable Long reminderId,
            @Valid @RequestBody ReminderRequestDTO request) {
        ReminderResponseDTO updated = alertServiceClient.updateReminder(reminderId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/reminders/{reminderId}")
    public ResponseEntity<Void> deleteReminder(@PathVariable Long reminderId) {
        alertServiceClient.deleteReminder(reminderId);
        return ResponseEntity.noContent().build();
    }

    // ==================== RENDEZ-VOUS (génériques - à placer en second) ====================

    @PostMapping
    public ResponseEntity<AppointmentResponseDTO> createAppointment(@Valid @RequestBody AppointmentRequestDTO requestDTO) {
        AppointmentResponseDTO createdAppointment = appointmentService.createAppointment(requestDTO);
        return new ResponseEntity<>(createdAppointment, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AppointmentResponseDTO>> getAllAppointments() {
        List<AppointmentResponseDTO> appointments = appointmentService.getAllAppointments();
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponseDTO> getAppointmentById(@PathVariable Long id) {
        AppointmentResponseDTO appointment = appointmentService.getAppointmentById(id);
        return ResponseEntity.ok(appointment);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppointmentResponseDTO> updateAppointment(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentRequestDTO requestDTO) {
        AppointmentResponseDTO updatedAppointment = appointmentService.updateAppointment(id, requestDTO);
        return ResponseEntity.ok(updatedAppointment);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelAppointment(@PathVariable Long id) {
        appointmentService.cancelAppointment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<AppointmentResponseDTO>> getAppointmentsByPatient(@PathVariable String patientId) {
        List<AppointmentResponseDTO> appointments = appointmentService.getAppointmentsByPatient(patientId);
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<AppointmentResponseDTO>> getAppointmentsByDoctor(@PathVariable String doctorId) {
        List<AppointmentResponseDTO> appointments = appointmentService.getAppointmentsByDoctor(doctorId);
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/range")
    public ResponseEntity<List<AppointmentResponseDTO>> getAppointmentsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<AppointmentResponseDTO> appointments = appointmentService.getAppointmentsByDateRange(start, end);
        return ResponseEntity.ok(appointments);
    }

    // ==================== GESTIONNAIRES D'EXCEPTIONS ====================

    @ExceptionHandler(AppointmentNotFoundException.class)
    public ResponseEntity<String> handleNotFoundException(AppointmentNotFoundException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({AppointmentOverlapException.class, InvalidAppointmentException.class})
    public ResponseEntity<String> handleBadRequestException(RuntimeException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception ex) {
        return new ResponseEntity<>("Une erreur interne est survenue: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

