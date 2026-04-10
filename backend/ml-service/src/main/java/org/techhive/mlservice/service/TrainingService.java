package org.techhive.mlservice.service;

import org.techhive.mlservice.dto.ModuleDTO;
import org.techhive.mlservice.dto.ProgressDTO;
import org.techhive.mlservice.entity.TrainingModule;

import java.util.List;

public interface TrainingService {
    List<ModuleDTO> getModules();

    ModuleDTO getModuleById(Long id);

    ProgressDTO getUserProgress(Long userId);

    void markModuleCompleted(Long userId, Long moduleId, Double score);

    TrainingModule createModule(TrainingModule module);

    TrainingModule updateModule(Long id, TrainingModule module);

    void deleteModule(Long id);

    List<TrainingModule> getAllModules();
}
