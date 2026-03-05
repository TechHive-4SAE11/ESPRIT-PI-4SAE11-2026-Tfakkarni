package org.techhive.medicalservice.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.techhive.medicalservice.entity.DiagnosticAttachment;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosticAttachmentResponse {

	private Long id;

	private Long diagnosticId;

	private String fileName;

	private String originalFileName;

	private String contentType;

	private Long fileSize;

	private String description;

	private String fileType;

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime createdAt;

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime updatedAt;

	// Flag to indicate if file can be displayed inline (images)
	public boolean isImage() {
		return contentType != null && contentType.startsWith("image/");
	}

	// Flag to indicate if file is a PDF
	public boolean isPdf() {
		return contentType != null && contentType.equals("application/pdf");
	}

	// Get file size in human readable format
	public String getFormattedFileSize() {
		if (fileSize == null) return "Unknown";
		if (fileSize < 1024) return fileSize + " B";
		if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
		if (fileSize < 1024 * 1024 * 1024) return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
		return String.format("%.1f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
	}
}
