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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "diagnostics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Diagnostics implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull(message = "Medical folder cannot be null")
	@ManyToOne
	@JoinColumn(name = "medical_folder_id", nullable = false)
	@ToString.Exclude
	private MedicalFolder medicalFolder;

	@NotNull(message = "Disease name cannot be null")
	@NotBlank(message = "Disease name cannot be blank")
	@Size(min = 2, max = 255, message = "Disease name must be between 2 and 255 characters")
	@Column(name = "disease_name", nullable = false)
	private String diseaseName;

	@Size(max = 100, message = "Stage must not exceed 100 characters")
	@Column(name = "stage")
	private String stage;

	@Size(max = 1000, message = "Comorbidities must not exceed 1000 characters")
	@Column(name = "comorbidities", columnDefinition = "TEXT")
	private String comorbidities;

	@NotNull(message = "Diagnosis date cannot be null")
	@Column(name = "diagnosis_date", nullable = false)
	private LocalDateTime diagnosisDate;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private LocalDateTime updatedAt;

	/** Inverse relationship with DiagnosticAttachment for cascade delete */
	@OneToMany(mappedBy = "diagnostic", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	private List<DiagnosticAttachment> attachments;
}
