package org.techhive.medicalservice.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.medicalservice.dto.CreateMedicalHistoryRequest;
import org.techhive.medicalservice.dto.MedicalHistoryResponse;
import org.techhive.medicalservice.dto.UpdateMedicalHistoryRequest;
import org.techhive.medicalservice.entity.MedicalFolder;
import org.techhive.medicalservice.entity.MedicalHistory;
import org.techhive.medicalservice.mapper.MedicalHistoryMapper;
import org.techhive.medicalservice.repository.MedicalFolderRepository;
import org.techhive.medicalservice.repository.MedicalHistoryRepository;
import org.techhive.medicalservice.service.MedicalHistoryService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MedicalHistoryServiceImpl implements MedicalHistoryService {

	private final MedicalHistoryRepository medicalHistoryRepository;
	private final MedicalFolderRepository medicalFolderRepository;

	@Override
	public MedicalHistoryResponse createMedicalHistory(CreateMedicalHistoryRequest request) {
		log.debug("Creating medical history for medical folder: {}", request.getMedicalFolderId());
		MedicalFolder medicalFolder = medicalFolderRepository.findById(request.getMedicalFolderId())
			.orElseThrow(() -> new EntityNotFoundException(
				"Medical folder not found with id: " + request.getMedicalFolderId()));
		MedicalHistory medicalHistory = MedicalHistoryMapper.toEntity(request, medicalFolder);
		MedicalHistory savedHistory = medicalHistoryRepository.save(medicalHistory);
		log.info("Medical history created successfully with id: {}", savedHistory.getId());
		return MedicalHistoryMapper.toResponse(savedHistory);
	}

	@Override
	@Transactional(readOnly = true)
	public MedicalHistoryResponse getMedicalHistoryById(Long id) {
		log.debug("Getting medical history with id: {}", id);
		MedicalHistory medicalHistory = medicalHistoryRepository.findById(id)
			.orElseThrow(() -> new EntityNotFoundException("Medical history not found with id: " + id));
		return MedicalHistoryMapper.toResponse(medicalHistory);
	}

	@Override
	@Transactional(readOnly = true)
	public List<MedicalHistoryResponse> getMedicalHistoryByMedicalFolder(Long medicalFolderId) {
		log.debug("Getting medical history for medical folder: {}", medicalFolderId);
		medicalFolderRepository.findById(medicalFolderId)
			.orElseThrow(() -> new EntityNotFoundException("Medical folder not found with id: " + medicalFolderId));
		List<MedicalHistory> historyList = medicalHistoryRepository.findByMedicalFolderId(medicalFolderId);
		return historyList.stream().map(MedicalHistoryMapper::toResponse).toList();
	}

	@Override
	public MedicalHistoryResponse updateMedicalHistory(Long id, UpdateMedicalHistoryRequest request) {
		log.debug("Updating medical history with id: {}", id);
		MedicalHistory medicalHistory = medicalHistoryRepository.findById(id)
			.orElseThrow(() -> new EntityNotFoundException("Medical history not found with id: " + id));
		MedicalHistory updatedHistory = MedicalHistoryMapper.toEntity(request, medicalHistory);
		MedicalHistory savedHistory = medicalHistoryRepository.save(updatedHistory);
		log.info("Medical history updated successfully with id: {}", id);
		return MedicalHistoryMapper.toResponse(savedHistory);
	}

	@Override
	public MedicalHistoryResponse partialUpdateMedicalHistory(Long id, UpdateMedicalHistoryRequest request) {
		log.debug("Partially updating medical history with id: {}", id);
		return updateMedicalHistory(id, request);
	}

	@Override
	public void deleteMedicalHistory(Long id) {
		log.debug("Deleting medical history with id: {}", id);
		if (!medicalHistoryRepository.existsById(id)) {
			throw new EntityNotFoundException("Medical history not found with id: " + id);
		}
		medicalHistoryRepository.deleteById(id);
		log.info("Medical history deleted successfully with id: {}", id);
	}
}
