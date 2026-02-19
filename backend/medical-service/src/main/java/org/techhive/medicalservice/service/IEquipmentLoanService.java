package org.techhive.medicalservice.service;

import org.techhive.medicalservice.dto.EquipmentLoanDTO;
import org.techhive.medicalservice.entity.EquipmentLoan;
import org.techhive.medicalservice.entity.enums.LoanStatus;

import java.time.LocalDateTime;
import java.util.List;


public interface IEquipmentLoanService {

    // Basic CRUD
    EquipmentLoan createLoan(EquipmentLoanDTO loanDTO);
    EquipmentLoan getLoanById(long id);
    EquipmentLoan updateLoan(EquipmentLoanDTO loanDTO);
    void deleteLoan(long id);
    List<EquipmentLoan> getAllLoans();

    // Query methods
    List<EquipmentLoan> getLoansByEquipmentId(Long equipmentId);
    List<EquipmentLoan> getLoansByBorrowerId(Long borrowerId);
    List<EquipmentLoan> getLoansByStatus(LoanStatus status);
    List<EquipmentLoan> getActiveLoansByBorrower(Long borrowerId);
    List<EquipmentLoan> getOverdueLoans();
    List<EquipmentLoan> getLoansDueBetween(LocalDateTime startDate, LocalDateTime endDate);
    EquipmentLoan getCurrentLoanForEquipment(Long equipmentId);

    // Statistics
    long countActiveLoansByBorrower(Long borrowerId);
    boolean isEquipmentLoaned(Long equipmentId);

    // Business methods
    EquipmentLoan borrowEquipment(EquipmentLoanDTO loanDTO);
    EquipmentLoan returnEquipment(Long loanId);
    EquipmentLoan extendLoan(Long loanId, int additionalDays);
    EquipmentLoan cancelLoan(Long loanId);
    List<EquipmentLoan> getLoansByBorrowerAndStatus(Long borrowerId, LoanStatus status);
    void checkAndUpdateOverdueLoans();
    List<EquipmentLoan> getLoansDueSoon(int daysThreshold);
}
