package org.techhive.trackingservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.trackingservice.dto.MedicalFolderRequestDTO;
import org.techhive.trackingservice.dto.MedicalFolderResponseDTO;
import org.techhive.trackingservice.entity.MedicalFolder;
import org.techhive.trackingservice.service.MedicalFolderService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/medical-folders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MedicalFolderController {

    private final MedicalFolderService medicalFolderService;

    @PostMapping
    public ResponseEntity<MedicalFolderResponseDTO> createMedicalFolder(@RequestBody MedicalFolderRequestDTO requestDTO) {
        MedicalFolder medicalFolder = new MedicalFolder();
        medicalFolder.setIdPatient(requestDTO.getIdPatient());
        medicalFolder.setIdDoctor(requestDTO.getIdDoctor());

        MedicalFolder saved = medicalFolderService.createMedicalFolder(medicalFolder);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(saved));
    }

    @GetMapping
    public ResponseEntity<List<MedicalFolderResponseDTO>> getAllMedicalFolders() {
        List<MedicalFolder> folders = medicalFolderService.getAllMedicalFolders();
        List<MedicalFolderResponseDTO> responseDTOs = folders.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicalFolderResponseDTO> getMedicalFolderById(@PathVariable Long id) {
        return medicalFolderService.getMedicalFolderById(id)
                .map(folder -> ResponseEntity.ok(toResponseDTO(folder)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/patient/{idPatient}")
    public ResponseEntity<List<MedicalFolderResponseDTO>> getMedicalFoldersByPatient(@PathVariable String idPatient) {
        List<MedicalFolder> folders = medicalFolderService.getMedicalFoldersByPatient(idPatient);
        List<MedicalFolderResponseDTO> responseDTOs = folders.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/doctor/{idDoctor}")
    public ResponseEntity<List<MedicalFolderResponseDTO>> getMedicalFoldersByDoctor(@PathVariable String idDoctor) {
        List<MedicalFolder> folders = medicalFolderService.getMedicalFoldersByDoctor(idDoctor);
        List<MedicalFolderResponseDTO> responseDTOs = folders.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/patient/{idPatient}/doctor/{idDoctor}")
    public ResponseEntity<List<MedicalFolderResponseDTO>> getMedicalFoldersByPatientAndDoctor(
            @PathVariable String idPatient,
            @PathVariable String idDoctor) {
        List<MedicalFolder> folders = medicalFolderService.getMedicalFoldersByPatientAndDoctor(idPatient, idDoctor);
        List<MedicalFolderResponseDTO> responseDTOs = folders.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicalFolderResponseDTO> updateMedicalFolder(
            @PathVariable Long id,
            @RequestBody MedicalFolderRequestDTO requestDTO) {
        MedicalFolder medicalFolder = new MedicalFolder();
        medicalFolder.setIdPatient(requestDTO.getIdPatient());
        medicalFolder.setIdDoctor(requestDTO.getIdDoctor());

        try {
            MedicalFolder updated = medicalFolderService.updateMedicalFolder(id, medicalFolder);
            return ResponseEntity.ok(toResponseDTO(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedicalFolder(@PathVariable Long id) {
        medicalFolderService.deleteMedicalFolder(id);
        return ResponseEntity.noContent().build();
    }

    private MedicalFolderResponseDTO toResponseDTO(MedicalFolder folder) {
        return new MedicalFolderResponseDTO(
                folder.getId(),
                folder.getIdPatient(),
                folder.getIdDoctor(),
                folder.getCreatedAt(),
                folder.getUpdatedAt()
        );
    }
}
