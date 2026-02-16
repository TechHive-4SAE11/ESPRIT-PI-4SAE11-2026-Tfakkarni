package org.techhive.trackingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.techhive.trackingservice.entity.MedicalFolder;

import java.util.List;

@Repository
public interface MedicalFolderRepository extends JpaRepository<MedicalFolder, Long> {
    
    List<MedicalFolder> findByIdPatient(String idPatient);
    
    List<MedicalFolder> findByIdDoctor(String idDoctor);
    
    List<MedicalFolder> findByIdPatientAndIdDoctor(String idPatient, String idDoctor);
}
