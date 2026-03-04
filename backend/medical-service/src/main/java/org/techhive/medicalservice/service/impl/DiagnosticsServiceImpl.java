package org.techhive.medicalservice.service.impl;

import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.medicalservice.dto.CreateDiagnosticsRequest;
import org.techhive.medicalservice.dto.DiagnosticsResponse;
import org.techhive.medicalservice.dto.UpdateDiagnosticsRequest;
import org.techhive.medicalservice.entity.Diagnostics;
import org.techhive.medicalservice.entity.MedicalFolder;
import org.techhive.medicalservice.mapper.DiagnosticsMapper;
import org.techhive.medicalservice.repository.DiagnosticsRepository;
import org.techhive.medicalservice.repository.MedicalFolderRepository;
import org.techhive.medicalservice.service.DiagnosticsService;
import org.techhive.medicalservice.service.AIReportService;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class DiagnosticsServiceImpl implements DiagnosticsService {

	private final DiagnosticsRepository diagnosticsRepository;
	private final MedicalFolderRepository medicalFolderRepository;
	private final AIReportService aiReportService;

	public DiagnosticsServiceImpl(
			DiagnosticsRepository diagnosticsRepository,
			MedicalFolderRepository medicalFolderRepository,
			@Lazy AIReportService aiReportService) {
		this.diagnosticsRepository = diagnosticsRepository;
		this.medicalFolderRepository = medicalFolderRepository;
		this.aiReportService = aiReportService;
	}

	@Override
	public DiagnosticsResponse createDiagnostics(CreateDiagnosticsRequest request) {
		log.debug("Creating diagnostics for medical folder: {}", request.getMedicalFolderId());
		MedicalFolder medicalFolder = medicalFolderRepository.findById(request.getMedicalFolderId())
			.orElseThrow(() -> new EntityNotFoundException(
				"Medical folder not found with id: " + request.getMedicalFolderId()));
		Diagnostics diagnostics = DiagnosticsMapper.toEntity(request, medicalFolder);
		Diagnostics savedDiagnostics = diagnosticsRepository.save(diagnostics);
		log.info("Diagnostics created successfully with id: {}", savedDiagnostics.getId());
		aiReportService.generateReport(request.getMedicalFolderId());
		return DiagnosticsMapper.toResponse(savedDiagnostics);
	}

	@Override
	@Transactional(readOnly = true)
	public DiagnosticsResponse getDiagnosticsById(Long id) {
		log.debug("Getting diagnostics with id: {}", id);
		Diagnostics diagnostics = diagnosticsRepository.findById(id)
			.orElseThrow(() -> new EntityNotFoundException("Diagnostics not found with id: " + id));
		return DiagnosticsMapper.toResponse(diagnostics);
	}

	@Override
	@Transactional(readOnly = true)
	public List<DiagnosticsResponse> getDiagnosticsByMedicalFolder(Long medicalFolderId) {
		log.debug("Getting diagnostics for medical folder: {}", medicalFolderId);
		medicalFolderRepository.findById(medicalFolderId)
			.orElseThrow(() -> new EntityNotFoundException("Medical folder not found with id: " + medicalFolderId));
		List<Diagnostics> diagnosticsList = diagnosticsRepository.findByMedicalFolderId(medicalFolderId);
		return diagnosticsList.stream().map(DiagnosticsMapper::toResponse).toList();
	}

	@Override
	public DiagnosticsResponse updateDiagnostics(Long id, UpdateDiagnosticsRequest request) {
		log.debug("Updating diagnostics with id: {}", id);
		Diagnostics diagnostics = diagnosticsRepository.findById(id)
			.orElseThrow(() -> new EntityNotFoundException("Diagnostics not found with id: " + id));
		Diagnostics updatedDiagnostics = DiagnosticsMapper.toEntity(request, diagnostics);
		Diagnostics savedDiagnostics = diagnosticsRepository.save(updatedDiagnostics);
		log.info("Diagnostics updated successfully with id: {}", id);
		aiReportService.generateReport(diagnostics.getMedicalFolder().getId());
		return DiagnosticsMapper.toResponse(savedDiagnostics);
	}

	@Override
	public DiagnosticsResponse partialUpdateDiagnostics(Long id, UpdateDiagnosticsRequest request) {
		log.debug("Partially updating diagnostics with id: {}", id);
		return updateDiagnostics(id, request);
	}

	@Override
	public void deleteDiagnostics(Long id) {
		log.debug("Deleting diagnostics with id: {}", id);
		if (!diagnosticsRepository.existsById(id)) {
			throw new EntityNotFoundException("Diagnostics not found with id: " + id);
		}
		diagnosticsRepository.deleteById(id);
		log.info("Diagnostics deleted successfully with id: {}", id);
	}
}
