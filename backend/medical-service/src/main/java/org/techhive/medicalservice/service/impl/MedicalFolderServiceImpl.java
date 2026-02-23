package org.techhive.medicalservice.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.medicalservice.dto.CreateMedicalFolderRequest;
import org.techhive.medicalservice.dto.MedicalFolderResponse;
import org.techhive.medicalservice.dto.UpdateMedicalFolderRequest;
import org.techhive.medicalservice.entity.MedicalFolder;
import org.techhive.medicalservice.exception.ResourceNotFoundException;
import org.techhive.medicalservice.mapper.MedicalFolderMapper;
import org.techhive.medicalservice.repository.MedicalFolderRepository;
import org.techhive.medicalservice.service.MedicalFolderService;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class MedicalFolderServiceImpl implements MedicalFolderService {

	@Autowired
	private MedicalFolderRepository medicalFolderRepository;

	@Autowired
	private MedicalFolderMapper medicalFolderMapper;

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
		medicalFolderRepository.delete(folder);
		log.info("Medical folder deleted successfully with id: {}", id);
	}
}
