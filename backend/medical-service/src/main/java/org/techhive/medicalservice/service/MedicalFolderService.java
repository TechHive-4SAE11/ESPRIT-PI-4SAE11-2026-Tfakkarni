package org.techhive.medicalservice.service;

import org.techhive.medicalservice.dto.CreateMedicalFolderRequest;
import org.techhive.medicalservice.dto.MedicalFolderResponse;

public interface MedicalFolderService {

	MedicalFolderResponse createMedicalFolder(CreateMedicalFolderRequest request);

	MedicalFolderResponse getMedicalFolderById(Long id);

	void deleteMedicalFolder(Long id);
}
