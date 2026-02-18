package org.techhive.gameservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Tracks per-data-point performance for spaced-repetition style game mixing.
 * Each record represents the latest result for a specific patient + data point.
 */
@Entity
@Table(name = "data_point_performance", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "patient_keycloak_id", "data_type", "data_point_id" })
})
public class DataPointPerformance {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "patient_keycloak_id", nullable = false)
  private String patientKeycloakId;

  @Enumerated(EnumType.STRING)
  @Column(name = "data_type", nullable = false)
  private DataPointType dataType;

  @Column(name = "data_point_id", nullable = false)
  private Long dataPointId;

  /**
   * How many times this data point was answered correctly (cumulative).
   */
  @Column(name = "correct_count", nullable = false)
  private int correctCount = 0;

  /**
   * How many times this data point was answered incorrectly (cumulative).
   */
  @Column(name = "incorrect_count", nullable = false)
  private int incorrectCount = 0;

  /**
   * The most recent result: true = correct, false = incorrect.
   */
  @Column(name = "last_correct", nullable = false)
  private boolean lastCorrect = false;

  @Column(name = "last_attempt_at", nullable = false)
  private LocalDateTime lastAttemptAt;

  @PrePersist
  protected void onCreate() {
    if (this.lastAttemptAt == null) {
      this.lastAttemptAt = LocalDateTime.now();
    }
  }

  public DataPointPerformance() {
  }

  public DataPointPerformance(String patientKeycloakId, DataPointType dataType, Long dataPointId,
      boolean correct) {
    this.patientKeycloakId = patientKeycloakId;
    this.dataType = dataType;
    this.dataPointId = dataPointId;
    this.lastCorrect = correct;
    this.correctCount = correct ? 1 : 0;
    this.incorrectCount = correct ? 0 : 1;
    this.lastAttemptAt = LocalDateTime.now();
  }

  public void recordResult(boolean correct) {
    this.lastCorrect = correct;
    if (correct) {
      this.correctCount++;
    } else {
      this.incorrectCount++;
    }
    this.lastAttemptAt = LocalDateTime.now();
  }

  // ── Getters & Setters ──

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getPatientKeycloakId() {
    return patientKeycloakId;
  }

  public void setPatientKeycloakId(String patientKeycloakId) {
    this.patientKeycloakId = patientKeycloakId;
  }

  public DataPointType getDataType() {
    return dataType;
  }

  public void setDataType(DataPointType dataType) {
    this.dataType = dataType;
  }

  public Long getDataPointId() {
    return dataPointId;
  }

  public void setDataPointId(Long dataPointId) {
    this.dataPointId = dataPointId;
  }

  public int getCorrectCount() {
    return correctCount;
  }

  public void setCorrectCount(int correctCount) {
    this.correctCount = correctCount;
  }

  public int getIncorrectCount() {
    return incorrectCount;
  }

  public void setIncorrectCount(int incorrectCount) {
    this.incorrectCount = incorrectCount;
  }

  public boolean isLastCorrect() {
    return lastCorrect;
  }

  public void setLastCorrect(boolean lastCorrect) {
    this.lastCorrect = lastCorrect;
  }

  public LocalDateTime getLastAttemptAt() {
    return lastAttemptAt;
  }

  public void setLastAttemptAt(LocalDateTime lastAttemptAt) {
    this.lastAttemptAt = lastAttemptAt;
  }
}
