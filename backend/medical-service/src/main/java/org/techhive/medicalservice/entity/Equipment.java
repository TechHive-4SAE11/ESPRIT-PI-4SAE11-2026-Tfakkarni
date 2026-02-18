package org.techhive.medicalservice.entity;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.techhive.medicalservice.entity.enums.EquipmentStatus;

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
    private String category;

    @Enumerated(EnumType.STRING)
    private EquipmentStatus status;

    private LocalDateTime donationDate;
    private String condition;

    @Column(name = "donor_id")
    private Long donorId;

    @OneToMany(mappedBy = "equipment", cascade = CascadeType.ALL)
    private List<EquipmentLoan> loans;


}
