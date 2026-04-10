package org.techhive.mlservice.repository;

import org.techhive.mlservice.entity.TrainingModule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TrainingModuleRepository extends JpaRepository<TrainingModule, Long> {
    List<TrainingModule> findByActiveTrue();
    List<TrainingModule> findByCategory(String category);
}
