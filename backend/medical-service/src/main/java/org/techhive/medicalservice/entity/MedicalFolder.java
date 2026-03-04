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

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private LocalDateTime updatedAt;

	/** Inverse relationship with AIReport for cascade delete */
	@OneToMany(mappedBy = "medicalFolder", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	private List<AIReport> aiReports;
}
