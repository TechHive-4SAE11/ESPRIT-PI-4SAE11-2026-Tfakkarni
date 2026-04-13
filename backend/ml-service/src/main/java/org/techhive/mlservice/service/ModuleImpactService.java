package org.techhive.mlservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.techhive.mlservice.dto.ModuleImpactDTO;
import org.techhive.mlservice.dto.ModuleDTO;
import org.techhive.mlservice.entity.UserProgress;
import org.techhive.mlservice.entity.CaregiverStressHistory;
import org.techhive.mlservice.entity.ComplianceHistory;
import org.techhive.mlservice.repository.UserProgressRepository;
import org.techhive.mlservice.repository.CaregiverStressHistoryRepository;
import org.techhive.mlservice.repository.ComplianceHistoryRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ModuleImpactService {

    private final UserProgressRepository userProgressRepository;
    private final CaregiverStressHistoryRepository stressRepository;
    private final ComplianceHistoryRepository complianceRepository;
    private final TrainingService trainingService;

    public List<ModuleImpactDTO> getModuleImpacts(Long userId) {
        List<ModuleImpactDTO> impacts = new ArrayList<>();

        try {
            List<UserProgress> completedModules = userProgressRepository.findByUserIdAndCompletedTrue(userId);

            for (UserProgress progress : completedModules) {
                try {
                    ModuleDTO module = trainingService.getModuleById(progress.getModuleId());
                    LocalDateTime completedDate = progress.getCompletedAt();

                    if (completedDate == null) continue;

                    // Stress avant/après
                    Integer stressBefore = getAverageStressBefore(userId, completedDate);
                    Integer stressAfter = getAverageStressAfter(userId, completedDate);
                    Integer stressImprovement = (stressBefore != null && stressAfter != null) ? stressBefore - stressAfter : null;

                    // Observance avant/après
                    Integer observanceBefore = getAverageObservanceBefore(userId, completedDate);
                    Integer observanceAfter = getAverageObservanceAfter(userId, completedDate);
                    Integer observanceImprovement = (observanceBefore != null && observanceAfter != null) ? observanceAfter - observanceBefore : null;

                    String impactMessage = generateImpactMessage(stressImprovement, observanceImprovement);

                    ModuleImpactDTO impact = new ModuleImpactDTO(
                            module.getId(),
                            module.getTitle(),
                            module.getCategory(),
                            completedDate,
                            stressBefore,
                            stressAfter,
                            stressImprovement,
                            observanceBefore,
                            observanceAfter,
                            observanceImprovement,
                            impactMessage
                    );
                    impacts.add(impact);

                } catch (Exception e) {
                    System.err.println("Erreur traitement module " + progress.getModuleId() + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("Erreur getModuleImpacts: " + e.getMessage());
            e.printStackTrace();
        }

        return impacts;
    }

    private Integer getAverageStressBefore(Long userId, LocalDateTime date) {
        try {
            List<CaregiverStressHistory> stresses = stressRepository
                    .findByCaregiverIdAndCreatedAtAfterOrderByCreatedAtAsc(String.valueOf(userId), date.minusDays(30));

            if (stresses == null || stresses.isEmpty()) return null;

            stresses = stresses.stream()
                    .filter(s -> s.getCreatedAt() != null && s.getCreatedAt().isBefore(date))
                    .toList();

            if (stresses.isEmpty()) return null;
            return (int) stresses.stream().mapToInt(CaregiverStressHistory::getStressScore).average().orElse(0);

        } catch (Exception e) {
            System.err.println("Erreur getAverageStressBefore: " + e.getMessage());
            return null;
        }
    }

    private Integer getAverageStressAfter(Long userId, LocalDateTime date) {
        try {
            List<CaregiverStressHistory> stresses = stressRepository
                    .findByCaregiverIdAndCreatedAtAfterOrderByCreatedAtAsc(String.valueOf(userId), date);

            if (stresses == null || stresses.isEmpty()) return null;
            return (int) stresses.stream().mapToInt(CaregiverStressHistory::getStressScore).average().orElse(0);

        } catch (Exception e) {
            System.err.println("Erreur getAverageStressAfter: " + e.getMessage());
            return null;
        }
    }

    private Integer getAverageObservanceBefore(Long userId, LocalDateTime date) {
        try {
            List<ComplianceHistory> compliances = complianceRepository
                    .findByPatientIdAndDateAfterOrderByDateAsc(String.valueOf(userId), date.minusDays(30));

            if (compliances == null || compliances.isEmpty()) return null;

            compliances = compliances.stream()
                    .filter(c -> c.getDate() != null && c.getDate().isBefore(date))
                    .toList();

            if (compliances.isEmpty()) return null;
            return (int) compliances.stream().mapToDouble(ComplianceHistory::getScore).average().orElse(0);

        } catch (Exception e) {
            System.err.println("Erreur getAverageObservanceBefore: " + e.getMessage());
            return null;
        }
    }

    private Integer getAverageObservanceAfter(Long userId, LocalDateTime date) {
        try {
            List<ComplianceHistory> compliances = complianceRepository
                    .findByPatientIdAndDateAfterOrderByDateAsc(String.valueOf(userId), date);

            if (compliances == null || compliances.isEmpty()) return null;
            return (int) compliances.stream().mapToDouble(ComplianceHistory::getScore).average().orElse(0);

        } catch (Exception e) {
            System.err.println("Erreur getAverageObservanceAfter: " + e.getMessage());
            return null;
        }
    }

    private String generateImpactMessage(Integer stressImprovement, Integer observanceImprovement) {
        if (stressImprovement != null && stressImprovement > 10) {
            return "📉 Votre stress a baissé de " + stressImprovement + " points !";
        }
        if (observanceImprovement != null && observanceImprovement > 10) {
            return "📈 Votre observance s'est améliorée de " + observanceImprovement + "% !";
        }
        if (stressImprovement != null && stressImprovement > 0) {
            return "✅ Léger mieux sur votre niveau de stress";
        }
        if (observanceImprovement != null && observanceImprovement > 0) {
            return "✅ Petite amélioration de votre observance";
        }
        return "👍 Module complété, continuez votre progression !";
    }
}