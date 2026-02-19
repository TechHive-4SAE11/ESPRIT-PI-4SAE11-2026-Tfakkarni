package org.techhive.medicalservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.techhive.medicalservice.entity.EquipmentLoan;
import org.techhive.medicalservice.entity.enums.LoanStatus;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentLoanDTO {
    private Long id;

    @NotNull(message = "Equipment ID is required")
    private Long equipmentId;

    private String equipmentName; // For display purposes

    @NotNull(message = "Borrower ID is required")
    private Long borrowerId;

    private LocalDateTime loanDate;

    @Future(message = "Due date must be in the future")
    private LocalDateTime dueDate;

    private LocalDateTime returnDate;
    private String purpose;
    private String notes;
    private LoanStatus status;

    // Convert Entity to DTO
    public static EquipmentLoanDTO fromEntity(EquipmentLoan loan) {
        if (loan == null) return null;

        EquipmentLoanDTOBuilder builder = EquipmentLoanDTO.builder()
                .id(loan.getId())
                .borrowerId(loan.getBorrowerId())
                .loanDate(loan.getLoanDate())
                .dueDate(loan.getDueDate())
                .returnDate(loan.getReturnDate())
                .purpose(loan.getPurpose())
                .notes(loan.getNotes())
                .status(loan.getStatus());

        if (loan.getEquipment() != null) {
            builder.equipmentId(loan.getEquipment().getId())
                    .equipmentName(loan.getEquipment().getName());
        }

        return builder.build();
    }

    // Convert DTO to Entity
    public EquipmentLoan toEntity() {
        EquipmentLoan loan = new EquipmentLoan();
        loan.setId(this.id);
        loan.setBorrowerId(this.borrowerId);
        loan.setLoanDate(this.loanDate);
        loan.setDueDate(this.dueDate);
        loan.setReturnDate(this.returnDate);
        loan.setPurpose(this.purpose);
        loan.setNotes(this.notes);
        loan.setStatus(this.status);
        return loan;
    }
}