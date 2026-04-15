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
				.bloodType(request.getBloodType())
				.height(request.getHeight())
				.weight(request.getWeight())
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
		if (request.getBloodType() != null) {
			existing.setBloodType(request.getBloodType());
		}
		if (request.getHeight() != null) {
			existing.setHeight(request.getHeight());
		}
		if (request.getWeight() != null) {
			existing.setWeight(request.getWeight());
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
				.bloodType(folder.getBloodType())
				.height(folder.getHeight())
				.weight(folder.getWeight())
				.createdAt(folder.getCreatedAt())
				.updatedAt(folder.getUpdatedAt())
				.consecutiveNoShows(folder.getConsecutiveNoShows())
				.totalNoShows(folder.getTotalNoShows())
				.bookingRestricted(folder.isBookingRestricted())
				.restrictionReason(folder.getRestrictionReason())
				.manualReviewRequired(folder.isManualReviewRequired())
				.attendanceRiskLevel(folder.getAttendanceRiskLevel())
				.attendanceRestrictionOverridden(folder.isAttendanceRestrictionOverridden())
				.build();
	}
}
