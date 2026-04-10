package org.techhive.mlservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.techhive.mlservice.entity.FAQAnalytics;
import java.util.List;

@Repository
public interface FAQAnalyticsRepository extends JpaRepository<FAQAnalytics, Long> {
    List<FAQAnalytics> findTop10ByOrderByFrequencyDesc();
    List<FAQAnalytics> findByQuestionContainingIgnoreCase(String question);
}
