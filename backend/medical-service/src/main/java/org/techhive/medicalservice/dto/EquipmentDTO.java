package org.techhive.medicalservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.techhive.medicalservice.entity.Equipment;
import org.techhive.medicalservice.entity.enums.EquipmentStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentDTO {
    private Long id;

    @NotBlank(message = "Equipment name is required")
    private String name;

    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    private EquipmentStatus status;
    private LocalDateTime donationDate;
    private String condition;

    @NotNull(message = "Donor ID is required")
    private Long donorId;

    private List<EquipmentLoanDTO> loans;

    // Convert Entity to DTO
    public static EquipmentDTO fromEntity(Equipment equipment) {
        if (equipment == null) return null;

        EquipmentDTOBuilder builder = EquipmentDTO.builder()
                .id(equipment.getId())
                .name(equipment.getName())
                .description(equipment.getDescription())
                .category(equipment.getCategory())
                .status(equipment.getStatus())
                .donationDate(equipment.getDonationDate())
                .condition(equipment.getCondition())
                .donorId(equipment.getDonorId());

        if (equipment.getLoans() != null && !equipment.getLoans().isEmpty()) {
            builder.loans(equipment.getLoans().stream()
                    .map(EquipmentLoanDTO::fromEntity)
                    .toList());
        }

        return builder.build();
    }

    // Convert DTO to Entity
    public Equipment toEntity() {
        Equipment equipment = new Equipment();
        equipment.setId(this.id);
        equipment.setName(this.name);
        equipment.setDescription(this.description);
        equipment.setCategory(this.category);
        equipment.setStatus(this.status);
        equipment.setDonationDate(this.donationDate);
        equipment.setCondition(this.condition);
        equipment.setDonorId(this.donorId);
        return equipment;
    }
}