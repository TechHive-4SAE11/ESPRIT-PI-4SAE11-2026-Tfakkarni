package org.techhive.medicalservice.mapper;

import org.springframework.stereotype.Component;
import org.techhive.medicalservice.dto.CreateMedicalFolderRequest;
import org.techhive.medicalservice.dto.MedicalFolderResponse;
import org.techhive.medicalservice.dto.UpdateMedicalFolderRequest;
import org.techhive.medicalservice.entity.MedicalFolder;

@Component
public class MedicalFolderMapper {

	public MedicalFolder toEntity(CreateMedicalFolderRequest request) {
		if (request == null) {
			return null;
		}

		return MedicalFolder.builder()
				.patientId(request.getPatientId())
				.doctorId(request.getDoctorId())
				.build();
	}

	public MedicalFolder toEntity(UpdateMedicalFolderRequest request, MedicalFolder existing) {
		if (request == null || existing == null) {
			return existing;
		}

		if (request.getPatientId() != null) {
			existing.setPatientId(request.getPatientId());
		}
		if (request.getDoctorId() != null) {
			existing.setDoctorId(request.getDoctorId());
		}
		return existing;
	}

	public MedicalFolderResponse toResponse(MedicalFolder folder) {
		if (folder == null) {
			return null;
		}

		return MedicalFolderResponse.builder()
				.id(folder.getId())
				.patientId(folder.getPatientId())
				.doctorId(folder.getDoctorId())
				.createdAt(folder.getCreatedAt())
				.updatedAt(folder.getUpdatedAt())
				.build();
	}
}
