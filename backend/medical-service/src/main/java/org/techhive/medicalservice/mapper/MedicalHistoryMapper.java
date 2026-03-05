package org.techhive.medicalservice.mapper;

import org.techhive.medicalservice.dto.CreateMedicalHistoryRequest;
import org.techhive.medicalservice.dto.MedicalHistoryResponse;
import org.techhive.medicalservice.dto.UpdateMedicalHistoryRequest;
import org.techhive.medicalservice.entity.MedicalFolder;
import org.techhive.medicalservice.entity.MedicalHistory;

public class MedicalHistoryMapper {
	public static MedicalHistory toEntity(CreateMedicalHistoryRequest request, MedicalFolder medicalFolder) {
		return MedicalHistory.builder()
			.medicalFolder(medicalFolder)
			.allergies(request.getAllergies())
			.conditions(request.getConditions())
			.surgeries(request.getSurgeries())
			.symptoms(request.getSymptoms())
			.recommendedTreatment(request.getRecommendedTreatment())
			.familyHistory(request.getFamilyHistory())
			.build();
	}

	public static MedicalHistory toEntity(UpdateMedicalHistoryRequest request, MedicalHistory existing) {
		if (request.getAllergies() != null) {
			existing.setAllergies(request.getAllergies());
		}
		if (request.getConditions() != null) {
			existing.setConditions(request.getConditions());
		}
		if (request.getSurgeries() != null) {
			existing.setSurgeries(request.getSurgeries());
		}
		if (request.getSymptoms() != null) {
			existing.setSymptoms(request.getSymptoms());
		}
		if (request.getRecommendedTreatment() != null) {
			existing.setRecommendedTreatment(request.getRecommendedTreatment());
		}
		if (request.getFamilyHistory() != null) {
			existing.setFamilyHistory(request.getFamilyHistory());
		}
		return existing;
	}

	public static MedicalHistoryResponse toResponse(MedicalHistory entity) {
		return MedicalHistoryResponse.builder()
			.id(entity.getId())
			.medicalFolderId(entity.getMedicalFolder().getId())
			.allergies(entity.getAllergies())
			.conditions(entity.getConditions())
			.surgeries(entity.getSurgeries())
			.symptoms(entity.getSymptoms())
			.recommendedTreatment(entity.getRecommendedTreatment())
			.familyHistory(entity.getFamilyHistory())
			.createdAt(entity.getCreatedAt())
			.updatedAt(entity.getUpdatedAt())
			.build();
	}
}
