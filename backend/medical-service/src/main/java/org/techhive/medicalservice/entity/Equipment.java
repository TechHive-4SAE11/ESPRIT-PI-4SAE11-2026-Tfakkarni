package org.techhive.medicalservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.techhive.medicalservice.entity.enums.EquipmentStatus;
import org.techhive.medicalservice.entity.enums.EquipmentCategory;
import org.techhive.medicalservice.entity.enums.EquipmentCondition;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "equipments")
public class Equipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    @Enumerated(EnumType.STRING)
    private EquipmentCategory category;

    @Enumerated(EnumType.STRING)
    private EquipmentStatus status;

    private LocalDateTime donationDate;

    @Enumerated(EnumType.STRING)
    private EquipmentCondition condition;

    @Column(name = "donor_id")
    private Long donorId;

    @OneToMany(mappedBy = "equipment", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonIgnoreProperties("equipment")
    private List<EquipmentLoan> loans;

}
