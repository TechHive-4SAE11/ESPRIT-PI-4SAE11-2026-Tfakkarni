package org.techhive.medicalservice.service;

import org.techhive.medicalservice.dto.EquipmentDTO;
import org.techhive.medicalservice.entity.Equipment;
import org.techhive.medicalservice.entity.enums.EquipmentStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface IEquipmentService {

    // Basic CRUD
    Equipment createEquipment(EquipmentDTO equipmentDTO);
    Equipment getEquipmentById(long id);
    Equipment updateEquipment(EquipmentDTO equipmentDTO);
    void deleteEquipment(long id);
    List<Equipment> getAllEquipment();

    // Query methods
    List<Equipment> getEquipmentByStatus(EquipmentStatus status);
    List<Equipment> getEquipmentByCategory(String category);
    List<Equipment> getEquipmentByDonorId(Long donorId);
    List<Equipment> getAvailableEquipment();
    List<Equipment> searchEquipmentByName(String name);
    List<Equipment> getEquipmentByCategoryAndStatus(String category, EquipmentStatus status);
    List<Equipment> getEquipmentDonatedAfter(LocalDateTime date);

    // Statistics
    long countEquipmentByStatus(EquipmentStatus status);
    List<Equipment> getEquipmentWithOverdueLoans();

    // Business methods
    Equipment registerDonation(EquipmentDTO equipmentDTO);
    boolean isEquipmentAvailable(Long equipmentId);
    Equipment updateEquipmentStatus(Long equipmentId, EquipmentStatus status);
    List<EquipmentDTO> getEquipmentSuggestions(String keyword);
}
