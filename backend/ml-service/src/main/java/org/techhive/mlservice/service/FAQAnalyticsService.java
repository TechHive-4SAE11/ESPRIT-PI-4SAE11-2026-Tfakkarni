package org.techhive.mlservice.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.mlservice.entity.FAQAnalytics;
import org.techhive.mlservice.repository.FAQAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FAQAnalyticsService {

    private final FAQAnalyticsRepository faqAnalyticsRepository;

    public void analyzeFAQs() {
        // Logic to analyze current questions (batch job proxy)
        // Groups recent chat questions and updates the FAQ metrics.
        System.out.println("Executing FAQ Analysis Batch Job...");
    }

    public List<FAQAnalytics> getTopFAQs(int limit) {
        List<FAQAnalytics> topList = faqAnalyticsRepository.findTop10ByOrderByFrequencyDesc();
        if (topList.size() > limit) {
            return topList.subList(0, limit);
        }
        return topList;
    }
}
