package org.techhive.medicalservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.techhive.medicalservice.service.coaching.CoachingStaleScheduler;

import java.util.Map;

@RestController
@RequestMapping({"/api/coaching/scheduler", "/api/medical-folders/coaching/scheduler"})
@RequiredArgsConstructor
public class CoachingSchedulerController {

    private final CoachingStaleScheduler coachingStaleScheduler;

    /** Manual test hook: trigger stale-goal reminders immediately. */
    @PostMapping("/stale/run-now")
    public ResponseEntity<Map<String, Object>> runStaleNow() {
        int sent = coachingStaleScheduler.runNowForTest();
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "sent", sent
        ));
    }

    @GetMapping("/mode")
    public ResponseEntity<Map<String, Object>> getMode() {
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "demoMode", coachingStaleScheduler.isDemoMode()
        ));
    }

    @PostMapping("/mode")
    public ResponseEntity<Map<String, Object>> setMode(@RequestParam("demo") boolean demo) {
        coachingStaleScheduler.setDemoMode(demo);
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "demoMode", coachingStaleScheduler.isDemoMode()
        ));
    }
}
