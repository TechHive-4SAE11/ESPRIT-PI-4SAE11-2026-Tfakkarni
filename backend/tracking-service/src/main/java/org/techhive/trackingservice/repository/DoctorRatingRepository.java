package org.techhive.trackingservice.repository;

import org.techhive.trackingservice.entity.DoctorRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DoctorRatingRepository extends JpaRepository<DoctorRating, Long> {

    List<DoctorRating> findByDoctorKeycloakIdOrderByCreatedAtDesc(String doctorKeycloakId);

    boolean existsByMeetingIdAndPatientKeycloakId(Long meetingId, String patientKeycloakId);

    Optional<DoctorRating> findByMeetingIdAndPatientKeycloakId(Long meetingId, String patientKeycloakId);

    @Query("SELECT r.doctorKeycloakId, r.doctorName, AVG(r.rating), COUNT(r) " +
           "FROM DoctorRating r GROUP BY r.doctorKeycloakId, r.doctorName " +
           "ORDER BY AVG(r.rating) DESC")
    List<Object[]> findDoctorAverageRatings();

    @Query("SELECT AVG(r.rating) FROM DoctorRating r WHERE r.doctorKeycloakId = :id")
    Double findAverageRatingByDoctor(@Param("id") String doctorKeycloakId);
}
