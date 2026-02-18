package org.techhive.medicalservice.entity;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.techhive.medicalservice.entity.enums.LoanStatus;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "equipment_loans")
public class EquipmentLoan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "equipment_id")
    private Equipment equipment;

    @Column(name = "borrower_id")
    private Long borrowerId;

    private LocalDateTime loanDate;
    private LocalDateTime dueDate;
    private LocalDateTime returnDate;

    private String purpose;
    private String notes;

    @Enumerated(EnumType.STRING)
    private LoanStatus status;


}
