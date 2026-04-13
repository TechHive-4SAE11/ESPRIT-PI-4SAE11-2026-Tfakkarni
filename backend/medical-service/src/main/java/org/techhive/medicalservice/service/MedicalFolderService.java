package org.techhive.medicalservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.techhive.medicalservice.dto.CreateMedicalFolderRequest;
import org.techhive.medicalservice.dto.MedicalFolderResponse;
import org.techhive.medicalservice.dto.MedicalFolderStatsResponse;
import org.techhive.medicalservice.dto.UpdateMedicalFolderRequest;

import java.util.List;

public interface MedicalFolderService {

	Page<MedicalFolderResponse> getMedicalFolders(Pageable pageable, String search);

	MedicalFolderStatsResponse getMedicalFolderStats();

	List<MedicalFolderResponse> getAllMedicalFolders();

	List<MedicalFolderResponse> getMedicalFoldersByDoctorId(String doctorId);

	List<MedicalFolderResponse> getMedicalFoldersByPatientId(String patientId);

	MedicalFolderResponse createMedicalFolder(CreateMedicalFolderRequest request);

	MedicalFolderResponse getMedicalFolderById(Long id);

	MedicalFolderResponse updateMedicalFolder(Long id, UpdateMedicalFolderRequest request);

	MedicalFolderResponse partialUpdateMedicalFolder(Long id, UpdateMedicalFolderRequest request);

	void deleteMedicalFolder(Long id);

	/**
	 * Clears temporary booking restriction after manual review (attendance monitoring).
	 */
	MedicalFolderResponse clearBookingRestrictionAfterReview(Long medicalFolderId);

	/**
	 * Manually restricts a patient from booking appointments (by an admin/doctor).
	 */
	MedicalFolderResponse manualRestrictPatientBooking(Long medicalFolderId, String reason);
}
