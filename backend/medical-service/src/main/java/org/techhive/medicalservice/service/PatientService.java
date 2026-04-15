package org.techhive.medicalservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.techhive.medicalservice.client.UserServiceRestClient;
import org.techhive.medicalservice.dto.PatientDTO;
import org.techhive.medicalservice.entity.MedicalFolder;
import org.techhive.medicalservice.repository.MedicalFolderRepository;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientService {

    private final UserServiceRestClient userServiceClient;
    private final MedicalFolderRepository medicalFolderRepository;

    public PatientDTO findByName(String name) {
        log.info("Searching for patient in user-service by name: {}", name);
        
        List<Map<String, Object>> users = userServiceClient.searchUsersByName(name);
        
        if (users == null || users.isEmpty()) {
            return null; // Let the caller handle 404
        }
        
        // Take the first match
        Map<String, Object> user = users.get(0);
        
        Long id = user.get("id") != null ? Long.valueOf(user.get("id").toString()) : null;
        String keycloakId = user.get("keycloakId") != null ? user.get("keycloakId").toString() : null;
        String firstName = user.get("firstName") != null ? user.get("firstName").toString() : "";
        String lastName = user.get("lastName") != null ? user.get("lastName").toString() : "";
        String email = user.get("email") != null ? user.get("email").toString() : "";
        
        String diagnosis = "Non spécifié";
        if (keycloakId != null) {
            List<MedicalFolder> folders = medicalFolderRepository.findByPatientId(keycloakId);
            if (!folders.isEmpty() && folders.get(0).getDiagnostics() != null && !folders.get(0).getDiagnostics().isEmpty()) {
                diagnosis = folders.get(0).getDiagnostics().get(0).getDiseaseName() != null ? 
                    folders.get(0).getDiagnostics().get(0).getDiseaseName() : "Non spécifié";
            } else if (id != null) {
                folders = medicalFolderRepository.findByPatientId(String.valueOf(id));
                if (!folders.isEmpty() && folders.get(0).getDiagnostics() != null && !folders.get(0).getDiagnostics().isEmpty()) {
                    diagnosis = folders.get(0).getDiagnostics().get(0).getDiseaseName() != null ? 
                        folders.get(0).getDiagnostics().get(0).getDiseaseName() : "Non spécifié";
                }
            }
        }
        
        return PatientDTO.builder()
                .id(id)
                .keycloakId(keycloakId)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .age(65) // Default age, as User entity doesn't have birthDate
                .diagnosis(diagnosis)
                .build();
    }
}
