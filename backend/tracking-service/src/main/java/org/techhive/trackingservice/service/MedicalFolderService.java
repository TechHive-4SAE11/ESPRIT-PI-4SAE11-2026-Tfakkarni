package org.techhive.trackingservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.trackingservice.entity.MedicalFolder;
import org.techhive.trackingservice.repository.MedicalFolderRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class MedicalFolderService {

    private final MedicalFolderRepository medicalFolderRepository;

    public MedicalFolder createMedicalFolder(MedicalFolder medicalFolder) {
        return medicalFolderRepository.save(medicalFolder);
    }

    @Transactional(readOnly = true)
    public List<MedicalFolder> getAllMedicalFolders() {
        return medicalFolderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<MedicalFolder> getMedicalFolderById(Long id) {
        return medicalFolderRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<MedicalFolder> getMedicalFoldersByPatient(String idPatient) {
        return medicalFolderRepository.findByIdPatient(idPatient);
    }

    @Transactional(readOnly = true)
    public List<MedicalFolder> getMedicalFoldersByDoctor(String idDoctor) {
        return medicalFolderRepository.findByIdDoctor(idDoctor);
    }

    @Transactional(readOnly = true)
    public List<MedicalFolder> getMedicalFoldersByPatientAndDoctor(String idPatient, String idDoctor) {
        return medicalFolderRepository.findByIdPatientAndIdDoctor(idPatient, idDoctor);
    }

    public MedicalFolder updateMedicalFolder(Long id, MedicalFolder medicalFolder) {
        return medicalFolderRepository.findById(id)
                .map(existing -> {
                    existing.setIdPatient(medicalFolder.getIdPatient());
                    existing.setIdDoctor(medicalFolder.getIdDoctor());
                    return medicalFolderRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("MedicalFolder not found with id: " + id));
    }

    public void deleteMedicalFolder(Long id) {
        medicalFolderRepository.deleteById(id);
    }
}
