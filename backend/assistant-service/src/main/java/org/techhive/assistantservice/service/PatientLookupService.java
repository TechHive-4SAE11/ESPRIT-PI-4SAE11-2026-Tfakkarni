package org.techhive.assistantservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.techhive.assistantservice.client.MedicalServiceClient;
import org.techhive.assistantservice.dto.PatientDTO;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientLookupService {

    private final MedicalServiceClient medicalServiceClient;

    public PatientDTO findPatientByName(String name) {
        log.info("Searching for patient by name: {}", name);
        
        PatientDTO patient = medicalServiceClient.findPatientByName(name);
        
        if (patient == null) {
            throw new RuntimeException("Patient not found with name: " + name);
        }
        
        log.info("Patient found: {} {} (ID: {})", 
                 patient.getFirstName(), patient.getLastName(), patient.getId());
        return patient;
    }
}
