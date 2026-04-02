package org.techhive.medicalservice.service;

import java.util.List;

import org.techhive.medicalservice.dto.CreateDiagnosticsRequest;
import org.techhive.medicalservice.dto.DiagnosticsResponse;
import org.techhive.medicalservice.dto.UpdateDiagnosticsRequest;

public interface DiagnosticsService {
	DiagnosticsResponse createDiagnostics(CreateDiagnosticsRequest request);

	DiagnosticsResponse getDiagnosticsById(Long id);

	List<DiagnosticsResponse> getDiagnosticsByMedicalFolder(Long medicalFolderId);

	DiagnosticsResponse updateDiagnostics(Long id, UpdateDiagnosticsRequest request);

	DiagnosticsResponse partialUpdateDiagnostics(Long id, UpdateDiagnosticsRequest request);

	void deleteDiagnostics(Long id);
}
