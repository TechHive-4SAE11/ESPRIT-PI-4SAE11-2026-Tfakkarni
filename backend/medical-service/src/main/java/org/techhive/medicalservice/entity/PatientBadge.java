package org.techhive.medicalservice.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Représente un badge / trophée décerné à un patient pour ses performances
 * aux jeux cognitifs. Stocké dans le medical-service (pas dans le game-service)
 * pour centraliser la vue clinique du patient.
 */
@Entity
@Table(name = "patient_badges", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "patient_id", "badge_code", "source_attempt_id" })
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientBadge implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Keycloak ID du patient */
    @Column(name = "patient_id", nullable = false, length = 255)
    private String patientId;

    /** Code machine du badge (ex: MEMORY_STAR, THREE_DAY_STREAK) */
    @Column(name = "badge_code", nullable = false, length = 50)
    private String badgeCode;

    /** Titre lisible du badge (ex: "Memory Star 🧠") */
    @Column(name = "badge_title", nullable = false, length = 120)
    private String badgeTitle;

    /** Description contextuelle (ex: "100% score on Image Game") */
    @Column(name = "description", length = 500)
    private String description;

    /** Date et heure d'attribution */
    @CreationTimestamp
    @Column(name = "awarded_at", nullable = false, updatable = false)
    private LocalDateTime awardedAt;

    /** Type de jeu source (MINI, CUSTOM, MOVIE, PERSONAL) */
    @Column(name = "source_game_type", length = 30)
    private String sourceGameType;

    /** ID de la tentative (attempt) qui a déclenché le badge */
    @Column(name = "source_attempt_id")
    private Long sourceAttemptId;
}
