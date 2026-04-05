package org.techhive.medicalservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.techhive.medicalservice.entity.EquipmentLoan;
import org.techhive.medicalservice.entity.enums.LoanStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface EquipmentLoanRepository extends JpaRepository<EquipmentLoan, Long> {

    // Find by equipment
    List<EquipmentLoan> findByEquipmentId(Long equipmentId);

    // Find by borrower
    List<EquipmentLoan> findByBorrowerId(Long borrowerId);

    // Find by status
    List<EquipmentLoan> findByStatus(LoanStatus status);

    // Find active loans for a borrower
    List<EquipmentLoan> findByBorrowerIdAndStatus(Long borrowerId, LoanStatus status);

    // Find overdue loans
    @Query("SELECT l FROM EquipmentLoan l WHERE l.status = 'ACTIVE' AND l.dueDate < CURRENT_TIMESTAMP")
    List<EquipmentLoan> findOverdueLoans();

    // Find loans due between dates
    List<EquipmentLoan> findByDueDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    // Find current loan for equipment
    @Query("SELECT l FROM EquipmentLoan l WHERE l.equipment.id = :equipmentId AND l.status = 'ACTIVE'")
    EquipmentLoan findCurrentLoanByEquipmentId(@Param("equipmentId") Long equipmentId);

    // Check if equipment is currently loaned
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM EquipmentLoan l WHERE l.equipment.id = :equipmentId AND l.status = 'ACTIVE'")
    boolean isEquipmentLoaned(@Param("equipmentId") Long equipmentId);

    // Count active loans for borrower
    long countByBorrowerIdAndStatus(Long borrowerId, LoanStatus status);

    // Update loan status
    @Modifying
    @Query("UPDATE EquipmentLoan l SET l.status = :status WHERE l.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") LoanStatus status);

    // Return equipment (set return date and status)
    @Modifying
    @Query("UPDATE EquipmentLoan l SET l.status = 'RETURNED', l.returnDate = :returnDate WHERE l.id = :id")
    void returnEquipment(@Param("id") Long id, @Param("returnDate") LocalDateTime returnDate);
}
