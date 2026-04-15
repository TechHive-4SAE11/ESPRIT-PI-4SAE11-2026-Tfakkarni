package org.techhive.medicalservice.repository;

import org.techhive.medicalservice.entity.Appointment;
import org.techhive.medicalservice.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

       List<Appointment> findByPatientId(String patientId);

       List<Appointment> findByPatientIdAndStartTimeBeforeOrderByStartTimeDesc(String patientId,
                     LocalDateTime before);

       long countByPatientIdAndStatus(String patientId, AppointmentStatus status);

       List<Appointment> findByDoctorId(String doctorId);

       long countByDoctorIdAndGoogleEventIdIsNotNull(String doctorId);

       @Query("SELECT a FROM Appointment a WHERE a.patientId = :patientId AND a.startTime BETWEEN :start AND :end")
       List<Appointment> findByPatientIdAndDateRange(@Param("patientId") String patientId,
                     @Param("start") LocalDateTime start,
                     @Param("end") LocalDateTime end);

       @Query("SELECT a FROM Appointment a WHERE a.startTime BETWEEN :start AND :end")
       List<Appointment> findByDateRange(@Param("start") LocalDateTime start,
                     @Param("end") LocalDateTime end);

       // Vérifier les chevauchements (exclut les rendez-vous annulés)
       @Query("SELECT a FROM Appointment a WHERE a.patientId = :patientId " +
                     "AND a.status NOT IN ('CANCELLED', 'NO_SHOW') " +
                     "AND ((a.startTime BETWEEN :start AND :end) OR (a.endTime BETWEEN :start AND :end) " +
                     "OR (:start BETWEEN a.startTime AND a.endTime))")
       List<Appointment> findOverlappingAppointments(@Param("patientId") String patientId,
                     @Param("start") LocalDateTime start,
                     @Param("end") LocalDateTime end);

       @Query("SELECT a FROM Appointment a WHERE a.doctorId = :doctorId " +
                     "AND a.status NOT IN ('CANCELLED', 'NO_SHOW') " +
                     "AND ((a.startTime BETWEEN :start AND :end) OR (a.endTime BETWEEN :start AND :end) " +
                     "OR (:start BETWEEN a.startTime AND a.endTime))")
       List<Appointment> findOverlappingAppointmentsForDoctor(
                     @Param("doctorId") String doctorId,
                     @Param("start") LocalDateTime start,
                     @Param("end") LocalDateTime end);
}
