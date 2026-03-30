package org.techhive.trackingservice.repository;

import org.techhive.trackingservice.entity.MedicalMeeting;
import org.techhive.trackingservice.entity.MeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalMeetingRepository extends JpaRepository<MedicalMeeting, Long> {

    List<MedicalMeeting> findByDoctorKeycloakIdOrderByCreatedAtDesc(String doctorKeycloakId);

    List<MedicalMeeting> findByPatientKeycloakIdOrderByCreatedAtDesc(String patientKeycloakId);

    List<MedicalMeeting> findByDoctorKeycloakIdAndStatus(String doctorKeycloakId, MeetingStatus status);

    Optional<MedicalMeeting> findByRoomName(String roomName);
}
