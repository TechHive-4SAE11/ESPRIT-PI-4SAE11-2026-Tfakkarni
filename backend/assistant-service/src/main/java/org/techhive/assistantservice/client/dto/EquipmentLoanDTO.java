package org.techhive.assistantservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentLoanDTO {
    private Long id;
    private Long equipmentId;
    private String equipmentName;
    private Long borrowerId;
    private LocalDateTime loanDate;
    private LocalDateTime dueDate;
    private LocalDateTime returnDate;
    private String purpose;
    private String notes;
    private String status;  // ACTIVE, RETURNED, OVERDUE, CANCELLED
}
