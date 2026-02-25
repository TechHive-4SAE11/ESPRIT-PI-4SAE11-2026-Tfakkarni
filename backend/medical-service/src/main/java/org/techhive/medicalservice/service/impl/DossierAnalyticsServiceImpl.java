package org.techhive.medicalservice.service.impl;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.techhive.medicalservice.dto.CrossPatientDiseaseDto;
import org.techhive.medicalservice.dto.DiseaseCountDto;
import org.techhive.medicalservice.dto.DiagnosticsByMonthDto;
import org.techhive.medicalservice.dto.MonthComparisonDto;
import org.techhive.medicalservice.entity.Diagnostics;
import org.techhive.medicalservice.repository.DiagnosticsRepository;
import org.techhive.medicalservice.repository.MedicalFolderRepository;
import org.techhive.medicalservice.service.DossierAnalyticsService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DossierAnalyticsServiceImpl implements DossierAnalyticsService {

    private final DiagnosticsRepository diagnosticsRepository;
    private final MedicalFolderRepository medicalFolderRepository;

    public DossierAnalyticsServiceImpl(DiagnosticsRepository diagnosticsRepository,
                                       MedicalFolderRepository medicalFolderRepository) {
        this.diagnosticsRepository = diagnosticsRepository;
        this.medicalFolderRepository = medicalFolderRepository;
    }

    @Override
    public List<DiseaseCountDto> getTopDiseases(int limit) {
        return diagnosticsRepository.findDiseaseCounts(PageRequest.of(0, limit)).stream()
                .map(row -> DiseaseCountDto.builder()
                        .diseaseName((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<DiagnosticsByMonthDto> getDiagnosticsByMonth(int year) {
        return diagnosticsRepository.findDiagnosticsCountByMonthAndDisease().stream()
                .filter(row -> ((Number) row[0]).intValue() == year)
                .map(row -> DiagnosticsByMonthDto.builder()
                        .year(((Number) row[0]).intValue())
                        .month(((Number) row[1]).intValue())
                        .diseaseName((String) row[2])
                        .count(((Number) row[3]).longValue())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public MonthComparisonDto getMonthComparison() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfThisMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime startOfLastMonth = startOfThisMonth.minusMonths(1);

        long thisMonthDiagnostics = diagnosticsRepository.countByDiagnosisDateAfter(startOfThisMonth);
        long lastMonthDiagnostics = diagnosticsRepository.countByDiagnosisDateBetween(startOfLastMonth, startOfThisMonth);
        long thisMonthFolders = medicalFolderRepository.countByCreatedAtAfter(startOfThisMonth);
        long lastMonthFolders = medicalFolderRepository.countByCreatedAtBetween(startOfLastMonth, startOfThisMonth);

        return MonthComparisonDto.builder()
                .thisMonthDiagnostics(thisMonthDiagnostics)
                .lastMonthDiagnostics(lastMonthDiagnostics)
                .thisMonthFolders(thisMonthFolders)
                .lastMonthFolders(lastMonthFolders)
                .build();
    }

    @Override
    public List<CrossPatientDiseaseDto> getByDisease(String diseaseName, String stage) {
        if (diseaseName == null || diseaseName.isBlank()) {
            return Collections.emptyList();
        }
        String stageParam = (stage != null && !stage.isBlank()) ? stage.trim() : null;
        List<Diagnostics> list = diagnosticsRepository.findByDiseaseNameContainingIgnoreCaseAndOptionalStage(diseaseName.trim(), stageParam);
        return list.stream()
                .map(d -> CrossPatientDiseaseDto.builder()
                        .diagnosticsId(d.getId())
                        .medicalFolderId(d.getMedicalFolder().getId())
                        .patientId(d.getMedicalFolder().getPatientId())
                        .doctorId(d.getMedicalFolder().getDoctorId())
                        .diseaseName(d.getDiseaseName())
                        .stage(d.getStage())
                        .diagnosisDate(d.getDiagnosisDate())
                        .build())
                .collect(Collectors.toList());
    }
}
