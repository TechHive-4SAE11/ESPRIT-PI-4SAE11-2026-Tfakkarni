package org.techhive.medicalservice.service;

import org.techhive.medicalservice.dto.CrossPatientDiseaseDto;
import org.techhive.medicalservice.dto.DiseaseCountDto;
import org.techhive.medicalservice.dto.DiagnosticsByMonthDto;
import org.techhive.medicalservice.dto.MonthComparisonDto;
import org.techhive.medicalservice.dto.ClinicalSafetyStatsDto;

import java.util.List;

public interface DossierAnalyticsService {

    List<DiseaseCountDto> getTopDiseases(int limit);

    List<DiagnosticsByMonthDto> getDiagnosticsByMonth(int year);

    MonthComparisonDto getMonthComparison();

    List<CrossPatientDiseaseDto> getByDisease(String diseaseName, String stage);

    ClinicalSafetyStatsDto getClinicalSafetyStats();

    org.techhive.medicalservice.dto.FolderSpecificStatsDto getFolderStats(Long folderId);
}
