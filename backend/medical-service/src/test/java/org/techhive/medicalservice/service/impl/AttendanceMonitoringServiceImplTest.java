package org.techhive.medicalservice.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.medicalservice.entity.Appointment;
import org.techhive.medicalservice.entity.AppointmentStatus;
import org.techhive.medicalservice.entity.AttendanceRiskLevel;
import org.techhive.medicalservice.entity.MedicalFolder;
import org.techhive.medicalservice.exception.ResourceNotFoundException;
import org.techhive.medicalservice.repository.AppointmentRepository;
import org.techhive.medicalservice.repository.MedicalFolderRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceMonitoringServiceImplTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private MedicalFolderRepository medicalFolderRepository;

    private AttendanceMonitoringServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AttendanceMonitoringServiceImpl(appointmentRepository, medicalFolderRepository);
    }

    @Test
    void recalculateReturnsWithoutRepositoryWritesForBlankOrMissingFolder() {
        service.recalculateForPatient(null);
        service.recalculateForPatient("   ");
        verifyNoInteractions(appointmentRepository, medicalFolderRepository);

        when(medicalFolderRepository.findByPatientId("patient-a")).thenReturn(List.of());

        service.recalculateForPatient("patient-a");

        verify(medicalFolderRepository).findByPatientId("patient-a");
        verifyNoInteractions(appointmentRepository);
        verify(medicalFolderRepository, never()).save(any());
    }

    @Test
    void recalculateSetsNoneWhenLatestAppointmentBreaksNoShowStreakAndClearsOverride() {
        MedicalFolder folder = folder("patient-a");
        folder.setAttendanceRestrictionOverridden(true);
        folder.setBookingRestricted(true);
        folder.setManualReviewRequired(true);
        when(medicalFolderRepository.findByPatientId("patient-a")).thenReturn(List.of(folder));
        when(appointmentRepository.findByPatientIdAndStartTimeBeforeOrderByStartTimeDesc(eq("patient-a"), any(LocalDateTime.class)))
                .thenReturn(List.of(appointment(AppointmentStatus.COMPLETED), appointment(AppointmentStatus.NO_SHOW)));
        when(appointmentRepository.countByPatientIdAndStatus("patient-a", AppointmentStatus.NO_SHOW)).thenReturn(1L);

        service.recalculateForPatient("patient-a");

        assertEquals(0, folder.getConsecutiveNoShows());
        assertEquals(1, folder.getTotalNoShows());
        assertFalse(folder.isAttendanceRestrictionOverridden());
        assertFalse(folder.isBookingRestricted());
        assertFalse(folder.isManualReviewRequired());
        assertEquals(AttendanceRiskLevel.NONE, folder.getAttendanceRiskLevel());
        assertNull(folder.getRestrictionReason());
        verify(medicalFolderRepository).save(folder);
    }

    @Test
    void recalculateSetsWarningForTwoConsecutiveNoShows() {
        MedicalFolder folder = folder("patient-a");
        when(medicalFolderRepository.findByPatientId("patient-a")).thenReturn(List.of(folder));
        when(appointmentRepository.findByPatientIdAndStartTimeBeforeOrderByStartTimeDesc(eq("patient-a"), any(LocalDateTime.class)))
                .thenReturn(List.of(
                        appointment(AppointmentStatus.NO_SHOW),
                        appointment(AppointmentStatus.NO_SHOW),
                        appointment(AppointmentStatus.COMPLETED)));
        when(appointmentRepository.countByPatientIdAndStatus("patient-a", AppointmentStatus.NO_SHOW)).thenReturn(2L);

        service.recalculateForPatient("patient-a");

        assertEquals(2, folder.getConsecutiveNoShows());
        assertEquals(2, folder.getTotalNoShows());
        assertFalse(folder.isBookingRestricted());
        assertFalse(folder.isManualReviewRequired());
        assertEquals(AttendanceRiskLevel.WARNING, folder.getAttendanceRiskLevel());
        assertTrue(folder.getRestrictionReason().contains("Two consecutive no-shows"));
        verify(medicalFolderRepository).save(folder);
    }

    @Test
    void recalculateRestrictsForThreeConsecutiveNoShowsAndClampsTotalCount() {
        MedicalFolder folder = folder("patient-a");
        when(medicalFolderRepository.findByPatientId("patient-a")).thenReturn(List.of(folder));
        when(appointmentRepository.findByPatientIdAndStartTimeBeforeOrderByStartTimeDesc(eq("patient-a"), any(LocalDateTime.class)))
                .thenReturn(List.of(
                        appointment(AppointmentStatus.NO_SHOW),
                        appointment(AppointmentStatus.NO_SHOW),
                        appointment(AppointmentStatus.NO_SHOW)));
        when(appointmentRepository.countByPatientIdAndStatus("patient-a", AppointmentStatus.NO_SHOW))
                .thenReturn((long) Integer.MAX_VALUE + 7L);

        service.recalculateForPatient("patient-a");

        assertEquals(3, folder.getConsecutiveNoShows());
        assertEquals(Integer.MAX_VALUE, folder.getTotalNoShows());
        assertTrue(folder.isBookingRestricted());
        assertTrue(folder.isManualReviewRequired());
        assertEquals(AttendanceRiskLevel.RESTRICTED, folder.getAttendanceRiskLevel());
        assertTrue(folder.getRestrictionReason().contains("Three or more consecutive missed appointments"));
        verify(medicalFolderRepository).save(folder);
    }

    @Test
    void recalculateHonorsManualOverrideWhileStreakRemainsRestricted() {
        MedicalFolder folder = folder("patient-a");
        folder.setAttendanceRestrictionOverridden(true);
        when(medicalFolderRepository.findByPatientId("patient-a")).thenReturn(List.of(folder));
        when(appointmentRepository.findByPatientIdAndStartTimeBeforeOrderByStartTimeDesc(eq("patient-a"), any(LocalDateTime.class)))
                .thenReturn(List.of(
                        appointment(AppointmentStatus.NO_SHOW),
                        appointment(AppointmentStatus.NO_SHOW),
                        appointment(AppointmentStatus.NO_SHOW),
                        appointment(AppointmentStatus.NO_SHOW)));
        when(appointmentRepository.countByPatientIdAndStatus("patient-a", AppointmentStatus.NO_SHOW)).thenReturn(4L);

        service.recalculateForPatient("patient-a");

        assertEquals(4, folder.getConsecutiveNoShows());
        assertEquals(4, folder.getTotalNoShows());
        assertTrue(folder.isAttendanceRestrictionOverridden());
        assertFalse(folder.isBookingRestricted());
        assertFalse(folder.isManualReviewRequired());
        assertEquals(AttendanceRiskLevel.WARNING, folder.getAttendanceRiskLevel());
        assertTrue(folder.getRestrictionReason().contains("lifted after review"));
        verify(medicalFolderRepository).save(folder);
    }

    @Test
    void clearBookingRestrictionAfterReviewMarksOverrideAndRecalculatesPatient() {
        MedicalFolder folder = folder("patient-a");
        folder.setId(77L);
        when(medicalFolderRepository.findById(77L)).thenReturn(Optional.of(folder));
        when(medicalFolderRepository.findByPatientId("patient-a")).thenReturn(List.of(folder));
        when(appointmentRepository.findByPatientIdAndStartTimeBeforeOrderByStartTimeDesc(eq("patient-a"), any(LocalDateTime.class)))
                .thenReturn(List.of(
                        appointment(AppointmentStatus.NO_SHOW),
                        appointment(AppointmentStatus.NO_SHOW),
                        appointment(AppointmentStatus.NO_SHOW)));
        when(appointmentRepository.countByPatientIdAndStatus("patient-a", AppointmentStatus.NO_SHOW)).thenReturn(3L);

        service.clearBookingRestrictionAfterReview(77L);

        assertTrue(folder.isAttendanceRestrictionOverridden());
        assertFalse(folder.isBookingRestricted());
        assertFalse(folder.isManualReviewRequired());
        assertEquals(AttendanceRiskLevel.WARNING, folder.getAttendanceRiskLevel());
        assertTrue(folder.getRestrictionReason().contains("lifted after review"));
        verify(medicalFolderRepository, times(2)).save(folder);
    }

    @Test
    void clearBookingRestrictionAfterReviewThrowsWhenFolderMissing() {
        when(medicalFolderRepository.findById(404L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.clearBookingRestrictionAfterReview(404L));

        assertEquals("Medical folder not found with id: 404", ex.getMessage());
        verify(medicalFolderRepository, never()).save(any());
        verifyNoInteractions(appointmentRepository);
    }

    private static MedicalFolder folder(String patientId) {
        return MedicalFolder.builder()
                .id(10L)
                .patientId(patientId)
                .doctorId("doctor-a")
                .bookingRestricted(false)
                .manualReviewRequired(false)
                .attendanceRiskLevel(AttendanceRiskLevel.NONE)
                .build();
    }

    private static Appointment appointment(AppointmentStatus status) {
        Appointment appointment = new Appointment();
        appointment.setStatus(status);
        return appointment;
    }
}
