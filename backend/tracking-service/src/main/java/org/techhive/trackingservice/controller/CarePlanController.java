package org.techhive.trackingservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.trackingservice.dto.CarePlanRequestDTO;
import org.techhive.trackingservice.dto.CarePlanResponseDTO;
import org.techhive.trackingservice.entity.CareActivity;
import org.techhive.trackingservice.entity.CarePlan;
import org.techhive.trackingservice.mapper.CarePlanMapper;
import org.techhive.trackingservice.service.CarePlanService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/care-plans")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CarePlanController {

    private final CarePlanService carePlanService;
    private final CarePlanMapper carePlanMapper;

    @PostMapping
    public ResponseEntity<?> createCarePlan(@RequestBody CarePlanRequestDTO requestDTO) {
        try {
            log.info("Received care plan creation request: sessionId={}, activitiesCount={}",
                requestDTO.getSessionId(),
                requestDTO.getActivities() != null ? requestDTO.getActivities().size() : 0);

            // Validation
            if (requestDTO.getSessionId() == null) {
                log.warn("Session ID is missing in care plan request");
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Session ID is required"));
            }

            if (requestDTO.getActivities() == null || requestDTO.getActivities().isEmpty()) {
                log.warn("No activities provided in care plan request");
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "At least one activity is required"));
            }
            
            CarePlan carePlan = new CarePlan();
            
            // Map activities
            List<CareActivity> activities = requestDTO.getActivities().stream()
                    .map(carePlanMapper::toActivityEntity)
                    .collect(Collectors.toList());
            carePlan.setCareActivities(activities);

            CarePlan saved = carePlanService.createCarePlanForSession(requestDTO.getSessionId(), carePlan);
            log.info("Care plan created successfully with ID: {}", saved.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(carePlanMapper.toResponseDTO(saved));
        } catch (IllegalArgumentException e) {
            log.error("Validation error creating care plan: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Error creating care plan", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create care plan: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<CarePlanResponseDTO>> getAllCarePlans() {
        List<CarePlan> carePlans = carePlanService.getAllCarePlans();
        List<CarePlanResponseDTO> responseDTOs = carePlans.stream()
                .map(carePlanMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CarePlanResponseDTO> getCarePlanById(@PathVariable Long id) {
        return carePlanService.getCarePlanById(id)
                .map(carePlan -> ResponseEntity.ok(carePlanMapper.toResponseDTO(carePlan)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCarePlan(@PathVariable Long id, @RequestBody CarePlanRequestDTO requestDTO) {
        try {
            log.info("Received care plan update request: id={}, activitiesCount={}", 
                id, requestDTO.getActivities() != null ? requestDTO.getActivities().size() : 0);

            if (requestDTO.getActivities() == null || requestDTO.getActivities().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "At least one activity is required"));
            }

            CarePlan carePlanUpdates = new CarePlan();
            List<CareActivity> activities = requestDTO.getActivities().stream()
                    .map(carePlanMapper::toActivityEntity)
                    .collect(Collectors.toList());
            carePlanUpdates.setCareActivities(activities);

            CarePlan updated = carePlanService.updateCarePlan(id, carePlanUpdates);
            return ResponseEntity.ok(carePlanMapper.toResponseDTO(updated));
        } catch (RuntimeException e) {
            log.error("Error updating care plan", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update care plan: " + e.getMessage()));
        }
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<CarePlanResponseDTO>> getCarePlansBySession(@PathVariable Long sessionId) {
        List<CarePlan> carePlans = carePlanService.getCarePlansBySession(sessionId);
        List<CarePlanResponseDTO> responseDTOs = carePlans.stream()
                .map(carePlanMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/patient/{idPatient}")
    public ResponseEntity<List<CarePlanResponseDTO>> getCarePlansByPatient(@PathVariable String idPatient) {
        List<CarePlan> carePlans = carePlanService.getCarePlansByPatient(idPatient);
        List<CarePlanResponseDTO> responseDTOs = carePlans.stream()
                .map(carePlanMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @PatchMapping("/activities/{activityId}/status")
    public ResponseEntity<?> updateActivityStatus(@PathVariable Long activityId, @RequestBody Map<String, String> statusMap) {
        String status = statusMap.get("status");
        if (status == null || status.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Status is required"));
        }
        
        try {
            CareActivity updatedActivity = carePlanService.updateActivityStatus(activityId, status);
            return ResponseEntity.ok(carePlanMapper.toActivityResponseDTO(updatedActivity));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCarePlan(@PathVariable Long id) {
        try {
            carePlanService.deleteCarePlan(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
