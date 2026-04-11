package org.techhive.medicalservice.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "medical_folders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalFolder implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull(message = "Patient ID cannot be null")
	@NotBlank(message = "Patient ID cannot be blank")
	@Size(min = 1, max = 255, message = "Patient ID must be between 1 and 255 characters")
	@Column(name = "id_patient", nullable = false)
	private String patientId;

	@NotNull(message = "Doctor ID cannot be null")
	@NotBlank(message = "Doctor ID cannot be blank")
	@Size(min = 1, max = 255, message = "Doctor ID must be between 1 and 255 characters")
	@Column(name = "id_doctor", nullable = false)
	private String doctorId;

	@Size(max = 10, message = "Blood type must not exceed 10 characters")
	@Column(name = "blood_type", length = 10)
	private String bloodType;

	@Column(name = "height")
	private Double height;

	@Column(name = "weight")
	private Double weight;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private LocalDateTime updatedAt;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getPatientId() { return patientId; }
	public void setPatientId(String patientId) { this.patientId = patientId; }
	public String getDoctorId() { return doctorId; }
	public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
	public String getBloodType() { return bloodType; }
	public void setBloodType(String bloodType) { this.bloodType = bloodType; }
	public Double getHeight() { return height; }
	public void setHeight(Double height) { this.height = height; }
	public Double getWeight() { return weight; }
	public void setWeight(Double weight) { this.weight = weight; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
	public List<AIReport> getAiReports() { return aiReports; }
	public void setAiReports(List<AIReport> aiReports) { this.aiReports = aiReports; }
	public List<Diagnostics> getDiagnostics() { return diagnostics; }
	public void setDiagnostics(List<Diagnostics> diagnostics) { this.diagnostics = diagnostics; }
	public List<MedicalHistory> getMedicalHistories() { return medicalHistories; }
	public void setMedicalHistories(List<MedicalHistory> medicalHistories) { this.medicalHistories = medicalHistories; }

	/** Inverse relationship with AIReport for cascade delete */
	@OneToMany(mappedBy = "medicalFolder", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	private List<AIReport> aiReports;

	/** Inverse relationship with Diagnostics for cascade delete */
	@OneToMany(mappedBy = "medicalFolder", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	private List<Diagnostics> diagnostics;

	/** Inverse relationship with MedicalHistory for cascade delete */
	@OneToMany(mappedBy = "medicalFolder", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	private List<MedicalHistory> medicalHistories;
}
