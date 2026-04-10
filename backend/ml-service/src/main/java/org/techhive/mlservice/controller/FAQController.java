package org.techhive.mlservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.techhive.mlservice.entity.FAQAnalytics;
import org.techhive.mlservice.service.FAQAnalyticsService;
import org.techhive.mlservice.repository.FAQAnalyticsRepository;

import java.util.List;

@RestController
@RequestMapping("/api/ml/faq")
@RequiredArgsConstructor
@PreAuthorize("hasRole('caregiver')")
public class FAQController {

    private final FAQAnalyticsService faqAnalyticsService;
    private final FAQAnalyticsRepository faqAnalyticsRepository;

    @GetMapping("/top")
    public List<FAQAnalytics> getTopFAQs() {
        return faqAnalyticsService.getTopFAQs(10);
    }

    @GetMapping("/search")
    public List<FAQAnalytics> searchFAQ(@RequestParam("q") String term) {
        return faqAnalyticsRepository.findByQuestionContainingIgnoreCase(term);
    }
}
