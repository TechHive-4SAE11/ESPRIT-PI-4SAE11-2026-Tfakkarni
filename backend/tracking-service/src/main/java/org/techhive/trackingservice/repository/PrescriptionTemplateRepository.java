package org.techhive.trackingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.techhive.trackingservice.entity.PrescriptionTemplate;

import java.util.List;

@Repository
public interface PrescriptionTemplateRepository extends JpaRepository<PrescriptionTemplate, Long> {

    List<PrescriptionTemplate> findByDoctorIdOrderByCreatedAtDesc(String doctorId);

    List<PrescriptionTemplate> findByDoctorIdAndNameContainingIgnoreCase(String doctorId, String name);
}
