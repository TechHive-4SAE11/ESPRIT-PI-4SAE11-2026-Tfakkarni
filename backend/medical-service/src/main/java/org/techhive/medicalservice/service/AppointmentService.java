package org.techhive.medicalservice.service;

import org.techhive.medicalservice.dto.AppointmentRequestDTO;
import org.techhive.medicalservice.dto.AppointmentResponseDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentService {
    AppointmentResponseDTO createAppointment(AppointmentRequestDTO requestDTO);
    AppointmentResponseDTO updateAppointment(Long id, AppointmentRequestDTO requestDTO);
    void cancelAppointment(Long id);
    AppointmentResponseDTO getAppointmentById(Long id);
    List<AppointmentResponseDTO> getAppointmentsByPatient(String patientId);
    List<AppointmentResponseDTO> getAppointmentsByDoctor(String doctorId);
    List<AppointmentResponseDTO> getAppointmentsByDateRange(LocalDateTime start, LocalDateTime end);
    List<AppointmentResponseDTO> getAllAppointments();
    
    /**
     * Crée une série de rendez-vous récurrents à partir d'une demande de base.
     *
     * @param requestDTO           les informations du rendez-vous de base
     * @param frequency            la fréquence de récurrence (DAILY, WEEKLY, MONTHLY)
     * @param numberOfOccurrences  nombre d'occurrences à créer (>= 1)
     * @return la liste des rendez-vous créés
     */
    List<AppointmentResponseDTO> createRecurringAppointments(AppointmentRequestDTO requestDTO,
                                                             String frequency,
                                                             int numberOfOccurrences);
}

