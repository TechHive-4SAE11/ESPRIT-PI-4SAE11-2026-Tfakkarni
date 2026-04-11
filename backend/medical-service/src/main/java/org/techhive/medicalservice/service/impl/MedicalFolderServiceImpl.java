package org.techhive.medicalservice.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.medicalservice.dto.CreateMedicalFolderRequest;
import org.techhive.medicalservice.dto.MedicalFolderResponse;
import org.techhive.medicalservice.dto.MedicalFolderStatsResponse;
import org.techhive.medicalservice.dto.UpdateMedicalFolderRequest;
import org.techhive.medicalservice.entity.Diagnostics;
import org.techhive.medicalservice.entity.MedicalFolder;
import org.techhive.medicalservice.entity.MedicalHistory;
import org.techhive.medicalservice.exception.ResourceNotFoundException;
import org.techhive.medicalservice.mapper.MedicalFolderMapper;
import org.techhive.medicalservice.repository.AIReportRepository;
import org.techhive.medicalservice.repository.DiagnosticsRepository;
import org.techhive.medicalservice.repository.MedicalFolderRepository;
import org.techhive.medicalservice.repository.MedicalHistoryRepository;
import org.techhive.medicalservice.service.MedicalFolderService;

import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class MedicalFolderServiceImpl implements MedicalFolderService {

	@Autowired
	private MedicalFolderRepository medicalFolderRepository;

	@Autowired
	private DiagnosticsRepository diagnosticsRepository;

	@Autowired
	private MedicalHistoryRepository medicalHistoryRepository;

	@Autowired
	private AIReportRepository aiReportRepository;

	@Autowired
	private MedicalFolderMapper medicalFolderMapper;

	@Override
	public Page<MedicalFolderResponse> getMedicalFolders(Pageable pageable, String search) {
		log.debug("Fetching medical folders page: {} search: {}", pageable, search);
		if (search != null && !search.isBlank()) {
			return medicalFolderRepository.findByPatientIdContainingIgnoreCase(search.trim(), pageable)
					.map(medicalFolderMapper::toResponse);
		}
		return medicalFolderRepository.findAll(pageable)
				.map(medicalFolderMapper::toResponse);
	}

	@Override
	public MedicalFolderStatsResponse getMedicalFolderStats() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
		LocalDateTime weekAgo = now.minusDays(7);
		return MedicalFolderStatsResponse.builder()
				.total(medicalFolderRepository.count())
				.thisMonth(medicalFolderRepository.countByCreatedAtAfter(startOfMonth))
				.thisWeek(medicalFolderRepository.countByUpdatedAtAfter(weekAgo))
				.patientCount(medicalFolderRepository.countDistinctPatientIds())
				.build();
	}

	@Override
	public List<MedicalFolderResponse> getAllMedicalFolders() {
		log.debug("Fetching all medical folders");
		return medicalFolderRepository.findAll().stream()
				.map(medicalFolderMapper::toResponse)
				.toList();
	}

	@Override
	public List<MedicalFolderResponse> getMedicalFoldersByDoctorId(String doctorId) {
		log.debug("Fetching medical folders for doctor: {}", doctorId);
		return medicalFolderRepository.findByDoctorId(doctorId).stream()
				.map(medicalFolderMapper::toResponse)
				.toList();
	}

	@Override
	public List<MedicalFolderResponse> getMedicalFoldersByPatientId(String patientId) {
		log.debug("Fetching medical folders for patient: {}", patientId);
		return medicalFolderRepository.findByPatientId(patientId).stream()
				.map(medicalFolderMapper::toResponse)
				.toList();
	}

	@Override
	public List<MedicalFolderResponse> getMedicalFoldersByPatientIdAndDoctorId(String patientId, String doctorId) {
		log.debug("Fetching medical folders for patient: {} and doctor: {}", patientId, doctorId);
		return medicalFolderRepository.findByPatientIdAndDoctorId(patientId, doctorId).stream()
				.map(medicalFolderMapper::toResponse)
				.toList();
	}

	@Override
	public MedicalFolderResponse createMedicalFolder(CreateMedicalFolderRequest request) {
		log.debug("Creating new medical folder for patient: {} and doctor: {}", request.getPatientId(),
				request.getDoctorId());
		MedicalFolder folder = medicalFolderMapper.toEntity(request);
		MedicalFolder savedFolder = medicalFolderRepository.save(folder);
		log.info("Medical folder created successfully with id: {}", savedFolder.getId());
		return medicalFolderMapper.toResponse(savedFolder);
	}

	@Override
	public MedicalFolderResponse getMedicalFolderById(Long id) {
		log.debug("Fetching medical folder with id: {}", id);
		MedicalFolder folder = medicalFolderRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Medical folder not found with id: " + id));
		return medicalFolderMapper.toResponse(folder);
	}

	@Override
	public MedicalFolderResponse updateMedicalFolder(Long id, UpdateMedicalFolderRequest request) {
		log.debug("Updating medical folder with id: {}", id);
		MedicalFolder folder = medicalFolderRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Medical folder not found with id: " + id));
		MedicalFolder updatedFolder = medicalFolderMapper.toEntity(request, folder);
		MedicalFolder savedFolder = medicalFolderRepository.save(updatedFolder);
		log.info("Medical folder updated successfully with id: {}", id);
		return medicalFolderMapper.toResponse(savedFolder);
	}

	@Override
	public MedicalFolderResponse partialUpdateMedicalFolder(Long id, UpdateMedicalFolderRequest request) {
		log.debug("Partially updating medical folder with id: {}", id);
		return updateMedicalFolder(id, request);
	}

	@Override
	public void deleteMedicalFolder(Long id) {
		log.debug("Deleting medical folder with id: {}", id);
		MedicalFolder folder = medicalFolderRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Medical folder not found with id: " + id));
		
		// Delete dependent records first to avoid FK constraint violation
		// 1. Delete AI Reports
		List<org.techhive.medicalservice.entity.AIReport> aiReports = aiReportRepository.findByMedicalFolderIdOrderByGeneratedAtDesc(id);
		if (!aiReports.isEmpty()) {
			aiReportRepository.deleteAll(aiReports);
			log.debug("Deleted {} AI reports for folder id: {}", aiReports.size(), id);
		}
		
		// 2. Delete Diagnostics
		List<Diagnostics> diagnostics = diagnosticsRepository.findByMedicalFolderId(id);
		if (!diagnostics.isEmpty()) {
			diagnosticsRepository.deleteAll(diagnostics);
			log.debug("Deleted {} diagnostics for folder id: {}", diagnostics.size(), id);
		}
		
		// 3. Delete Medical History
		List<MedicalHistory> histories = medicalHistoryRepository.findByMedicalFolderId(id);
		if (!histories.isEmpty()) {
			medicalHistoryRepository.deleteAll(histories);
			log.debug("Deleted {} medical history entries for folder id: {}", histories.size(), id);
		}
		
		// 4. Finally delete the medical folder itself
		medicalFolderRepository.delete(folder);
		log.info("Medical folder deleted successfully with id: {}", id);
	}
}
