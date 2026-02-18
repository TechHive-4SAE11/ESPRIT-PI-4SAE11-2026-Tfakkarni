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

	@Column(name = "allergies", columnDefinition = "TEXT")
	private String allergies;

	@Column(name = "conditions", columnDefinition = "TEXT")
	private String conditions;

	@Column(name = "surgeries", columnDefinition = "TEXT")
	private String surgeries;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private LocalDateTime updatedAt;
}
