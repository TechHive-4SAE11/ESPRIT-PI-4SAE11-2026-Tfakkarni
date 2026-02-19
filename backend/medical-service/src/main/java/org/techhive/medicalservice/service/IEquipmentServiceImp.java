package org.techhive.medicalservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.medicalservice.dto.EquipmentDTO;
import org.techhive.medicalservice.entity.Equipment;
import org.techhive.medicalservice.entity.enums.EquipmentStatus;
import org.techhive.medicalservice.repository.EquipmentRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class IEquipmentServiceImp implements IEquipmentService {

    private final EquipmentRepository equipmentRepository;

    @Override
    public Equipment createEquipment(EquipmentDTO equipmentDTO) {
        Equipment equipment = equipmentDTO.toEntity();

        if (!validateEquipment(equipment)) {
            log.error("Invalid equipment data");
            return null;
        }

        // Set default values
        if (equipment.getStatus() == null) {
            equipment.setStatus(EquipmentStatus.AVAILABLE);
        }
        if (equipment.getDonationDate() == null) {
            equipment.setDonationDate(LocalDateTime.now());
        }

        return equipmentRepository.save(equipment);
    }

    @Override
    public Equipment getEquipmentById(long id) {
        return equipmentRepository.findById(id).orElse(null);
    }

    @Override
    public Equipment updateEquipment(EquipmentDTO equipmentDTO) {
        if (!equipmentRepository.existsById(equipmentDTO.getId())) {
            log.error("Equipment not found with id: {}", equipmentDTO.getId());
            return null;
        }

        Equipment equipment = equipmentDTO.toEntity();

        if (!validateEquipment(equipment)) {
            log.error("Invalid equipment data");
            return null;
        }

        return equipmentRepository.save(equipment);
    }

    @Override
    public void deleteEquipment(long id) {
        if (!equipmentRepository.existsById(id)) {
            log.error("Equipment not found with id: {}", id);
            return;
        }
        equipmentRepository.deleteById(id);
    }

    @Override
    public List<Equipment> getAllEquipment() {
        return StreamSupport.stream(equipmentRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());
    }

    @Override
    public List<Equipment> getEquipmentByStatus(EquipmentStatus status) {
        return equipmentRepository.findByStatus(status);
    }

    @Override
    public List<Equipment> getEquipmentByCategory(String category) {
        return equipmentRepository.findByCategory(category);
    }

    @Override
    public List<Equipment> getEquipmentByDonorId(Long donorId) {
        return equipmentRepository.findByDonorId(donorId);
    }

    @Override
    public List<Equipment> getAvailableEquipment() {
        return equipmentRepository.findAvailableEquipment();
    }

    @Override
    public List<Equipment> searchEquipmentByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return List.of();
        }
        return equipmentRepository.findByNameContainingIgnoreCase(name);
    }

    @Override
    public List<Equipment> getEquipmentByCategoryAndStatus(String category, EquipmentStatus status) {
        return equipmentRepository.findByCategoryAndStatus(category, status);
    }

    @Override
    public List<Equipment> getEquipmentDonatedAfter(LocalDateTime date) {
        return equipmentRepository.findByDonationDateAfter(date);
    }

    @Override
    public long countEquipmentByStatus(EquipmentStatus status) {
        return equipmentRepository.countByStatus(status);
    }

    @Override
    public List<Equipment> getEquipmentWithOverdueLoans() {
        return equipmentRepository.findWithOverdueLoans();
    }

    @Override
    public Equipment registerDonation(EquipmentDTO equipmentDTO) {
        // Set donation specific fields
        equipmentDTO.setDonationDate(LocalDateTime.now());
        equipmentDTO.setStatus(EquipmentStatus.AVAILABLE);

        return createEquipment(equipmentDTO);
    }

    @Override
    public boolean isEquipmentAvailable(Long equipmentId) {
        Equipment equipment = getEquipmentById(equipmentId);
        return equipment != null && equipment.getStatus() == EquipmentStatus.AVAILABLE;
    }

    @Override
    public Equipment updateEquipmentStatus(Long equipmentId, EquipmentStatus status) {
        Equipment equipment = getEquipmentById(equipmentId);
        if (equipment == null) {
            log.error("Equipment not found with id: {}", equipmentId);
            return null;
        }

        equipment.setStatus(status);
        return equipmentRepository.save(equipment);
    }

    @Override
    public List<EquipmentDTO> getEquipmentSuggestions(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }

        List<Equipment> equipment = equipmentRepository.findByNameContainingIgnoreCase(keyword);
        return equipment.stream()
                .map(EquipmentDTO::fromEntity)
                .limit(5) // Limit suggestions
                .toList();
    }

    private boolean validateEquipment(Equipment equipment) {
        if (equipment.getName() == null || equipment.getName().trim().isEmpty()) {
            log.error("Equipment name cannot be empty");
            return false;
        }
        if (equipment.getCategory() == null || equipment.getCategory().trim().isEmpty()) {
            log.error("Equipment category cannot be empty");
            return false;
        }
        if (equipment.getDonorId() == null) {
            log.error("Donor ID is required");
            return false;
        }
        return true;
    }
}