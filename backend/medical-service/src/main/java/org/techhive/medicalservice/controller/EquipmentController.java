package org.techhive.medicalservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.medicalservice.dto.EquipmentDTO;
import org.techhive.medicalservice.entity.Equipment;
import org.techhive.medicalservice.entity.enums.EquipmentStatus;
import org.techhive.medicalservice.entity.enums.EquipmentCategory;
import org.techhive.medicalservice.service.IEquipmentService;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RestController
@RequestMapping("/api/medical/equipment")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EquipmentController {

    private final IEquipmentService equipmentService;

    @PostMapping
    @Transactional
    public ResponseEntity<EquipmentDTO> createEquipment(@Valid @RequestBody EquipmentDTO equipmentDTO) {
        log.info("Creating new equipment: {}", equipmentDTO.getName());

        Equipment createdEquipment = equipmentService.createEquipment(equipmentDTO);
        if (createdEquipment == null) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(EquipmentDTO.fromEntity(createdEquipment), HttpStatus.CREATED);
    }

    @PostMapping("/donate")
    @Transactional
    public ResponseEntity<EquipmentDTO> registerDonation(@Valid @RequestBody EquipmentDTO equipmentDTO) {
        log.info("Registering donation: {}", equipmentDTO.getName());

        Equipment donatedEquipment = equipmentService.registerDonation(equipmentDTO);
        if (donatedEquipment == null) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(EquipmentDTO.fromEntity(donatedEquipment), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<EquipmentDTO> updateEquipment(@PathVariable("id") Long id,
            @Valid @RequestBody EquipmentDTO equipmentDTO) {
        log.info("Updating equipment with ID: {}", id);

        equipmentDTO.setId(id);
        Equipment updatedEquipment = equipmentService.updateEquipment(equipmentDTO);
        if (updatedEquipment == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(EquipmentDTO.fromEntity(updatedEquipment));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteEquipment(@PathVariable("id") Long id) {
        log.info("Deleting equipment with ID: {}", id);

        equipmentService.deleteEquipment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipmentDTO> getEquipmentById(@PathVariable("id") Long id) {
        log.info("Fetching equipment with ID: {}", id);

        Equipment equipment = equipmentService.getEquipmentById(id);
        if (equipment == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(EquipmentDTO.fromEntity(equipment));
    }

    @GetMapping
    public ResponseEntity<List<EquipmentDTO>> getAllEquipment() {
        log.info("Fetching all equipment");

        List<Equipment> equipmentList = equipmentService.getAllEquipment();
        List<EquipmentDTO> equipmentDTOs = equipmentList.stream()
                .map(EquipmentDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(equipmentDTOs);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<EquipmentDTO>> getEquipmentByStatus(@PathVariable("status") EquipmentStatus status) {
        log.info("Fetching equipment with status: {}", status);

        List<Equipment> equipmentList = equipmentService.getEquipmentByStatus(status);
        List<EquipmentDTO> equipmentDTOs = equipmentList.stream()
                .map(EquipmentDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(equipmentDTOs);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<EquipmentDTO>> getEquipmentByCategory(@PathVariable("category") EquipmentCategory category) {
        List<Equipment> equipment = equipmentService.getEquipmentByCategory(category);
        return ResponseEntity.ok(equipment.stream()
                .map(EquipmentDTO::fromEntity)
                .toList());
    }

    @GetMapping("/donor/{donorId}")
    public ResponseEntity<List<EquipmentDTO>> getEquipmentByDonorId(@PathVariable("donorId") Long donorId) {
        log.info("Fetching equipment donated by user: {}", donorId);

        List<Equipment> equipmentList = equipmentService.getEquipmentByDonorId(donorId);
        List<EquipmentDTO> equipmentDTOs = equipmentList.stream()
                .map(EquipmentDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(equipmentDTOs);
    }

    @GetMapping("/available")
    public ResponseEntity<List<EquipmentDTO>> getAvailableEquipment() {
        log.info("Fetching available equipment");

        List<Equipment> equipmentList = equipmentService.getAvailableEquipment();
        List<EquipmentDTO> equipmentDTOs = equipmentList.stream()
                .map(EquipmentDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(equipmentDTOs);
    }

    @GetMapping("/search")
    public ResponseEntity<List<EquipmentDTO>> searchEquipment(@RequestParam String name) {
        log.info("Searching equipment with name: {}", name);

        List<Equipment> equipmentList = equipmentService.searchEquipmentByName(name);
        List<EquipmentDTO> equipmentDTOs = equipmentList.stream()
                .map(EquipmentDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(equipmentDTOs);
    }

    @GetMapping("/filter/{category}/{status}")
    public ResponseEntity<List<EquipmentDTO>> getEquipmentByCategoryAndStatus(
            @PathVariable("category") EquipmentCategory category, @PathVariable("status") EquipmentStatus status) {
        List<Equipment> equipment = equipmentService.getEquipmentByCategoryAndStatus(category, status);
        return ResponseEntity.ok(equipment.stream()
                .map(EquipmentDTO::fromEntity)
                .toList());
    }

    @GetMapping("/donated-after")
    public ResponseEntity<List<EquipmentDTO>> getEquipmentDonatedAfter(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
        log.info("Fetching equipment donated after: {}", date);

        List<Equipment> equipmentList = equipmentService.getEquipmentDonatedAfter(date);
        List<EquipmentDTO> equipmentDTOs = equipmentList.stream()
                .map(EquipmentDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(equipmentDTOs);
    }

    @GetMapping("/status/{status}/count")
    public ResponseEntity<Long> countEquipmentByStatus(@PathVariable("status") EquipmentStatus status) {
        log.info("Counting equipment with status: {}", status);

        long count = equipmentService.countEquipmentByStatus(status);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<EquipmentDTO>> getEquipmentWithOverdueLoans() {
        log.info("Fetching equipment with overdue loans");

        List<Equipment> equipmentList = equipmentService.getEquipmentWithOverdueLoans();
        List<EquipmentDTO> equipmentDTOs = equipmentList.stream()
                .map(EquipmentDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(equipmentDTOs);
    }

    @GetMapping("/{id}/available")
    public ResponseEntity<Boolean> isEquipmentAvailable(@PathVariable("id") Long id) {
        log.info("Checking if equipment {} is available", id);

        boolean available = equipmentService.isEquipmentAvailable(id);
        return ResponseEntity.ok(available);
    }

    @PatchMapping("/{id}/status")
    @Transactional
    public ResponseEntity<EquipmentDTO> updateEquipmentStatus(
            @PathVariable("id") Long id, @RequestParam EquipmentStatus status) {
        log.info("Updating equipment {} status to {}", id, status);

        Equipment equipment = equipmentService.updateEquipmentStatus(id, status);
        if (equipment == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(EquipmentDTO.fromEntity(equipment));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<EquipmentDTO>> getEquipmentSuggestions(@RequestParam String keyword) {
        log.info("Getting equipment suggestions for: {}", keyword);

        List<EquipmentDTO> suggestions = equipmentService.getEquipmentSuggestions(keyword);
        return ResponseEntity.ok(suggestions);
    }
}
