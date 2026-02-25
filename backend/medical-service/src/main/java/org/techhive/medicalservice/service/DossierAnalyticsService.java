package org.techhive.medicalservice.service;

import org.techhive.medicalservice.dto.CrossPatientDiseaseDto;
import org.techhive.medicalservice.dto.DiseaseCountDto;
import org.techhive.medicalservice.dto.DiagnosticsByMonthDto;
import org.techhive.medicalservice.dto.MonthComparisonDto;

import java.util.List;

public interface DossierAnalyticsService {

    List<DiseaseCountDto> getTopDiseases(int limit);

    List<DiagnosticsByMonthDto> getDiagnosticsByMonth(int year);

    MonthComparisonDto getMonthComparison();

    List<CrossPatientDiseaseDto> getByDisease(String diseaseName, String stage);
}
