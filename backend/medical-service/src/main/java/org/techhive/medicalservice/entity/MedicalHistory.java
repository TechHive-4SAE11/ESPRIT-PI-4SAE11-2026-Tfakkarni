package org.techhive.medicalservice.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "medical_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalHistory implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull(message = "Medical folder cannot be null")
	@ManyToOne
	@JoinColumn(name = "medical_folder_id", nullable = false)
	@ToString.Exclude
	private MedicalFolder medicalFolder;

	@Size(max = 2000, message = "Allergies must not exceed 2000 characters")
	@Column(name = "allergies", columnDefinition = "TEXT")
	private String allergies;

	@Size(max = 2000, message = "Conditions must not exceed 2000 characters")
	@Column(name = "conditions", columnDefinition = "TEXT")
	private String conditions;

	@Size(max = 2000, message = "Surgeries must not exceed 2000 characters")
	@Column(name = "surgeries", columnDefinition = "TEXT")
	private String surgeries;

	@Size(max = 2000, message = "Symptoms must not exceed 2000 characters")
	@Column(name = "symptoms", columnDefinition = "TEXT")
	private String symptoms;

	@Size(max = 2000, message = "Recommended treatment must not exceed 2000 characters")
	@Column(name = "recommended_treatment", columnDefinition = "TEXT")
	private String recommendedTreatment;

	@Size(max = 2000, message = "Family history must not exceed 2000 characters")
	@Column(name = "family_history", columnDefinition = "TEXT")
	private String familyHistory;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private LocalDateTime updatedAt;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public MedicalFolder getMedicalFolder() { return medicalFolder; }
	public void setMedicalFolder(MedicalFolder medicalFolder) { this.medicalFolder = medicalFolder; }
	public String getAllergies() { return allergies; }
	public void setAllergies(String allergies) { this.allergies = allergies; }
	public String getConditions() { return conditions; }
	public void setConditions(String conditions) { this.conditions = conditions; }
	public String getSurgeries() { return surgeries; }
	public void setSurgeries(String surgeries) { this.surgeries = surgeries; }
	public String getSymptoms() { return symptoms; }
	public void setSymptoms(String symptoms) { this.symptoms = symptoms; }
	public String getRecommendedTreatment() { return recommendedTreatment; }
	public void setRecommendedTreatment(String recommendedTreatment) { this.recommendedTreatment = recommendedTreatment; }
	public String getFamilyHistory() { return familyHistory; }
	public void setFamilyHistory(String familyHistory) { this.familyHistory = familyHistory; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
