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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "diagnostic_attachments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosticAttachment implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull(message = "Diagnostic cannot be null")
	@ManyToOne
	@JoinColumn(name = "diagnostic_id", nullable = false)
	@ToString.Exclude
	private Diagnostics diagnostic;

	@NotNull(message = "File name cannot be null")
	@NotBlank(message = "File name cannot be blank")
	@Size(max = 255, message = "File name must not exceed 255 characters")
	@Column(name = "file_name", nullable = false)
	private String fileName;

	@NotNull(message = "Original file name cannot be null")
	@NotBlank(message = "Original file name cannot be blank")
	@Size(max = 255, message = "Original file name must not exceed 255 characters")
	@Column(name = "original_file_name", nullable = false)
	private String originalFileName;

	@NotNull(message = "Content type cannot be null")
	@Size(max = 100, message = "Content type must not exceed 100 characters")
	@Column(name = "content_type", nullable = false)
	private String contentType;

	@NotNull(message = "File size cannot be null")
	@Column(name = "file_size", nullable = false)
	private Long fileSize;

	@Lob
	@Column(name = "file_data", columnDefinition = "BYTEA")
	private byte[] fileData;

	@Size(max = 500, message = "Description must not exceed 500 characters")
	@Column(name = "description")
	private String description;

	@Size(max = 50, message = "File type must not exceed 50 characters")
	@Column(name = "file_type")
	private String fileType; // XRAY, MRI, CT_SCAN, ULTRASOUND, PHOTO, DOCUMENT, OTHER

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private LocalDateTime updatedAt;
}
