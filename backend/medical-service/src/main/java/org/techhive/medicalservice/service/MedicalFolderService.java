package org.techhive.medicalservice.service;

import org.techhive.medicalservice.dto.CreateMedicalFolderRequest;
import org.techhive.medicalservice.dto.MedicalFolderResponse;
import org.techhive.medicalservice.dto.UpdateMedicalFolderRequest;

public interface MedicalFolderService {

	MedicalFolderResponse createMedicalFolder(CreateMedicalFolderRequest request);

	MedicalFolderResponse getMedicalFolderById(Long id);

	MedicalFolderResponse updateMedicalFolder(Long id, UpdateMedicalFolderRequest request);

	MedicalFolderResponse partialUpdateMedicalFolder(Long id, UpdateMedicalFolderRequest request);

	void deleteMedicalFolder(Long id);
}
