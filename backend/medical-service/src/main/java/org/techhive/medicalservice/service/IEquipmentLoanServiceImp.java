package org.techhive.medicalservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.medicalservice.dto.EquipmentLoanDTO;
import org.techhive.medicalservice.entity.Equipment;
import org.techhive.medicalservice.entity.EquipmentLoan;
import org.techhive.medicalservice.entity.enums.EquipmentStatus;
import org.techhive.medicalservice.entity.enums.LoanStatus;
import org.techhive.medicalservice.repository.EquipmentLoanRepository;
import org.techhive.medicalservice.repository.EquipmentRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class IEquipmentLoanServiceImp implements IEquipmentLoanService {

    private final EquipmentLoanRepository loanRepository;
    private final EquipmentRepository equipmentRepository;
    private final IEquipmentService equipmentService;

    @Override
    public EquipmentLoan createLoan(EquipmentLoanDTO loanDTO) {
        EquipmentLoan loan = loanDTO.toEntity();

        Equipment equipment = equipmentRepository.findById(loanDTO.getEquipmentId())
                .orElse(null);

        if (equipment == null) {
            log.error("Equipment not found with id: {}", loanDTO.getEquipmentId());
            return null;
        }

        loan.setEquipment(equipment);

        // Set default values
        if (loan.getLoanDate() == null) {
            loan.setLoanDate(LocalDateTime.now());
        }
        if (loan.getStatus() == null) {
            loan.setStatus(LoanStatus.ACTIVE);
        }

        if (!validateLoan(loan)) {
            log.error("Invalid loan data");
            return null;
        }

        // Update equipment status
        equipment.setStatus(EquipmentStatus.LOANED);
        equipmentRepository.save(equipment);

        return loanRepository.save(loan);
    }

    @Override
    public EquipmentLoan getLoanById(long id) {
        return loanRepository.findById(id).orElse(null);
    }

    @Override
    public EquipmentLoan updateLoan(EquipmentLoanDTO loanDTO) {
        if (!loanRepository.existsById(loanDTO.getId())) {
            log.error("Loan not found with id: {}", loanDTO.getId());
            return null;
        }

        EquipmentLoan loan = loanDTO.toEntity();

        // Set the equipment
        Equipment equipment = equipmentRepository.findById(loanDTO.getEquipmentId())
                .orElse(null);

        if (equipment == null) {
            log.error("Equipment not found with id: {}", loanDTO.getEquipmentId());
            return null;
        }

        loan.setEquipment(equipment);

        if (!validateLoan(loan)) {
            log.error("Invalid loan data");
            return null;
        }

        return loanRepository.save(loan);
    }

    @Override
    public void deleteLoan(long id) {
        if (!loanRepository.existsById(id)) {
            log.error("Loan not found with id: {}", id);
            return;
        }

        EquipmentLoan loan = getLoanById(id);
        if (loan != null && loan.getEquipment() != null) {
            // Update equipment status back to available
            loan.getEquipment().setStatus(EquipmentStatus.AVAILABLE);
            equipmentRepository.save(loan.getEquipment());
        }

        loanRepository.deleteById(id);
    }

    @Override
    public List<EquipmentLoan> getAllLoans() {
        return StreamSupport.stream(loanRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());
    }

    @Override
    public List<EquipmentLoan> getLoansByEquipmentId(Long equipmentId) {
        return loanRepository.findByEquipmentId(equipmentId);
    }

    @Override
    public List<EquipmentLoan> getLoansByBorrowerId(Long borrowerId) {
        return loanRepository.findByBorrowerId(borrowerId);
    }

    @Override
    public List<EquipmentLoan> getLoansByStatus(LoanStatus status) {
        return loanRepository.findByStatus(status);
    }

    @Override
    public List<EquipmentLoan> getActiveLoansByBorrower(Long borrowerId) {
        return loanRepository.findByBorrowerIdAndStatus(borrowerId, LoanStatus.ACTIVE);
    }

    @Override
    public List<EquipmentLoan> getOverdueLoans() {
        return loanRepository.findOverdueLoans();
    }

    @Override
    public List<EquipmentLoan> getLoansDueBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return loanRepository.findByDueDateBetween(startDate, endDate);
    }

    @Override
    public EquipmentLoan getCurrentLoanForEquipment(Long equipmentId) {
        return loanRepository.findCurrentLoanByEquipmentId(equipmentId);
    }

    @Override
    public long countActiveLoansByBorrower(Long borrowerId) {
        return loanRepository.countByBorrowerIdAndStatus(borrowerId, LoanStatus.ACTIVE);
    }

    @Override
    public boolean isEquipmentLoaned(Long equipmentId) {
        return loanRepository.isEquipmentLoaned(equipmentId);
    }

    @Override
    public EquipmentLoan borrowEquipment(EquipmentLoanDTO loanDTO) {
        // Check if equipment is available
        if (!equipmentService.isEquipmentAvailable(loanDTO.getEquipmentId())) {
            log.error("Equipment not available for borrowing: {}", loanDTO.getEquipmentId());
            return null;
        }

        // Set default dates if not provided
        if (loanDTO.getLoanDate() == null) {
            loanDTO.setLoanDate(LocalDateTime.now());
        }
        if (loanDTO.getDueDate() == null) {
            loanDTO.setDueDate(LocalDateTime.now().plusDays(14)); // Default 14 days
        }

        loanDTO.setStatus(LoanStatus.ACTIVE);

        return createLoan(loanDTO);
    }

    @Override
    public EquipmentLoan returnEquipment(Long loanId) {
        EquipmentLoan loan = getLoanById(loanId);
        if (loan == null) {
            log.error("Loan not found with id: {}", loanId);
            return null;
        }

        if (loan.getStatus() != LoanStatus.ACTIVE) {
            log.error("Loan is not active: {}", loanId);
            return null;
        }

        // Update loan
        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnDate(LocalDateTime.now());

        // Update equipment status
        if (loan.getEquipment() != null) {
            loan.getEquipment().setStatus(EquipmentStatus.AVAILABLE);
            equipmentRepository.save(loan.getEquipment());
        }

        return loanRepository.save(loan);
    }

    @Override
    public EquipmentLoan extendLoan(Long loanId, int additionalDays) {
        EquipmentLoan loan = getLoanById(loanId);
        if (loan == null) {
            log.error("Loan not found with id: {}", loanId);
            return null;
        }

        if (loan.getStatus() != LoanStatus.ACTIVE) {
            log.error("Cannot extend inactive loan: {}", loanId);
            return null;
        }

        loan.setDueDate(loan.getDueDate().plusDays(additionalDays));

        return loanRepository.save(loan);
    }

    @Override
    public EquipmentLoan cancelLoan(Long loanId) {
        EquipmentLoan loan = getLoanById(loanId);
        if (loan == null) {
            log.error("Loan not found with id: {}", loanId);
            return null;
        }

        loan.setStatus(LoanStatus.CANCELLED);

        // Update equipment status
        if (loan.getEquipment() != null) {
            loan.getEquipment().setStatus(EquipmentStatus.AVAILABLE);
            equipmentRepository.save(loan.getEquipment());
        }

        return loanRepository.save(loan);
    }

    @Override
    public List<EquipmentLoan> getLoansByBorrowerAndStatus(Long borrowerId, LoanStatus status) {
        return loanRepository.findByBorrowerIdAndStatus(borrowerId, status);
    }

    @Override
    @Transactional
    public void checkAndUpdateOverdueLoans() {
        List<EquipmentLoan> overdueLoans = loanRepository.findOverdueLoans();

        for (EquipmentLoan loan : overdueLoans) {
            loan.setStatus(LoanStatus.OVERDUE);
            loanRepository.save(loan);
            log.info("Marked loan {} as overdue", loan.getId());
        }
    }

    @Override
    public List<EquipmentLoan> getLoansDueSoon(int daysThreshold) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusDays(daysThreshold);

        return loanRepository.findByDueDateBetween(now, threshold)
                .stream()
                .filter(loan -> loan.getStatus() == LoanStatus.ACTIVE)
                .toList();
    }

    private boolean validateLoan(EquipmentLoan loan) {
        if (loan.getBorrowerId() == null) {
            log.error("Borrower ID is required");
            return false;
        }
        if (loan.getEquipment() == null) {
            log.error("Equipment is required");
            return false;
        }
        if (loan.getDueDate() != null && loan.getLoanDate() != null &&
                loan.getDueDate().isBefore(loan.getLoanDate())) {
            log.error("Due date cannot be before loan date");
            return false;
        }
        return true;
    }
}
