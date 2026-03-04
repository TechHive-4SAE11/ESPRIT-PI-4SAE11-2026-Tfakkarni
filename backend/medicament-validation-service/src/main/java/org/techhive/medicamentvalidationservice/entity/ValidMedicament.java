package org.techhive.medicamentvalidationservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "valid_medicaments", indexes = {
    @Index(name = "idx_drug_name", columnList = "drugName"),
    @Index(name = "idx_brand_name", columnList = "brandName"),
    @Index(name = "idx_generic_name", columnList = "genericName")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidMedicament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "drug_name", nullable = false)
    private String drugName;

    @Column(name = "brand_name")
    private String brandName;

    @Column(name = "generic_name")
    private String genericName;

    @Column(name = "active_ingredients", columnDefinition = "TEXT")
    private String activeIngredients;

    @Column(name = "application_number")
    private String applicationNumber;

    @Column(name = "loaded_at", nullable = false)
    private LocalDateTime loadedAt;

    @PrePersist
    protected void onCreate() {
        loadedAt = LocalDateTime.now();
    }
}
