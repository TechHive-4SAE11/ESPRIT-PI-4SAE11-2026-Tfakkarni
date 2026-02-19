package org.techhive.medicalservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.medicalservice.dto.EquipmentLoanDTO;
import org.techhive.medicalservice.entity.EquipmentLoan;
import org.techhive.medicalservice.entity.enums.LoanStatus;
import org.techhive.medicalservice.service.IEquipmentLoanService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/medical/loans")
@RequiredArgsConstructor
public class EquipmentLoanController {

    private final IEquipmentLoanService loanService;

    @PostMapping
    public ResponseEntity<EquipmentLoanDTO> createLoan(@Valid @RequestBody EquipmentLoanDTO loanDTO) {
        log.info("Creating new loan for equipment ID: {}", loanDTO.getEquipmentId());

        EquipmentLoan createdLoan = loanService.createLoan(loanDTO);
        if (createdLoan == null) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(EquipmentLoanDTO.fromEntity(createdLoan), HttpStatus.CREATED);
    }

    @PostMapping("/borrow")
    public ResponseEntity<EquipmentLoanDTO> borrowEquipment(@Valid @RequestBody EquipmentLoanDTO loanDTO) {
        log.info("Borrowing equipment ID: {} for borrower ID: {}", loanDTO.getEquipmentId(), loanDTO.getBorrowerId());

        EquipmentLoan loan = loanService.borrowEquipment(loanDTO);
        if (loan == null) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(EquipmentLoanDTO.fromEntity(loan), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EquipmentLoanDTO> updateLoan(@PathVariable Long id, @Valid @RequestBody EquipmentLoanDTO loanDTO) {
        log.info("Updating loan with ID: {}", id);

        loanDTO.setId(id);
        EquipmentLoan updatedLoan = loanService.updateLoan(loanDTO);
        if (updatedLoan == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(EquipmentLoanDTO.fromEntity(updatedLoan));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLoan(@PathVariable Long id) {
        log.info("Deleting loan with ID: {}", id);

        loanService.deleteLoan(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipmentLoanDTO> getLoanById(@PathVariable Long id) {
        log.info("Fetching loan with ID: {}", id);

        EquipmentLoan loan = loanService.getLoanById(id);
        if (loan == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(EquipmentLoanDTO.fromEntity(loan));
    }

    @GetMapping
    public ResponseEntity<List<EquipmentLoanDTO>> getAllLoans() {
        log.info("Fetching all loans");

        List<EquipmentLoan> loans = loanService.getAllLoans();
        List<EquipmentLoanDTO> loanDTOs = loans.stream()
                .map(EquipmentLoanDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(loanDTOs);
    }

    @GetMapping("/equipment/{equipmentId}")
    public ResponseEntity<List<EquipmentLoanDTO>> getLoansByEquipmentId(@PathVariable Long equipmentId) {
        log.info("Fetching loans for equipment ID: {}", equipmentId);

        List<EquipmentLoan> loans = loanService.getLoansByEquipmentId(equipmentId);
        List<EquipmentLoanDTO> loanDTOs = loans.stream()
                .map(EquipmentLoanDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(loanDTOs);
    }

    @GetMapping("/borrower/{borrowerId}")
    public ResponseEntity<List<EquipmentLoanDTO>> getLoansByBorrowerId(@PathVariable Long borrowerId) {
        log.info("Fetching loans for borrower ID: {}", borrowerId);

        List<EquipmentLoan> loans = loanService.getLoansByBorrowerId(borrowerId);
        List<EquipmentLoanDTO> loanDTOs = loans.stream()
                .map(EquipmentLoanDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(loanDTOs);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<EquipmentLoanDTO>> getLoansByStatus(@PathVariable LoanStatus status) {
        log.info("Fetching loans with status: {}", status);

        List<EquipmentLoan> loans = loanService.getLoansByStatus(status);
        List<EquipmentLoanDTO> loanDTOs = loans.stream()
                .map(EquipmentLoanDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(loanDTOs);
    }

    @GetMapping("/borrower/{borrowerId}/active")
    public ResponseEntity<List<EquipmentLoanDTO>> getActiveLoansByBorrower(@PathVariable Long borrowerId) {
        log.info("Fetching active loans for borrower ID: {}", borrowerId);

        List<EquipmentLoan> loans = loanService.getActiveLoansByBorrower(borrowerId);
        List<EquipmentLoanDTO> loanDTOs = loans.stream()
                .map(EquipmentLoanDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(loanDTOs);
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<EquipmentLoanDTO>> getOverdueLoans() {
        log.info("Fetching overdue loans");

        List<EquipmentLoan> loans = loanService.getOverdueLoans();
        List<EquipmentLoanDTO> loanDTOs = loans.stream()
                .map(EquipmentLoanDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(loanDTOs);
    }

    @GetMapping("/due-between")
    public ResponseEntity<List<EquipmentLoanDTO>> getLoansDueBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("Fetching loans due between {} and {}", startDate, endDate);

        List<EquipmentLoan> loans = loanService.getLoansDueBetween(startDate, endDate);
        List<EquipmentLoanDTO> loanDTOs = loans.stream()
                .map(EquipmentLoanDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(loanDTOs);
    }

    @GetMapping("/equipment/{equipmentId}/current")
    public ResponseEntity<EquipmentLoanDTO> getCurrentLoanForEquipment(@PathVariable Long equipmentId) {
        log.info("Fetching current loan for equipment ID: {}", equipmentId);

        EquipmentLoan loan = loanService.getCurrentLoanForEquipment(equipmentId);
        if (loan == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(EquipmentLoanDTO.fromEntity(loan));
    }

    @GetMapping("/equipment/{equipmentId}/loaned")
    public ResponseEntity<Boolean> isEquipmentLoaned(@PathVariable Long equipmentId) {
        log.info("Checking if equipment {} is loaned", equipmentId);

        boolean isLoaned = loanService.isEquipmentLoaned(equipmentId);
        return ResponseEntity.ok(isLoaned);
    }

    @GetMapping("/borrower/{borrowerId}/active/count")
    public ResponseEntity<Long> countActiveLoansByBorrower(@PathVariable Long borrowerId) {
        log.info("Counting active loans for borrower ID: {}", borrowerId);

        long count = loanService.countActiveLoansByBorrower(borrowerId);
        return ResponseEntity.ok(count);
    }

    @PostMapping("/{id}/return")
    public ResponseEntity<EquipmentLoanDTO> returnEquipment(@PathVariable Long id) {
        log.info("Returning equipment for loan ID: {}", id);

        EquipmentLoan loan = loanService.returnEquipment(id);
        if (loan == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(EquipmentLoanDTO.fromEntity(loan));
    }

    @PostMapping("/{id}/extend")
    public ResponseEntity<EquipmentLoanDTO> extendLoan(
            @PathVariable Long id, @RequestBody Map<String, Integer> request) {
        Integer days = request.get("days");
        log.info("Extending loan ID: {} by {} days", id, days);

        EquipmentLoan loan = loanService.extendLoan(id, days);
        if (loan == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(EquipmentLoanDTO.fromEntity(loan));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<EquipmentLoanDTO> cancelLoan(@PathVariable Long id) {
        log.info("Cancelling loan ID: {}", id);

        EquipmentLoan loan = loanService.cancelLoan(id);
        if (loan == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(EquipmentLoanDTO.fromEntity(loan));
    }

    @GetMapping("/borrower/{borrowerId}/status/{status}")
    public ResponseEntity<List<EquipmentLoanDTO>> getLoansByBorrowerAndStatus(
            @PathVariable Long borrowerId, @PathVariable LoanStatus status) {
        log.info("Fetching loans for borrower {} with status {}", borrowerId, status);

        List<EquipmentLoan> loans = loanService.getLoansByBorrowerAndStatus(borrowerId, status);
        List<EquipmentLoanDTO> loanDTOs = loans.stream()
                .map(EquipmentLoanDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(loanDTOs);
    }

    @PostMapping("/check-overdue")
    public ResponseEntity<String> checkAndUpdateOverdueLoans() {
        log.info("Checking and updating overdue loans");

        loanService.checkAndUpdateOverdueLoans();
        return ResponseEntity.ok("Overdue loans updated");
    }

    @GetMapping("/due-soon")
    public ResponseEntity<List<EquipmentLoanDTO>> getLoansDueSoon(@RequestParam(defaultValue = "3") int days) {
        log.info("Fetching loans due within {} days", days);

        List<EquipmentLoan> loans = loanService.getLoansDueSoon(days);
        List<EquipmentLoanDTO> loanDTOs = loans.stream()
                .map(EquipmentLoanDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(loanDTOs);
    }
}