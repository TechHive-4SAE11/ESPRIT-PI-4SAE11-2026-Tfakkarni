package org.techhive.analyticsservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.analyticsservice.dto.PlatformOverviewResponse;
import org.techhive.analyticsservice.service.PlatformAnalyticsService;

@RestController
@RequestMapping("/api/analytics/platform")
@RequiredArgsConstructor
public class PlatformAnalyticsController {

    private final PlatformAnalyticsService platformService;

    @GetMapping("/overview")
    public ResponseEntity<PlatformOverviewResponse> getPlatformOverview() {
        return ResponseEntity.ok(platformService.getPlatformOverview());
    }
}
