package org.techhive.medicalservice.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.medicalservice.client.MlServiceClient;
import org.techhive.medicalservice.dto.AIReportResponse;
import org.techhive.medicalservice.dto.ClinicalAnalysisResult;
import org.techhive.medicalservice.dto.DiagnosticsResponse;
import org.techhive.medicalservice.dto.DossierForMlRequest;
import org.techhive.medicalservice.dto.MedicalHistoryResponse;
import org.techhive.medicalservice.entity.AIReport;
import org.techhive.medicalservice.entity.MedicalFolder;
import org.techhive.medicalservice.repository.AIReportRepository;
import org.techhive.medicalservice.repository.MedicalFolderRepository;
import org.techhive.medicalservice.service.AIReportService;
import org.techhive.medicalservice.service.DiagnosticsService;
import org.techhive.medicalservice.service.MedicalHistoryService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIReportServiceImpl implements AIReportService {

	private final AIReportRepository aiReportRepository;
	private final MedicalFolderRepository medicalFolderRepository;
	private final DiagnosticsService diagnosticsService;
	private final MedicalHistoryService medicalHistoryService;
	private final MlServiceClient mlServiceClient;
	private final ObjectMapper objectMapper;

	@Override
	@Transactional(readOnly = true)
	public List<AIReportResponse> getByFolderId(Long folderId) {
		return aiReportRepository.findByMedicalFolderIdOrderByGeneratedAtDesc(folderId)
			.stream()
			.map(this::toResponse)
			.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<AIReportResponse> getLatestByFolderId(Long folderId) {
		return aiReportRepository.findFirstByMedicalFolderIdOrderByGeneratedAtDesc(folderId)
			.map(this::toResponse);
	}

	@Override
	@Transactional
	public AIReportResponse generateReport(Long folderId) {
		MedicalFolder folder = medicalFolderRepository.findById(folderId)
			.orElseThrow(() -> new EntityNotFoundException("Medical folder not found: " + folderId));
		AIReport report = AIReport.builder()
			.medicalFolder(folder)
			.status(AIReport.Status.PENDING)
			.build();
		report = aiReportRepository.save(report);
		log.info("Created AI report PENDING for folder {} reportId={}", folderId, report.getId());
		runAnalysisAsync(report.getId());
		return toResponse(report);
	}

	@Async
	public void runAnalysisAsync(Long reportId) {
		try {
			runAnalysis(reportId);
		} catch (Exception e) {
			log.error("AI report generation failed for reportId={}", reportId, e);
			aiReportRepository.findById(reportId).ifPresent(report -> {
				report.setStatus(AIReport.Status.ERROR);
				report.setErrorMessage(e.getMessage() != null ? e.getMessage().substring(0, Math.min(1024, e.getMessage().length())) : "Unknown error");
				aiReportRepository.save(report);
			});
		}
	}

	@Transactional
	public void runAnalysis(Long reportId) {
		AIReport report = aiReportRepository.findById(reportId)
			.orElseThrow(() -> new EntityNotFoundException("AI report not found: " + reportId));
		MedicalFolder folder = report.getMedicalFolder();
		Long folderId = folder.getId();

		List<DiagnosticsResponse> diagnosticsList = diagnosticsService.getDiagnosticsByMedicalFolder(folderId);
		List<MedicalHistoryResponse> historyList = medicalHistoryService.getMedicalHistoryByMedicalFolder(folderId);

		DossierForMlRequest.DossierForMlRequestBuilder builder = DossierForMlRequest.builder()
			.folderId(folderId)
			.patientId(folder.getPatientId())
			.doctorId(folder.getDoctorId())
			.bloodType(folder.getBloodType())
			.height(folder.getHeight())
			.weight(folder.getWeight())
			.folderCreatedAt(folder.getCreatedAt())
			.diagnostics(diagnosticsList.stream()
				.map(d -> DossierForMlRequest.DiagnosticsSummary.builder()
					.id(d.getId())
					.diseaseName(d.getDiseaseName())
					.stage(d.getStage())
					.comorbidities(d.getComorbidities())
					.diagnosisDate(d.getDiagnosisDate())
					.build())
				.toList())
			.medicalHistory(historyList.stream()
				.map(h -> DossierForMlRequest.MedicalHistorySummary.builder()
					.id(h.getId())
					.allergies(h.getAllergies())
					.conditions(h.getConditions())
					.surgeries(h.getSurgeries())
					.symptoms(h.getSymptoms())
					.recommendedTreatment(h.getRecommendedTreatment())
					.familyHistory(h.getFamilyHistory())
					.createdAt(h.getCreatedAt())
					.build())
				.toList());

		DossierForMlRequest request = builder.build();
		ClinicalAnalysisResult result = mlServiceClient.analyzeDossier(request);
		if (result == null) {
			report.setStatus(AIReport.Status.ERROR);
			report.setErrorMessage("ML service returned empty response");
		} else {
			try {
				String reportJson = objectMapper.writeValueAsString(result);
				report.setReportJson(reportJson);
				report.setStatus(AIReport.Status.READY);
				report.setErrorMessage(null);
			} catch (JsonProcessingException e) {
				log.error("Failed to serialize AI result for report {}", reportId, e);
				report.setStatus(AIReport.Status.ERROR);
				report.setErrorMessage("Serialization error: " + (e.getMessage() != null ? e.getMessage().substring(0, Math.min(500, e.getMessage().length())) : "unknown"));
			}
		}
		aiReportRepository.save(report);
		log.info("AI report READY for folder {} reportId={}", folderId, reportId);
	}

	private AIReportResponse toResponse(AIReport report) {
		Object reportJsonObj = null;
		if (report.getReportJson() != null && !report.getReportJson().isBlank()) {
			try {
				reportJsonObj = objectMapper.readValue(report.getReportJson(), Object.class);
			} catch (JsonProcessingException e) {
				log.warn("Could not parse reportJson for report {}", report.getId(), e);
			}
		}
		return AIReportResponse.builder()
			.id(report.getId())
			.medicalFolderId(report.getMedicalFolder().getId())
			.generatedAt(report.getGeneratedAt())
			.reportJson(reportJsonObj)
			.status(report.getStatus().name())
			.errorMessage(report.getErrorMessage())
			.build();
	}
}
