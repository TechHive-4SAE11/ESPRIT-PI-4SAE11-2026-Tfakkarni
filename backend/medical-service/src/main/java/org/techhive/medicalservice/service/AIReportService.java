package org.techhive.medicalservice.service;

import java.util.List;
import java.util.Optional;

import org.techhive.medicalservice.dto.AIReportResponse;

public interface AIReportService {

	List<AIReportResponse> getByFolderId(Long folderId);

	Optional<AIReportResponse> getLatestByFolderId(Long folderId);

	/**
	 * Trigger async generation. Creates a PENDING report and runs ML analysis in background.
	 * @return the created report (status PENDING)
	 */
	AIReportResponse generateReport(Long folderId);
}
