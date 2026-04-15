package org.techhive.medicalservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.techhive.medicalservice.repository.DiagnosticsRepository;
import org.techhive.medicalservice.repository.MedicalFolderRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/medical-folders/analytics")
@RequiredArgsConstructor
public class DebugAnalyticsController {

    private final MedicalFolderRepository medicalFolderRepository;
    private final DiagnosticsRepository diagnosticsRepository;

    @GetMapping("/debug-data")
    public Map<String, Object> debugData() {
        Map<String, Object> debug = new HashMap<>();
        
        long folderCount = medicalFolderRepository.count();
        long diagCount = diagnosticsRepository.count();
        
        debug.put("medical_service_folders_count", folderCount);
        debug.put("medical_service_diagnostics_count", diagCount);
        
        debug.put("patient_ids_in_folders", medicalFolderRepository.findAll().stream()
                .map(f -> f.getPatientId())
                .collect(Collectors.toSet()));
                
        debug.put("diagnostics_sample", diagnosticsRepository.findAll().stream()
                .limit(5)
                .map(d -> d.getDiseaseName())
                .collect(Collectors.toList()));

        return debug;
    }
}
