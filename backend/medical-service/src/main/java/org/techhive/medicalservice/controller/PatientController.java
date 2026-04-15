package org.techhive.medicalservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.techhive.medicalservice.dto.PatientDTO;
import org.techhive.medicalservice.service.PatientService;

@Slf4j
@RestController
@RequestMapping("/api/medical/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @GetMapping("/search")
    public ResponseEntity<PatientDTO> searchPatientByName(@RequestParam("name") String name) {
        log.info("Received request to search patient by name: {}", name);
        
        PatientDTO patient = patientService.findByName(name);
        
        if (patient == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(patient);
    }
}
