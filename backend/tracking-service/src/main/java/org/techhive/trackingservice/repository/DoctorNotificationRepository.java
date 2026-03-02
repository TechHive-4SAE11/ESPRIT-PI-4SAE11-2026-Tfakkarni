package org.techhive.trackingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.techhive.trackingservice.entity.DoctorNotification;

import java.util.List;

public interface DoctorNotificationRepository extends JpaRepository<DoctorNotification, Long> {
    List<DoctorNotification> findByDoctorKeycloakIdOrderByCreatedAtDesc(String doctorKeycloakId);
    long countByDoctorKeycloakIdAndReadFalse(String doctorKeycloakId);
}
