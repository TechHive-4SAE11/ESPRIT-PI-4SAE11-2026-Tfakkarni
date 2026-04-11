package org.techhive.medicalservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.techhive.medicalservice.entity.MedicalFolder;

import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface MedicalFolderRepository extends JpaRepository<MedicalFolder, Long> {

    List<MedicalFolder> findByDoctorId(String doctorId);

    List<MedicalFolder> findByPatientId(String patientId);

    List<MedicalFolder> findByPatientIdAndDoctorId(String patientId, String doctorId);

    Page<MedicalFolder> findByDoctorId(String doctorId, Pageable pageable);

    Page<MedicalFolder> findByPatientIdContainingIgnoreCase(String patientId, Pageable pageable);

    long countByCreatedAtAfter(LocalDateTime date);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByUpdatedAtAfter(LocalDateTime date);

    @Query("SELECT COUNT(DISTINCT m.patientId) FROM MedicalFolder m")
    long countDistinctPatientIds();
}
