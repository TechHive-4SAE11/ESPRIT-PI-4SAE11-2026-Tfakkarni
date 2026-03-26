package org.techhive.medicalservice.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "ai_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIReport implements Serializable {

	private static final long serialVersionUID = 1L;

	public enum Status {
		PENDING,
		READY,
		ERROR
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull
	@ManyToOne
	@JoinColumn(name = "medical_folder_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	@ToString.Exclude
	private MedicalFolder medicalFolder;

	@CreationTimestamp
	@Column(name = "generated_at", nullable = false, updatable = false)
	private LocalDateTime generatedAt;

	/** JSON payload: differentials, anomalies, riskLevel, advice, contradictions */
	@Column(name = "report_json", columnDefinition = "TEXT")
	private String reportJson;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Status status;

	@Column(name = "error_message", length = 1024)
	private String errorMessage;
}
