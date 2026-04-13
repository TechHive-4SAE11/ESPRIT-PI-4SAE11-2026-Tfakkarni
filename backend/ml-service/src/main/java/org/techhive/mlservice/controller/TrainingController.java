package org.techhive.mlservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.techhive.mlservice.dto.ModuleDTO;
import org.techhive.mlservice.dto.ProgressDTO;
import org.techhive.mlservice.dto.ModuleImpactDTO;
import org.techhive.mlservice.service.TrainingService;
import org.techhive.mlservice.service.RecommendationService;
import org.techhive.mlservice.service.ModuleImpactService;

import java.util.List;

@RestController
@RequestMapping("/api/ml/training")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class TrainingController {

    private final TrainingService trainingService;
    private final RecommendationService recommendationService;
    private final ModuleImpactService moduleImpactService;  // ← NOUVEAU

    @GetMapping("/modules")
    public List<ModuleDTO> getModules() {
        return trainingService.getModules();
    }

    @GetMapping("/modules/{id}")
    public ModuleDTO getModuleById(@PathVariable Long id) {
        return trainingService.getModuleById(id);
    }

    @GetMapping("/progress/{userId}")
    public ProgressDTO getUserProgress(@PathVariable Long userId) {
        return trainingService.getUserProgress(userId);
    }

    @PostMapping("/complete")
    public void markModuleCompleted(@RequestParam Long userId,
                                    @RequestParam Long moduleId,
                                    @RequestParam(required = false) Double score) {
        trainingService.markModuleCompleted(userId, moduleId, score != null ? score : 100.0);
    }

    @GetMapping("/recommendations/{userId}")
    public List<ModuleDTO> getRecommendations(@PathVariable Long userId) {
        List<Long> recommendedIds = recommendationService.recommendModules(String.valueOf(userId));
        return recommendedIds.stream()
                .map(trainingService::getModuleById)
                .toList();
    }

    // NOUVEAU : Récupérer l'impact des modules complétés
    @GetMapping("/impact/{userId}")
    public List<ModuleImpactDTO> getModuleImpacts(@PathVariable Long userId) {
        return moduleImpactService.getModuleImpacts(userId);
    }
}