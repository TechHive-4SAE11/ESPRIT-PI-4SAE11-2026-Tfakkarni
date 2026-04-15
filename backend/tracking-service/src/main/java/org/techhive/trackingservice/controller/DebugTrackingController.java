package org.techhive.trackingservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.techhive.trackingservice.repository.PrescriptionRepository;
import org.techhive.trackingservice.repository.MedicationRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
public class DebugTrackingController {

    private final PrescriptionRepository prescriptionRepository;
    private final MedicationRepository medicationRepository;

    @GetMapping("/debug-data")
    public Map<String, Object> debugData() {
        Map<String, Object> debug = new HashMap<>();
        
        long prescCount = prescriptionRepository.count();
        long medCount = medicationRepository.count();
        
        debug.put("tracking_service_prescriptions_count", prescCount);
        debug.put("tracking_service_medications_count", medCount);
        
        debug.put("sample_medication_names", medicationRepository.findAll().stream()
                .limit(10)
                .map(m -> m.getMedicationName())
                .collect(Collectors.toList()));
                
        return debug;
    }
}
