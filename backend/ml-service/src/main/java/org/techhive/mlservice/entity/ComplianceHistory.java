package org.techhive.mlservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "compliance_history")
@Data
public class ComplianceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String patientId;
    private double score;
    private LocalDateTime date;
}
