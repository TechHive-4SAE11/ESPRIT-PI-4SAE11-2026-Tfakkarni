package org.techhive.medicalservice.mapper;

import org.techhive.medicalservice.dto.CreateDiagnosticsRequest;
import org.techhive.medicalservice.dto.DiagnosticsResponse;
import org.techhive.medicalservice.dto.UpdateDiagnosticsRequest;
import org.techhive.medicalservice.entity.Diagnostics;
import org.techhive.medicalservice.entity.MedicalFolder;

public class DiagnosticsMapper {
	public static Diagnostics toEntity(CreateDiagnosticsRequest request, MedicalFolder medicalFolder) {
		return Diagnostics.builder()
			.medicalFolder(medicalFolder)
			.diseaseName(request.getDiseaseName())
			.stage(request.getStage())
			.comorbidities(request.getComorbidities())
			.diagnosisDate(request.getDiagnosisDate())
			.build();
	}

	public static Diagnostics toEntity(UpdateDiagnosticsRequest request, Diagnostics existing) {
		if (request.getDiseaseName() != null) {
			existing.setDiseaseName(request.getDiseaseName());
		}
		if (request.getStage() != null) {
			existing.setStage(request.getStage());
		}
		if (request.getComorbidities() != null) {
			existing.setComorbidities(request.getComorbidities());
		}
		if (request.getDiagnosisDate() != null) {
			existing.setDiagnosisDate(request.getDiagnosisDate());
		}
		return existing;
	}

	public static DiagnosticsResponse toResponse(Diagnostics entity) {
		return DiagnosticsResponse.builder()
			.id(entity.getId())
			.medicalFolderId(entity.getMedicalFolder().getId())
			.diseaseName(entity.getDiseaseName())
			.stage(entity.getStage())
			.comorbidities(entity.getComorbidities())
			.diagnosisDate(entity.getDiagnosisDate())
			.createdAt(entity.getCreatedAt())
			.updatedAt(entity.getUpdatedAt())
			.build();
	}
}
