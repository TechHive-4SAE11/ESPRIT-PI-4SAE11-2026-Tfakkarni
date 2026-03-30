package org.techhive.trackingservice.repository;

import org.techhive.trackingservice.entity.MedicalMeeting;
import org.techhive.trackingservice.entity.MeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalMeetingRepository extends JpaRepository<MedicalMeeting, Long> {

    List<MedicalMeeting> findByDoctorKeycloakIdOrderByCreatedAtDesc(String doctorKeycloakId);

    List<MedicalMeeting> findByPatientKeycloakIdOrderByCreatedAtDesc(String patientKeycloakId);

    // Cherche les réunions du patient OU du helper (aidant connecté)
    @Query("SELECT m FROM MedicalMeeting m WHERE m.patientKeycloakId = :id OR m.helperKeycloakId = :id ORDER BY m.createdAt DESC")
    List<MedicalMeeting> findByPatientOrHelperKeycloakId(@Param("id") String keycloakId);

    List<MedicalMeeting> findByDoctorKeycloakIdAndStatus(String doctorKeycloakId, MeetingStatus status);

    Optional<MedicalMeeting> findByRoomName(String roomName);
}
