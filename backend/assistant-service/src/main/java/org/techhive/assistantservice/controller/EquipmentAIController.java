package org.techhive.assistantservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.assistantservice.dto.EquipmentRecommendRequest;
import org.techhive.assistantservice.dto.EquipmentRecommendResponse;
import org.techhive.assistantservice.service.EquipmentAIService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai/equipment")
@RequiredArgsConstructor
public class EquipmentAIController {

    private final EquipmentAIService equipmentAIService;

    /**
     * POST /api/ai/equipment/recommend
     * Get AI-powered equipment recommendations based on patient condition.
     */
    @PostMapping("/recommend")
    public ResponseEntity<?> recommendEquipment(@Valid @RequestBody EquipmentRecommendRequest request) {
        log.info("Equipment recommendation request: patient={}, condition={}, severity={}",
                request.getPatientId(), request.getCondition(), request.getSeverity());

        try {
            EquipmentRecommendResponse response = equipmentAIService.recommendEquipment(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Equipment recommendation failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Equipment recommendation failed",
                            "message", e.getMessage()
                    ));
        }
    }
}
