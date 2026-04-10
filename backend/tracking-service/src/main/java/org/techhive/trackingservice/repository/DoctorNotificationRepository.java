package org.techhive.trackingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.techhive.trackingservice.entity.DoctorNotification;

import java.util.List;

public interface DoctorNotificationRepository extends JpaRepository<DoctorNotification, Long> {

    // Query exacte par doctorKeycloakId
    List<DoctorNotification> findByDoctorKeycloakIdOrderByCreatedAtDesc(String doctorKeycloakId);

    long countByDoctorKeycloakIdAndReadFalse(String doctorKeycloakId);

    // Query par liste d'IDs (utile quand l'ID Keycloak a changé)
    @Query("SELECT n FROM DoctorNotification n WHERE n.doctorKeycloakId IN :ids ORDER BY n.createdAt DESC")
    List<DoctorNotification> findByDoctorKeycloakIdInOrderByCreatedAtDesc(@Param("ids") List<String> ids);

    // Toutes les notifications (pour debug et fallback)
    List<DoctorNotification> findAllByOrderByCreatedAtDesc();

    // Tous les doctorKeycloakId distincts (pour debug)
    @Query("SELECT DISTINCT n.doctorKeycloakId FROM DoctorNotification n")
    List<String> findDistinctDoctorKeycloakIds();
}
