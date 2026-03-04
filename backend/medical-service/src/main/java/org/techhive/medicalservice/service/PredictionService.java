package org.techhive.medicalservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.techhive.medicalservice.dto.DashboardStatsDTO;
import org.techhive.medicalservice.dto.PatientRiskDTO;
import org.techhive.medicalservice.dto.PredictionDTO;
import org.techhive.medicalservice.entity.Appointment;
import org.techhive.medicalservice.entity.AppointmentStatus;
import org.techhive.medicalservice.entity.PredictionResult;
import org.techhive.medicalservice.repository.AppointmentRepository;
import org.techhive.medicalservice.repository.PredictionResultRepository;

import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PredictionService {

    private final AppointmentRepository appointmentRepository;
    private final PredictionResultRepository predictionResultRepository;

    public PredictionDTO predictForAppointment(Long appointmentId) {
        Appointment current = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        List<Appointment> allAppointments = appointmentRepository.findByPatientId(current.getPatientId());

        List<Appointment> pastAppointments = allAppointments.stream()
                .filter(a -> a.getStartTime().isBefore(current.getStartTime()) && !a.getId().equals(current.getId()))
                .sorted(Comparator.comparing(Appointment::getStartTime).reversed())
                .toList();

        int riskScore = 0;
        Map<String, Object> factors = new HashMap<>();

        if (!pastAppointments.isEmpty()) {
            long totalPast = pastAppointments.size();
            long cancelledPast = pastAppointments.stream()
                    .filter(a -> a.getStatus() == AppointmentStatus.CANCELLED)
                    .count();

            double cancelRate = (double) cancelledPast / totalPast;
            int score1 = (int) (cancelRate * 35);
            riskScore += score1;
            factors.put("historicalRate", score1);

            List<Appointment> cancelledList = pastAppointments.stream()
                    .filter(a -> a.getStatus() == AppointmentStatus.CANCELLED)
                    .toList();

            boolean sameDayRisk = cancelledList.stream()
                    .anyMatch(a -> a.getStartTime().getDayOfWeek() == current.getStartTime().getDayOfWeek());
            if (sameDayRisk) {
                riskScore += 20;
                factors.put("sameDayOfWeek", 20);
            } else {
                factors.put("sameDayOfWeek", 0);
            }

            Appointment lastAppt = pastAppointments.get(0);
            if (lastAppt.getStatus() == AppointmentStatus.CANCELLED) {
                long daysSinceLast = ChronoUnit.DAYS.between(lastAppt.getStartTime(), current.getStartTime());
                if (daysSinceLast < 14) {
                    riskScore += 20;
                    factors.put("lastApptCancelledRecently", 20);
                } else {
                    factors.put("lastApptCancelledRecently", 0);
                }
            } else {
                factors.put("lastApptCancelledRecently", 0);
            }

            boolean sameTimeRisk = cancelledList.stream()
                    .anyMatch(a -> a.getStartTime().getHour() == current.getStartTime().getHour());
            if (sameTimeRisk) {
                riskScore += 10;
                factors.put("sameTime", 10);
            } else {
                factors.put("sameTime", 0);
            }

            boolean sameDoctorRisk = cancelledList.stream()
                    .anyMatch(a -> Objects.equals(a.getDoctorId(), current.getDoctorId()));
            if (sameDoctorRisk) {
                riskScore += 10;
                factors.put("sameDoctor", 10);
            } else {
                factors.put("sameDoctor", 0);
            }

            long monthsSinceLast = ChronoUnit.MONTHS.between(lastAppt.getStartTime(), current.getStartTime());
            if (monthsSinceLast > 3) {
                riskScore += 5;
                factors.put("delaySinceLast", 5);
            } else {
                factors.put("delaySinceLast", 0);
            }
        } else {
            factors.put("historicalRate", 0);
            factors.put("sameDayOfWeek", 0);
            factors.put("lastApptCancelledRecently", 0);
            factors.put("sameTime", 0);
            factors.put("sameDoctor", 0);
            factors.put("delaySinceLast", 0);
        }

        riskScore = Math.min(riskScore, 100);

        String riskLevel;
        String recommendation;
        if (riskScore <= 30) {
            riskLevel = "GREEN";
            recommendation = "Aucun risque particulier";
        } else if (riskScore <= 60) {
            riskLevel = "ORANGE";
            recommendation = "Rappel téléphonique conseillé";
        } else {
            riskLevel = "RED";
            recommendation = "Confirmation manuelle exigée ou double surréservation";
        }

        PredictionResult result = PredictionResult.builder()
                .appointmentId(appointmentId)
                .patientId(current.getPatientId())
                .riskScore(riskScore)
                .riskLevel(riskLevel)
                .factors(factors)
                .recommendation(recommendation)
                .createdAt(LocalDateTime.now())
                .build();

        predictionResultRepository.save(result);

        return PredictionDTO.builder()
                .riskScore(riskScore)
                .riskLevel(riskLevel)
                .factors(factors)
                .recommendation(recommendation)
                .build();
    }

    public DashboardStatsDTO getDashboardStats() {
        List<Appointment> allAppointments = appointmentRepository.findAll();

        long totalAppointments = allAppointments.size();

        long globalCancelled = allAppointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CANCELLED)
                .count();
        double globalNoShowRate = totalAppointments > 0 ? (double) globalCancelled / totalAppointments * 100 : 0.0;

        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);
        List<Appointment> monthlyApps = allAppointments.stream()
                .filter(a -> a.getStartTime().isAfter(startOfMonth))
                .toList();
        long monthlyCancelled = monthlyApps.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CANCELLED)
                .count();
        double monthlyNoShowRate = !monthlyApps.isEmpty() ? (double) monthlyCancelled / monthlyApps.size() * 100 : 0.0;

        Map<String, Integer> cancellationsByDay = new HashMap<>();
        List<Appointment> allCancelled = allAppointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CANCELLED)
                .toList();
        for (Appointment a : allCancelled) {
            String day = a.getStartTime().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH);
            cancellationsByDay.put(day, cancellationsByDay.getOrDefault(day, 0) + 1);
        }

        Map<String, List<Appointment>> appsByDoctor = allAppointments.stream()
                .filter(a -> a.getDoctorId() != null)
                .collect(Collectors.groupingBy(Appointment::getDoctorId));
        Map<String, Double> noShowRateByDoctor = new HashMap<>();
        appsByDoctor.forEach((docId, apps) -> {
            long cancelledCount = apps.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED).count();
            double rate = apps.isEmpty() ? 0.0 : (double) cancelledCount / apps.size() * 100;
            noShowRateByDoctor.put("Dr. " + docId, rate);
        });

        List<PatientRiskDTO> highRiskPatients = predictionResultRepository.findByRiskScoreGreaterThanEqual(70).stream()
                .map(r -> {
                    Appointment a = appointmentRepository.findById(r.getAppointmentId()).orElse(null);
                    if (a == null)
                        return null;
                    return PatientRiskDTO.builder()
                            .appointmentId(a.getId())
                            .patientId(a.getPatientId())
                            .title(a.getTitle())
                            .date(a.getStartTime().toLocalDate())
                            .time(a.getStartTime().toLocalTime())
                            .doctorId(a.getDoctorId())
                            .riskScore(r.getRiskScore())
                            .riskLevel(r.getRiskLevel())
                            .recommendation(r.getRecommendation())
                            .build();
                })
                .filter(Objects::nonNull)
                .limit(10)
                .toList();

        List<PatientRiskDTO> upcomingAppointments = getUpcomingAppointmentsWithRisk();

        return DashboardStatsDTO.builder()
                .totalAppointments(totalAppointments)
                .globalNoShowRate(globalNoShowRate)
                .monthlyNoShowRate(monthlyNoShowRate)
                .highRiskPatients(highRiskPatients)
                .upcomingAppointments(upcomingAppointments)
                .cancellationsByDay(cancellationsByDay)
                .noShowRateByDoctor(noShowRateByDoctor)
                .build();
    }

    public List<PatientRiskDTO> getUpcomingAppointmentsWithRisk() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next7Days = now.plusDays(7);
        List<Appointment> upcoming = appointmentRepository.findByDateRange(now, next7Days);

        return upcoming.stream().map(a -> {
            Optional<PredictionResult> predictionOpt = predictionResultRepository
                    .findTopByAppointmentIdOrderByCreatedAtDesc(a.getId());
            Integer riskScore = 0;
            String riskLevel = "GREEN";
            String recommendation = "Aucun risque particulier";

            if (predictionOpt.isPresent()) {
                PredictionResult p = predictionOpt.get();
                riskScore = p.getRiskScore();
                riskLevel = p.getRiskLevel();
                recommendation = p.getRecommendation();
            } else {
                PredictionDTO pred = predictForAppointment(a.getId());
                riskScore = pred.getRiskScore();
                riskLevel = pred.getRiskLevel();
                recommendation = pred.getRecommendation();
            }

            return PatientRiskDTO.builder()
                    .appointmentId(a.getId())
                    .patientId(a.getPatientId())
                    .title(a.getTitle())
                    .date(a.getStartTime().toLocalDate())
                    .time(a.getStartTime().toLocalTime())
                    .doctorId(a.getDoctorId())
                    .riskScore(riskScore)
                    .riskLevel(riskLevel)
                    .recommendation(recommendation)
                    .build();
        }).toList();
    }
}
