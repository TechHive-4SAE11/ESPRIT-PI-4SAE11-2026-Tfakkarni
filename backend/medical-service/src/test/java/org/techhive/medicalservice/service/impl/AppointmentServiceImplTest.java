package org.techhive.medicalservice.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.medicalservice.dto.AppointmentRequestDTO;
import org.techhive.medicalservice.dto.AppointmentResponseDTO;
import org.techhive.medicalservice.entity.Appointment;
import org.techhive.medicalservice.entity.AppointmentStatus;
import org.techhive.medicalservice.entity.AppointmentType;
import org.techhive.medicalservice.entity.MedicalFolder;
import org.techhive.medicalservice.exception.AppointmentNotFoundException;
import org.techhive.medicalservice.exception.AppointmentOverlapException;
import org.techhive.medicalservice.exception.BookingRestrictedException;
import org.techhive.medicalservice.exception.InvalidAppointmentException;
import org.techhive.medicalservice.repository.AppointmentRepository;
import org.techhive.medicalservice.repository.MedicalFolderRepository;
import org.techhive.medicalservice.service.AttendanceMonitoringService;
import org.techhive.medicalservice.service.GoogleCalendarService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private MedicalFolderRepository medicalFolderRepository;
    @Mock
    private AttendanceMonitoringService attendanceMonitoringService;
    @Mock
    private GoogleCalendarService googleCalendarService;

    private AppointmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AppointmentServiceImpl(
                appointmentRepository,
                medicalFolderRepository,
                attendanceMonitoringService,
                googleCalendarService);
    }

    @Test
    void createAppointmentRejectsRestrictedPatientInvalidTimesMissingDoctorAndOverlaps() {
        AppointmentRequestDTO request = request(AppointmentType.FOLLOW_UP, "doctor-a");
        MedicalFolder restricted = MedicalFolder.builder()
                .patientId("patient-a")
                .doctorId("doctor-a")
                .bookingRestricted(true)
                .restrictionReason("review required")
                .build();
        when(medicalFolderRepository.findByPatientId("patient-a")).thenReturn(List.of(restricted));

        assertThrows(BookingRestrictedException.class, () -> service.createAppointment(request));

        AppointmentRequestDTO invalidTimes = request(AppointmentType.FOLLOW_UP, "doctor-a");
        invalidTimes.setEndTime(invalidTimes.getStartTime());
        when(medicalFolderRepository.findByPatientId("patient-a")).thenReturn(List.of());
        assertThrows(InvalidAppointmentException.class, () -> service.createAppointment(invalidTimes));

        AppointmentRequestDTO missingDoctor = request(AppointmentType.CONSULTATION, " ");
        assertThrows(InvalidAppointmentException.class, () -> service.createAppointment(missingDoctor));

        AppointmentRequestDTO overlap = request(AppointmentType.FOLLOW_UP, "doctor-a");
        when(appointmentRepository.findOverlappingAppointments(eq("patient-a"), any(), any()))
                .thenReturn(List.of(appointment(90L, AppointmentStatus.SCHEDULED)));
        assertThrows(AppointmentOverlapException.class, () -> service.createAppointment(overlap));
    }

    @Test
    void createAppointmentSavesScheduledAppointmentAndSyncsDoctorCalendar() {
        AppointmentRequestDTO request = request(AppointmentType.CONSULTATION, "doctor-a");
        when(medicalFolderRepository.findByPatientId("patient-a")).thenReturn(List.of());
        when(appointmentRepository.findOverlappingAppointments(eq("patient-a"), any(), any())).thenReturn(List.of());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment appointment = invocation.getArgument(0);
            if (appointment.getId() == null) {
                appointment.setId(100L);
            }
            appointment.setCreatedBy("scheduler");
            return appointment;
        });
        doAnswer(invocation -> {
            Appointment appointment = invocation.getArgument(1);
            appointment.setGoogleEventId("google-100");
            return null;
        }).when(googleCalendarService).createEvent(eq("doctor-a"), any(Appointment.class));

        AppointmentResponseDTO response = service.createAppointment(request);

        assertEquals(100L, response.getId());
        assertEquals("Consultation", response.getTitle());
        assertEquals(AppointmentStatus.SCHEDULED, response.getStatus());
        assertEquals(AppointmentType.CONSULTATION, response.getType());
        assertEquals("scheduler", response.getCreatedBy());
        verify(googleCalendarService).createEvent(eq("doctor-a"), any(Appointment.class));
        verify(appointmentRepository, times(2)).save(any(Appointment.class));
    }

    @Test
    void updateAppointmentHandlesMissingLockedInvalidOverlapAndSuccessfulCalendarSync() {
        AppointmentRequestDTO request = request(AppointmentType.FOLLOW_UP, "doctor-b");
        when(appointmentRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(AppointmentNotFoundException.class, () -> service.updateAppointment(1L, request));

        Appointment completed = appointment(2L, AppointmentStatus.COMPLETED);
        when(appointmentRepository.findById(2L)).thenReturn(Optional.of(completed));
        assertThrows(InvalidAppointmentException.class, () -> service.updateAppointment(2L, request));

        Appointment editable = appointment(3L, AppointmentStatus.SCHEDULED);
        AppointmentRequestDTO invalidTimes = request(AppointmentType.FOLLOW_UP, "doctor-b");
        invalidTimes.setEndTime(invalidTimes.getStartTime());
        when(appointmentRepository.findById(3L)).thenReturn(Optional.of(editable));
        assertThrows(InvalidAppointmentException.class, () -> service.updateAppointment(3L, invalidTimes));

        Appointment otherOverlap = appointment(4L, AppointmentStatus.SCHEDULED);
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment(5L, AppointmentStatus.SCHEDULED)));
        when(appointmentRepository.findOverlappingAppointments(eq("patient-a"), any(), any()))
                .thenReturn(List.of(otherOverlap));
        assertThrows(AppointmentOverlapException.class, () -> service.updateAppointment(5L, request));

        Appointment editableSuccess = appointment(6L, AppointmentStatus.SCHEDULED);
        when(appointmentRepository.findById(6L)).thenReturn(Optional.of(editableSuccess));
        when(appointmentRepository.findOverlappingAppointments(eq("patient-a"), any(), any()))
                .thenReturn(List.of(editableSuccess));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponseDTO updated = service.updateAppointment(6L, request);

        assertEquals(6L, updated.getId());
        assertEquals("Consultation", updated.getTitle());
        assertEquals("doctor-a", updated.getDoctorId(), "updateAppointment keeps the original assigned doctor");
        verify(googleCalendarService).createEvent(eq("doctor-a"), any(Appointment.class));
    }

    @Test
    void readCancelAndListMethodsDelegateToRepositoryAndMapResponses() {
        Appointment appointment = appointment(10L, AppointmentStatus.SCHEDULED);
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(appointment)).thenReturn(appointment);

        service.cancelAppointment(10L);

        assertEquals(AppointmentStatus.CANCELLED, appointment.getStatus());
        verify(appointmentRepository).save(appointment);

        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));
        assertEquals(10L, service.getAppointmentById(10L).getId());
        when(appointmentRepository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(AppointmentNotFoundException.class, () -> service.getAppointmentById(404L));

        when(appointmentRepository.findByPatientId("patient-a")).thenReturn(List.of(appointment));
        when(appointmentRepository.findByDoctorId("doctor-a")).thenReturn(List.of(appointment));
        when(appointmentRepository.findByDateRange(any(), any())).thenReturn(List.of(appointment));
        when(appointmentRepository.findAll()).thenReturn(List.of(appointment));

        assertEquals(1, service.getAppointmentsByPatient("patient-a").size());
        assertEquals(1, service.getAppointmentsByDoctor("doctor-a").size());
        assertEquals(1, service.getAppointmentsByDateRange(LocalDateTime.now(), LocalDateTime.now().plusDays(1)).size());
        assertEquals(1, service.getAllAppointments().size());
    }

    @Test
    void createRecurringAppointmentsValidatesFrequencyCountsTimesAndOverlapsThenSavesSeries() {
        AppointmentRequestDTO request = request(AppointmentType.CONSULTATION, "doctor-a");
        when(medicalFolderRepository.findByPatientId("patient-a")).thenReturn(List.of());

        assertThrows(InvalidAppointmentException.class,
                () -> service.createRecurringAppointments(request, "DAILY", 0));
        assertThrows(InvalidAppointmentException.class,
                () -> service.createRecurringAppointments(request, "DAILY", 53));

        AppointmentRequestDTO invalidTimes = request(AppointmentType.FOLLOW_UP, "doctor-a");
        invalidTimes.setEndTime(invalidTimes.getStartTime());
        assertThrows(InvalidAppointmentException.class,
                () -> service.createRecurringAppointments(invalidTimes, "DAILY", 2));

        AppointmentRequestDTO missingDoctor = request(AppointmentType.CONSULTATION, "");
        assertThrows(InvalidAppointmentException.class,
                () -> service.createRecurringAppointments(missingDoctor, "DAILY", 2));

        assertThrows(InvalidAppointmentException.class,
                () -> service.createRecurringAppointments(request, "YEARLY", 2));

        when(appointmentRepository.findOverlappingAppointments(eq("patient-a"), any(), any()))
                .thenReturn(List.of(appointment(200L, AppointmentStatus.SCHEDULED)));
        assertThrows(AppointmentOverlapException.class,
                () -> service.createRecurringAppointments(request, "WEEKLY", 2));

        when(appointmentRepository.findOverlappingAppointments(eq("patient-a"), any(), any())).thenReturn(List.of());
        when(appointmentRepository.findOverlappingAppointmentsForDoctor(eq("doctor-a"), any(), any()))
                .thenReturn(List.of(appointment(201L, AppointmentStatus.SCHEDULED)));
        assertThrows(AppointmentOverlapException.class,
                () -> service.createRecurringAppointments(request, "MONTHLY", 2));

        when(appointmentRepository.findOverlappingAppointmentsForDoctor(eq("doctor-a"), any(), any())).thenReturn(List.of());
        when(appointmentRepository.saveAll(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<Appointment> appointments = invocation.getArgument(0);
            for (int i = 0; i < appointments.size(); i++) {
                appointments.get(i).setId(300L + i);
            }
            return appointments;
        });

        List<AppointmentResponseDTO> responses = service.createRecurringAppointments(request, "DAILY", 3);

        assertEquals(3, responses.size());
        assertEquals(300L, responses.get(0).getId());
        assertEquals(request.getStartTime().plusDays(2), responses.get(2).getStartTime());
        ArgumentCaptor<List<Appointment>> savedSeries = ArgumentCaptor.forClass(List.class);
        verify(appointmentRepository).saveAll(savedSeries.capture());
        assertEquals(AppointmentStatus.SCHEDULED, savedSeries.getValue().get(0).getStatus());
    }

    @Test
    void markNoShowAndCompletedUpdateStatusAndRecalculateAttendance() {
        Appointment noShow = appointment(50L, AppointmentStatus.SCHEDULED);
        when(appointmentRepository.findById(50L)).thenReturn(Optional.of(noShow));
        when(appointmentRepository.save(noShow)).thenReturn(noShow);

        AppointmentResponseDTO noShowResponse = service.markAppointmentNoShow(50L);

        assertEquals(AppointmentStatus.NO_SHOW, noShowResponse.getStatus());
        verify(attendanceMonitoringService).recalculateForPatient("patient-a");

        Appointment completed = appointment(51L, AppointmentStatus.SCHEDULED);
        when(appointmentRepository.findById(51L)).thenReturn(Optional.of(completed));
        when(appointmentRepository.save(completed)).thenReturn(completed);

        AppointmentResponseDTO completedResponse = service.markAppointmentCompleted(51L);

        assertEquals(AppointmentStatus.COMPLETED, completedResponse.getStatus());
        verify(attendanceMonitoringService, times(2)).recalculateForPatient("patient-a");

        Appointment cancelled = appointment(52L, AppointmentStatus.CANCELLED);
        when(appointmentRepository.findById(52L)).thenReturn(Optional.of(cancelled));
        assertThrows(InvalidAppointmentException.class, () -> service.markAppointmentNoShow(52L));
        assertThrows(InvalidAppointmentException.class, () -> service.markAppointmentCompleted(52L));
    }

    private static AppointmentRequestDTO request(AppointmentType type, String doctorId) {
        AppointmentRequestDTO request = new AppointmentRequestDTO();
        LocalDateTime start = LocalDateTime.now().plusDays(5).withNano(0);
        request.setTitle("Consultation");
        request.setDescription("Routine appointment");
        request.setPatientId("patient-a");
        request.setDoctorId(doctorId);
        request.setStartTime(start);
        request.setEndTime(start.plusMinutes(45));
        request.setType(type);
        request.setNotes("Bring latest scans");
        return request;
    }

    private static Appointment appointment(Long id, AppointmentStatus status) {
        Appointment appointment = new Appointment();
        LocalDateTime start = LocalDateTime.now().plusDays(7).withNano(0);
        appointment.setId(id);
        appointment.setTitle("Existing appointment");
        appointment.setDescription("Existing description");
        appointment.setPatientId("patient-a");
        appointment.setDoctorId("doctor-a");
        appointment.setStartTime(start);
        appointment.setEndTime(start.plusMinutes(30));
        appointment.setStatus(status);
        appointment.setType(AppointmentType.FOLLOW_UP);
        appointment.setNotes("Existing notes");
        appointment.setCreatedAt(start.minusDays(1));
        appointment.setCreatedBy("doctor-a");
        return appointment;
    }
}
