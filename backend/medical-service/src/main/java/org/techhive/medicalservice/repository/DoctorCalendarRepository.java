package org.techhive.medicalservice.repository;

import org.techhive.medicalservice.entity.DoctorCalendar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DoctorCalendarRepository extends JpaRepository<DoctorCalendar, Long> {
    Optional<DoctorCalendar> findByDoctorIdAndActiveTrue(String doctorId);

    Optional<DoctorCalendar> findByDoctorId(String doctorId);
}
