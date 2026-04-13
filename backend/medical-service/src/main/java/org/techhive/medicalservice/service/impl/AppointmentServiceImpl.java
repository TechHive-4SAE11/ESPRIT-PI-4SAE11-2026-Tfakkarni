package org.techhive.medicalservice.service.impl;

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
import org.techhive.medicalservice.service.AppointmentService;
import org.techhive.medicalservice.service.AttendanceMonitoringService;
import org.techhive.medicalservice.service.GoogleCalendarService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final MedicalFolderRepository medicalFolderRepository;
    private final AttendanceMonitoringService attendanceMonitoringService;
    private final GoogleCalendarService googleCalendarService;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
            MedicalFolderRepository medicalFolderRepository,
            AttendanceMonitoringService attendanceMonitoringService,
            GoogleCalendarService googleCalendarService) {
        this.appointmentRepository = appointmentRepository;
        this.medicalFolderRepository = medicalFolderRepository;
        this.attendanceMonitoringService = attendanceMonitoringService;
        this.googleCalendarService = googleCalendarService;
    }

    @Override
    public AppointmentResponseDTO createAppointment(AppointmentRequestDTO requestDTO) {
        assertPatientMayBook(requestDTO.getPatientId());

        // Contrôle de saisie 1: endTime après startTime
        if (requestDTO.getEndTime().isBefore(requestDTO.getStartTime()) ||
                requestDTO.getEndTime().isEqual(requestDTO.getStartTime())) {
            throw new InvalidAppointmentException("La date de fin doit être après la date de début");
        }

        // Contrôle de saisie 2: doctorId requis pour rendez-vous médical
        if (requestDTO.getType() == AppointmentType.CONSULTATION &&
                (requestDTO.getDoctorId() == null || requestDTO.getDoctorId().trim().isEmpty())) {
            throw new InvalidAppointmentException("Un rendez-vous de consultation nécessite un médecin");
        }

        // Contrôle de saisie 3: vérification des chevauchements
        List<Appointment> overlapping = appointmentRepository.findOverlappingAppointments(
                requestDTO.getPatientId(),
                requestDTO.getStartTime(),
                requestDTO.getEndTime());

        if (!overlapping.isEmpty()) {
            throw new AppointmentOverlapException("Le patient a déjà un rendez-vous sur cette plage horaire");
        }

        // Conversion et sauvegarde
        Appointment appointment = new Appointment();
        appointment.setTitle(requestDTO.getTitle());
        appointment.setDescription(requestDTO.getDescription());
        appointment.setPatientId(requestDTO.getPatientId());
        appointment.setDoctorId(requestDTO.getDoctorId());
        appointment.setStartTime(requestDTO.getStartTime());
        appointment.setEndTime(requestDTO.getEndTime());
        appointment.setType(requestDTO.getType());
        appointment.setNotes(requestDTO.getNotes());
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        Appointment savedAppointment = appointmentRepository.save(appointment);

        // Sync to Google Calendar if doctor is assigned
        if (savedAppointment.getDoctorId() != null) {
            googleCalendarService.createEvent(savedAppointment.getDoctorId(), savedAppointment);
            // Re-save in case googleEventId has been set
            savedAppointment = appointmentRepository.save(savedAppointment);
        }

        return mapToResponseDTO(savedAppointment);
    }

    @Override
    public AppointmentResponseDTO updateAppointment(Long id, AppointmentRequestDTO requestDTO) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException("Rendez-vous non trouvé avec l'id: " + id));

        // Vérifier que le rendez-vous n'est pas déjà complété ou annulé
        if (appointment.getStatus() == AppointmentStatus.COMPLETED ||
                appointment.getStatus() == AppointmentStatus.CANCELLED ||
                appointment.getStatus() == AppointmentStatus.NO_SHOW) {
            throw new InvalidAppointmentException("Impossible de modifier un rendez-vous " + appointment.getStatus());
        }

        // Contrôle de saisie: endTime après startTime
        if (requestDTO.getEndTime().isBefore(requestDTO.getStartTime()) ||
                requestDTO.getEndTime().isEqual(requestDTO.getStartTime())) {
            throw new InvalidAppointmentException("La date de fin doit être après la date de début");
        }

        // Vérification des chevauchements (exclure ce rendez-vous)
        List<Appointment> overlapping = appointmentRepository.findOverlappingAppointments(
                requestDTO.getPatientId(),
                requestDTO.getStartTime(),
                requestDTO.getEndTime()).stream()
                .filter(a -> !a.getId().equals(id))
                .collect(Collectors.toList());

        if (!overlapping.isEmpty()) {
            throw new AppointmentOverlapException("Le patient a déjà un rendez-vous sur cette plage horaire");
        }

        // Mise à jour
        appointment.setTitle(requestDTO.getTitle());
        appointment.setDescription(requestDTO.getDescription());
        appointment.setStartTime(requestDTO.getStartTime());
        appointment.setEndTime(requestDTO.getEndTime());
        appointment.setNotes(requestDTO.getNotes());

        Appointment updatedAppointment = appointmentRepository.save(appointment);

        // Sync to Google Calendar if doctor is assigned
        if (updatedAppointment.getDoctorId() != null) {
            googleCalendarService.createEvent(updatedAppointment.getDoctorId(), updatedAppointment);
            updatedAppointment = appointmentRepository.save(updatedAppointment);
        }

        return mapToResponseDTO(updatedAppointment);
    }

    @Override
    public void cancelAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException("Rendez-vous non trouvé avec l'id: " + id));

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
    }

    @Override
    public AppointmentResponseDTO getAppointmentById(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException("Rendez-vous non trouvé avec l'id: " + id));
        return mapToResponseDTO(appointment);
    }

    @Override
    public List<AppointmentResponseDTO> getAppointmentsByPatient(String patientId) {
        return appointmentRepository.findByPatientId(patientId)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AppointmentResponseDTO> getAppointmentsByDoctor(String doctorId) {
        return appointmentRepository.findByDoctorId(doctorId)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AppointmentResponseDTO> getAppointmentsByDateRange(LocalDateTime start, LocalDateTime end) {
        return appointmentRepository.findByDateRange(start, end)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AppointmentResponseDTO> getAllAppointments() {
        return appointmentRepository.findAll()
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AppointmentResponseDTO> createRecurringAppointments(AppointmentRequestDTO requestDTO,
            String frequency,
            int numberOfOccurrences) {
        assertPatientMayBook(requestDTO.getPatientId());

        if (numberOfOccurrences < 1) {
            throw new InvalidAppointmentException("Le nombre d'occurrences doit être au moins 1");
        }
        if (numberOfOccurrences > 52) {
            throw new InvalidAppointmentException("Le nombre d'occurrences ne peut pas dépasser 52");
        }

        if (requestDTO.getEndTime().isBefore(requestDTO.getStartTime()) ||
                requestDTO.getEndTime().isEqual(requestDTO.getStartTime())) {
            throw new InvalidAppointmentException("La date de fin doit être après la date de début");
        }

        if (requestDTO.getType() == AppointmentType.CONSULTATION &&
                (requestDTO.getDoctorId() == null || requestDTO.getDoctorId().trim().isEmpty())) {
            throw new InvalidAppointmentException("Un rendez-vous de consultation nécessite un médecin");
        }

        RecurrenceFrequency recurrenceFrequency = RecurrenceFrequency.fromString(frequency);

        LocalDateTime baseStart = requestDTO.getStartTime();
        LocalDateTime baseEnd = requestDTO.getEndTime();

        List<Appointment> appointmentsToCreate = new ArrayList<>();

        for (int occurrenceIndex = 0; occurrenceIndex < numberOfOccurrences; occurrenceIndex++) {
            LocalDateTime occurrenceStart = recurrenceFrequency.addTo(baseStart, occurrenceIndex);
            LocalDateTime occurrenceEnd = recurrenceFrequency.addTo(baseEnd, occurrenceIndex);

            List<Appointment> patientOverlaps = appointmentRepository.findOverlappingAppointments(
                    requestDTO.getPatientId(),
                    occurrenceStart,
                    occurrenceEnd);

            if (!patientOverlaps.isEmpty()) {
                throw new AppointmentOverlapException(
                        "Le patient a déjà un rendez-vous sur la plage horaire de l'occurrence "
                                + (occurrenceIndex + 1));
            }

            if (requestDTO.getDoctorId() != null && !requestDTO.getDoctorId().trim().isEmpty()) {
                List<Appointment> doctorOverlaps = appointmentRepository.findOverlappingAppointmentsForDoctor(
                        requestDTO.getDoctorId(),
                        occurrenceStart,
                        occurrenceEnd);

                if (!doctorOverlaps.isEmpty()) {
                    throw new AppointmentOverlapException(
                            "Le médecin a déjà un rendez-vous sur la plage horaire de l'occurrence "
                                    + (occurrenceIndex + 1));
                }
            }

            Appointment appointment = new Appointment();
            appointment.setTitle(requestDTO.getTitle());
            appointment.setDescription(requestDTO.getDescription());
            appointment.setPatientId(requestDTO.getPatientId());
            appointment.setDoctorId(requestDTO.getDoctorId());
            appointment.setStartTime(occurrenceStart);
            appointment.setEndTime(occurrenceEnd);
            appointment.setType(requestDTO.getType());
            appointment.setNotes(requestDTO.getNotes());
            appointment.setStatus(AppointmentStatus.SCHEDULED);

            appointmentsToCreate.add(appointment);
        }

        List<Appointment> savedAppointments = appointmentRepository.saveAll(appointmentsToCreate);

        return savedAppointments.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AppointmentResponseDTO markAppointmentNoShow(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException("Rendez-vous non trouvé avec l'id: " + appointmentId));
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new InvalidAppointmentException("Impossible de marquer no-show un rendez-vous annulé");
        }
        appointment.setStatus(AppointmentStatus.NO_SHOW);
        Appointment saved = appointmentRepository.save(appointment);
        attendanceMonitoringService.recalculateForPatient(appointment.getPatientId());
        return mapToResponseDTO(saved);
    }

    @Override
    public AppointmentResponseDTO markAppointmentCompleted(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException("Rendez-vous non trouvé avec l'id: " + appointmentId));
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new InvalidAppointmentException("Impossible de compléter un rendez-vous annulé");
        }
        appointment.setStatus(AppointmentStatus.COMPLETED);
        Appointment saved = appointmentRepository.save(appointment);
        attendanceMonitoringService.recalculateForPatient(appointment.getPatientId());
        return mapToResponseDTO(saved);
    }

    private void assertPatientMayBook(String patientId) {
        if (patientId == null || patientId.isBlank()) {
            return;
        }
        for (MedicalFolder folder : medicalFolderRepository.findByPatientId(patientId)) {
            if (folder.isBookingRestricted()) {
                String reason = folder.getRestrictionReason() != null ? folder.getRestrictionReason()
                        : "Réservation temporairement restreinte (absences répétées).";
                throw new BookingRestrictedException(reason);
            }
        }
    }

    private AppointmentResponseDTO mapToResponseDTO(Appointment appointment) {
        AppointmentResponseDTO dto = new AppointmentResponseDTO();
        dto.setId(appointment.getId());
        dto.setTitle(appointment.getTitle());
        dto.setDescription(appointment.getDescription());
        dto.setPatientId(appointment.getPatientId());
        dto.setDoctorId(appointment.getDoctorId());
        dto.setStartTime(appointment.getStartTime());
        dto.setEndTime(appointment.getEndTime());
        dto.setStatus(appointment.getStatus());
        dto.setType(appointment.getType());
        dto.setNotes(appointment.getNotes());
        dto.setCreatedAt(appointment.getCreatedAt());
        dto.setCreatedBy(appointment.getCreatedBy());
        return dto;
    }

    private enum RecurrenceFrequency {
        DAILY,
        WEEKLY,
        MONTHLY;

        public LocalDateTime addTo(LocalDateTime base, int occurrenceIndex) {
            return switch (this) {
                case DAILY -> base.plusDays(occurrenceIndex);
                case WEEKLY -> base.plusWeeks(occurrenceIndex);
                case MONTHLY -> base.plusMonths(occurrenceIndex);
            };
        }

        public static RecurrenceFrequency fromString(String value) {
            if (value == null || value.trim().isEmpty()) {
                throw new InvalidAppointmentException("La fréquence de récurrence est obligatoire");
            }
            try {
                return RecurrenceFrequency.valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new InvalidAppointmentException(
                        "Fréquence de récurrence invalide. Valeurs possibles: DAILY, WEEKLY, MONTHLY");
            }
        }
    }
}
