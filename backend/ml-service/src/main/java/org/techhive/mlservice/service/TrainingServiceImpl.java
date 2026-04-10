package org.techhive.mlservice.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.mlservice.dto.ModuleDTO;
import org.techhive.mlservice.dto.ProgressDTO;
import org.techhive.mlservice.entity.TrainingModule;
import org.techhive.mlservice.entity.UserProgress;
import org.techhive.mlservice.repository.TrainingModuleRepository;
import org.techhive.mlservice.repository.UserProgressRepository;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TrainingServiceImpl implements TrainingService {

    private final TrainingModuleRepository trainingModuleRepository;
    private final UserProgressRepository userProgressRepository;

    @Override
    public List<ModuleDTO> getModules() {
        return trainingModuleRepository.findByActiveTrue().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ModuleDTO getModuleById(Long id) {
        TrainingModule module = trainingModuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Module not found: " + id));
        return mapToDTO(module);
    }

    @Override
    public ProgressDTO getUserProgress(Long userId) {
        List<UserProgress> userProgress = userProgressRepository.findByUserId(userId);
        long completed = userProgress.stream().filter(UserProgress::getCompleted).count();
        long total = trainingModuleRepository.count();

        double percentage = total > 0 ? (double) completed / total * 100 : 0.0;
        return new ProgressDTO((int) completed, (int) total, percentage);
    }

    @Override
    public void markModuleCompleted(Long userId, Long moduleId, Double score) {
        UserProgress progress = userProgressRepository.findByUserId(userId).stream()
                .filter(p -> p.getModuleId().equals(moduleId))
                .findFirst()
                .orElse(UserProgress.builder()
                        .userId(userId)
                        .moduleId(moduleId)
                        .startedAt(LocalDateTime.now())
                        .build());

        progress.setCompleted(true);
        progress.setScore(score);
        progress.setCompletedAt(LocalDateTime.now());
        progress.setLastActivityAt(LocalDateTime.now());

        userProgressRepository.save(progress);
    }

    private ModuleDTO mapToDTO(TrainingModule entity) {
        return new ModuleDTO(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getDifficulty(),
                entity.getDuration());
    }

    @Override
    public TrainingModule createModule(TrainingModule module) {
        module.setCreatedAt(LocalDateTime.now());
        module.setUpdatedAt(LocalDateTime.now());
        return trainingModuleRepository.save(module);
    }

    @Override
    public TrainingModule updateModule(Long id, TrainingModule module) {
        TrainingModule existing = trainingModuleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Module non trouvé avec id: " + id));
        existing.setTitle(module.getTitle());
        existing.setDescription(module.getDescription());
        existing.setCategory(module.getCategory());
        existing.setDifficulty(module.getDifficulty());
        existing.setDuration(module.getDuration());
        existing.setVideoUrl(module.getVideoUrl());
        existing.setPdfUrl(module.getPdfUrl());
        existing.setActive(module.getActive());
        existing.setUpdatedAt(LocalDateTime.now());
        return trainingModuleRepository.save(existing);
    }

    @Override
    public void deleteModule(Long id) {
        trainingModuleRepository.deleteById(id);
    }

    @Override
    public List<TrainingModule> getAllModules() {
        return trainingModuleRepository.findAll();
    }
}
