package org.techhive.mlservice.repository;

import org.techhive.mlservice.entity.UserProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {
    List<UserProgress> findByUserId(Long userId);
    List<UserProgress> findByUserIdAndCompletedTrue(Long userId);
}
