package org.techhive.medicalservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.medicalservice.dto.DashboardStatsDTO;
import org.techhive.medicalservice.dto.PatientRiskDTO;
import org.techhive.medicalservice.dto.PredictionDTO;
import org.techhive.medicalservice.entity.Appointment;
import org.techhive.medicalservice.entity.AppointmentStatus;
import org.techhive.medicalservice.entity.PredictionResult;
import org.techhive.medicalservice.repository.AppointmentRepository;
import org.techhive.medicalservice.repository.PredictionResultRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PredictionServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PredictionResultRepository predictionResultRepository;

    private PredictionService service;

    @BeforeEach
    void setUp() {
        service = new PredictionService(appointmentRepository, predictionResultRepository);
    }

    @Test
    void predictForAppointment_throwsWhenAppointmentMissing() {
        when(appointmentRepository.findById(404L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.predictForAppointment(404L));

        assertEquals("Appointment not found", ex.getMessage());
        verifyNoInteractions(predictionResultRepository);
    }

    @Test
    void predictForAppointment_noPastAppointmentsSavesGreenPredictionWithZeroFactors() {
        Appointment current = appointment(10L, "patient-a", "doctor-a",
                LocalDateTime.now().plusDays(4), AppointmentStatus.SCHEDULED, "Consultation");
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(current));
        when(appointmentRepository.findByPatientId("patient-a")).thenReturn(List.of(current));

        PredictionDTO prediction = service.predictForAppointment(10L);

        assertEquals(0, prediction.getRiskScore());
        assertEquals("GREEN", prediction.getRiskLevel());
        assertEquals("Aucun risque particulier", prediction.getRecommendation());
        assertEquals(0, prediction.getFactors().get("historicalRate"));
        assertEquals(0, prediction.getFactors().get("sameDayOfWeek"));
        assertEquals(0, prediction.getFactors().get("lastApptCancelledRecently"));
        assertEquals(0, prediction.getFactors().get("sameTime"));
        assertEquals(0, prediction.getFactors().get("sameDoctor"));
        assertEquals(0, prediction.getFactors().get("delaySinceLast"));

        ArgumentCaptor<PredictionResult> saved = ArgumentCaptor.forClass(PredictionResult.class);
        verify(predictionResultRepository).save(saved.capture());
        assertEquals(10L, saved.getValue().getAppointmentId());
        assertEquals("patient-a", saved.getValue().getPatientId());
        assertEquals("GREEN", saved.getValue().getRiskLevel());
    }

    @Test
    void predictForAppointment_recentSameDoctorSameTimeCancellationsProduceRedPrediction() {
        LocalDateTime currentStart = LocalDateTime.now().plusDays(14).withHour(9).withMinute(0).withSecond(0).withNano(0);
        Appointment current = appointment(20L, "patient-b", "doctor-b", currentStart, AppointmentStatus.SCHEDULED, "Follow-up");
        Appointment recentCancelled = appointment(18L, "patient-b", "doctor-b", currentStart.minusDays(7), AppointmentStatus.CANCELLED, "Recent cancelled");
        Appointment olderCancelled = appointment(17L, "patient-b", "doctor-b", currentStart.minusDays(35), AppointmentStatus.CANCELLED, "Older cancelled");
        when(appointmentRepository.findById(20L)).thenReturn(Optional.of(current));
        when(appointmentRepository.findByPatientId("patient-b")).thenReturn(List.of(current, olderCancelled, recentCancelled));

        PredictionDTO prediction = service.predictForAppointment(20L);

        assertEquals(95, prediction.getRiskScore());
        assertEquals("RED", prediction.getRiskLevel());
        assertEquals("Confirmation manuelle exigée ou double surréservation", prediction.getRecommendation());
        assertEquals(35, prediction.getFactors().get("historicalRate"));
        assertEquals(20, prediction.getFactors().get("sameDayOfWeek"));
        assertEquals(20, prediction.getFactors().get("lastApptCancelledRecently"));
        assertEquals(10, prediction.getFactors().get("sameTime"));
        assertEquals(10, prediction.getFactors().get("sameDoctor"));
        assertEquals(0, prediction.getFactors().get("delaySinceLast"));
        verify(predictionResultRepository).save(any(PredictionResult.class));
    }

    @Test
    void getDashboardStatsAggregatesCancellationRatesHighRiskPatientsAndDoctorRates() {
        LocalDateTime now = LocalDateTime.now();
        Appointment cancelledThisMonth = appointment(30L, "patient-c", "doctor-c", now.minusDays(1), AppointmentStatus.CANCELLED, "Cancelled this month");
        Appointment completedThisMonth = appointment(31L, "patient-d", "doctor-c", now.minusDays(2), AppointmentStatus.COMPLETED, "Completed this month");
        Appointment cancelledEarlier = appointment(32L, "patient-e", "doctor-d", now.minusMonths(2), AppointmentStatus.CANCELLED, "Cancelled earlier");
        Appointment highRiskAppointment = appointment(33L, "patient-f", "doctor-e", now.plusDays(2), AppointmentStatus.SCHEDULED, "High risk upcoming");
        PredictionResult highRisk = PredictionResult.builder()
                .appointmentId(33L)
                .patientId("patient-f")
                .riskScore(80)
                .riskLevel("RED")
                .recommendation("Call now")
                .createdAt(now)
                .build();
        when(appointmentRepository.findAll()).thenReturn(List.of(cancelledThisMonth, completedThisMonth, cancelledEarlier));
        when(predictionResultRepository.findByRiskScoreGreaterThanEqual(70)).thenReturn(List.of(highRisk));
        when(appointmentRepository.findById(33L)).thenReturn(Optional.of(highRiskAppointment));
        when(appointmentRepository.findByDateRange(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());

        DashboardStatsDTO stats = service.getDashboardStats();

        assertEquals(3L, stats.getTotalAppointments());
        assertEquals(66.66666666666666, stats.getGlobalNoShowRate(), 0.001);
        assertEquals(50.0, stats.getMonthlyNoShowRate(), 0.001);
        assertFalse(stats.getCancellationsByDay().isEmpty());
        assertEquals(50.0, stats.getNoShowRateByDoctor().get("Dr. doctor-c"), 0.001);
        assertEquals(100.0, stats.getNoShowRateByDoctor().get("Dr. doctor-d"), 0.001);
        assertEquals(1, stats.getHighRiskPatients().size());
        PatientRiskDTO patient = stats.getHighRiskPatients().get(0);
        assertEquals(33L, patient.getAppointmentId());
        assertEquals("patient-f", patient.getPatientId());
        assertEquals("RED", patient.getRiskLevel());
        assertTrue(stats.getUpcomingAppointments().isEmpty());
    }

    @Test
    void getUpcomingAppointmentsWithRiskUsesExistingPredictionOrGeneratesMissingPrediction() {
        LocalDateTime now = LocalDateTime.now();
        Appointment existingPredictionAppointment = appointment(40L, "patient-g", "doctor-g", now.plusDays(2), AppointmentStatus.SCHEDULED, "Existing prediction");
        Appointment missingPredictionAppointment = appointment(41L, "patient-h", "doctor-h", now.plusDays(3), AppointmentStatus.SCHEDULED, "Generated prediction");
        PredictionResult existingPrediction = PredictionResult.builder()
                .appointmentId(40L)
                .patientId("patient-g")
                .riskScore(55)
                .riskLevel("ORANGE")
                .recommendation("Rappel téléphonique conseillé")
                .createdAt(now.minusHours(2))
                .build();
        when(appointmentRepository.findByDateRange(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(existingPredictionAppointment, missingPredictionAppointment));
        when(predictionResultRepository.findTopByAppointmentIdOrderByCreatedAtDesc(40L))
                .thenReturn(Optional.of(existingPrediction));
        when(predictionResultRepository.findTopByAppointmentIdOrderByCreatedAtDesc(41L))
                .thenReturn(Optional.empty());
        when(appointmentRepository.findById(41L)).thenReturn(Optional.of(missingPredictionAppointment));
        when(appointmentRepository.findByPatientId("patient-h")).thenReturn(List.of(missingPredictionAppointment));

        List<PatientRiskDTO> upcoming = service.getUpcomingAppointmentsWithRisk();

        assertEquals(2, upcoming.size());
        assertEquals(40L, upcoming.get(0).getAppointmentId());
        assertEquals(55, upcoming.get(0).getRiskScore());
        assertEquals("ORANGE", upcoming.get(0).getRiskLevel());
        assertEquals(41L, upcoming.get(1).getAppointmentId());
        assertEquals(0, upcoming.get(1).getRiskScore());
        assertEquals("GREEN", upcoming.get(1).getRiskLevel());
        verify(predictionResultRepository).save(any(PredictionResult.class));
    }

    private static Appointment appointment(Long id, String patientId, String doctorId,
                                           LocalDateTime start, AppointmentStatus status, String title) {
        Appointment appointment = new Appointment();
        appointment.setId(id);
        appointment.setPatientId(patientId);
        appointment.setDoctorId(doctorId);
        appointment.setStartTime(start);
        appointment.setEndTime(start.plusMinutes(30));
        appointment.setStatus(status);
        appointment.setTitle(title);
        return appointment;
    }
}
