package org.techhive.medicalservice.service;

import java.util.List;

import org.techhive.medicalservice.dto.CreateMedicalHistoryRequest;
import org.techhive.medicalservice.dto.MedicalHistoryResponse;
import org.techhive.medicalservice.dto.UpdateMedicalHistoryRequest;

public interface MedicalHistoryService {
	MedicalHistoryResponse createMedicalHistory(CreateMedicalHistoryRequest request);

	MedicalHistoryResponse getMedicalHistoryById(Long id);

	List<MedicalHistoryResponse> getMedicalHistoryByMedicalFolder(Long medicalFolderId);

	MedicalHistoryResponse updateMedicalHistory(Long id, UpdateMedicalHistoryRequest request);

	MedicalHistoryResponse partialUpdateMedicalHistory(Long id, UpdateMedicalHistoryRequest request);

	void deleteMedicalHistory(Long id);
}
