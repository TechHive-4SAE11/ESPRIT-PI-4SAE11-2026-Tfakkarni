package org.techhive.assistantservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentDTO {
    private Long id;
    private String name;
    private String description;
    private String category;    // MOBILITY, RESPIRATORY, etc.
    private String status;      // AVAILABLE, LOANED, etc.
    private LocalDateTime donationDate;
    private String condition;   // GOOD, FAIR, NEW, etc.
    private Long donorId;
    private List<EquipmentLoanDTO> loans;
}
