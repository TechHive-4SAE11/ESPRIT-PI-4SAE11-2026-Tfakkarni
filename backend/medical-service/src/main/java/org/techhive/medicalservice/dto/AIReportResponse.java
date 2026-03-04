package org.techhive.medicalservice.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIReportResponse {

	private Long id;
	private Long medicalFolderId;

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime generatedAt;

	/** Parsed report content (differentials, anomalies, riskLevel, advice, contradictions) */
	private Object reportJson;

	private String status; // PENDING, READY, ERROR
	private String errorMessage;
}
