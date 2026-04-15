package org.techhive.medicalservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.medicalservice.entity.PatientBadge;

import java.util.List;

public interface PatientBadgeRepository extends JpaRepository<PatientBadge, Long> {

    /** Tous les badges d'un patient, du plus récent au plus ancien */
    List<PatientBadge> findByPatientIdOrderByAwardedAtDesc(String patientId);

    /** Vérifie si un badge identique existe déjà (évite les doublons pour la même tentative) */
    boolean existsByPatientIdAndBadgeCodeAndSourceAttemptId(String patientId, String badgeCode, Long sourceAttemptId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM patient_badges WHERE id NOT IN (SELECT MIN(id) FROM patient_badges GROUP BY patient_id, badge_code)", nativeQuery = true)
    void cleanupDuplicates();

    /** Vérifie si un badge (sans tentative spécifique) existe déjà (pour les streak-badges) */
    boolean existsByPatientIdAndBadgeCode(String patientId, String badgeCode);

    /** Nombre total de badges d'un patient */
    long countByPatientId(String patientId);
}
