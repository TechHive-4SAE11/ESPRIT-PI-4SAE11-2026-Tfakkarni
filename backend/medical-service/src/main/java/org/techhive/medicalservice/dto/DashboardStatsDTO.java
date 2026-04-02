package org.techhive.medicalservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDTO {
    private Long totalAppointments;
    private Double globalNoShowRate;
    private Double monthlyNoShowRate;
    private List<PatientRiskDTO> highRiskPatients;
    private List<PatientRiskDTO> upcomingAppointments;
    private Map<String, Integer> cancellationsByDay;
    private Map<String, Double> noShowRateByDoctor;
}
