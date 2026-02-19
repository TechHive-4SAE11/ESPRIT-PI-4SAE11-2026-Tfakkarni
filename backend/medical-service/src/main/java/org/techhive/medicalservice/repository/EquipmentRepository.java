package org.techhive.medicalservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.techhive.medicalservice.entity.Equipment;
import org.techhive.medicalservice.entity.enums.EquipmentStatus;

import java.time.LocalDateTime;
import java.util.List;


public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    // Find by status
    List<Equipment> findByStatus(EquipmentStatus status);

    // Find by category
    List<Equipment> findByCategory(String category);

    // Find by donor
    List<Equipment> findByDonorId(Long donorId);

    // Find available equipment
    @Query("SELECT e FROM Equipment e WHERE e.status = 'AVAILABLE'")
    List<Equipment> findAvailableEquipment();

    // Find by name containing
    List<Equipment> findByNameContainingIgnoreCase(String name);

    // Find by category and status
    List<Equipment> findByCategoryAndStatus(String category, EquipmentStatus status);

    // Find donated after date
    List<Equipment> findByDonationDateAfter(LocalDateTime date);

    // Count by status
    long countByStatus(EquipmentStatus status);

    // Update status
    @Modifying
    @Query("UPDATE Equipment e SET e.status = :status WHERE e.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") EquipmentStatus status);

    // Find overdue equipment
    @Query("SELECT DISTINCT e FROM Equipment e JOIN e.loans l WHERE l.status = 'ACTIVE' AND l.dueDate < CURRENT_TIMESTAMP")
    List<Equipment> findWithOverdueLoans();
}
