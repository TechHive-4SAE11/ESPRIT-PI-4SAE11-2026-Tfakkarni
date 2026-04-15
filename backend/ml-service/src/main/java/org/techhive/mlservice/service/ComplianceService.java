package org.techhive.mlservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.techhive.mlservice.client.MedicalServiceClient;
import org.techhive.mlservice.dto.AppointmentResponseDTO;
import org.techhive.mlservice.entity.ComplianceHistory;
import org.techhive.mlservice.repository.ComplianceHistoryRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComplianceService {

    private final MedicalServiceClient medicalServiceClient;
    private final ComplianceHistoryRepository complianceHistoryRepository;

    public Map<String, Object> calculateCompliance(String patientId) {
        List<AppointmentResponseDTO> appointments = medicalServiceClient.getAppointments(patientId);
        
        Map<String, Object> result = new HashMap<>();

        if (appointments == null || appointments.isEmpty()) {
            saveHistory(patientId, 100.0);
            result.put("score", 100.0);
            result.put("severity", "INFO");
            result.put("message", "Aucun rendez-vous trouvé.");
            return result;
        }

        // Constraint: Boucles imbriquées pour calcul score
        Map<Integer, List<AppointmentResponseDTO>> appointmentsByYear = appointments.stream()
                .filter(a -> a.getStartTime() != null)
                .collect(Collectors.groupingBy(a -> a.getStartTime().getYear()));

        int totalAnalyzed = 0;
        int penalty = 0;

        for (Map.Entry<Integer, List<AppointmentResponseDTO>> entry : appointmentsByYear.entrySet()) {
            for (AppointmentResponseDTO apt : entry.getValue()) {
                totalAnalyzed++;
                String status = apt.getStatus();
                if ("CANCELLED".equalsIgnoreCase(status) || "MISSED".equalsIgnoreCase(status)) {
                    penalty += 20; // -20 points par absence ou annulation
                }
            }
        }

        double score = Math.max(0, 100 - (totalAnalyzed == 0 ? 0 : penalty));
        
        saveHistory(patientId, score);
        
        result.put("score", score);

        if (score < 30) {
            result.put("severity", "CRITIQUE");
            result.put("message", "Alerte critique : mauvaise observance");
        } else if (score <= 50) {
            result.put("severity", "MODEREE");
            result.put("message", "Activation rappels SMS");
        } else {
            result.put("severity", "INFO");
            result.put("message", "Message encouragement");
        }

        return result;
    }
    
    private void saveHistory(String patientId, double score) {
        ComplianceHistory history = new ComplianceHistory();
        history.setPatientId(patientId);
        history.setScore(score);
        history.setDate(LocalDateTime.now());
        complianceHistoryRepository.save(history);
    }
}
