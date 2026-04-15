package org.techhive.mlservice.repository;

import org.techhive.mlservice.entity.CaregiverStressHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CaregiverStressHistoryRepository extends JpaRepository<CaregiverStressHistory, Long> {

    Optional<CaregiverStressHistory> findTopByCaregiverIdOrderByCreatedAtDesc(String caregiverId);

    List<CaregiverStressHistory> findByCaregiverIdAndCreatedAtAfterOrderByCreatedAtAsc(String caregiverId, LocalDateTime date);

    @Query("SELECT AVG(c.stressScore) FROM CaregiverStressHistory c WHERE c.caregiverId = :caregiverId AND c.createdAt >= :startDate")
    Double getAverageScoreSince(@Param("caregiverId") String caregiverId, @Param("startDate") LocalDateTime startDate);
}