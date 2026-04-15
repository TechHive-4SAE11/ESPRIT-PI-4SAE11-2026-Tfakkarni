package org.techhive.medicalservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.medicalservice.dto.EquipmentDTO;
import org.techhive.medicalservice.entity.Equipment;
import org.techhive.medicalservice.entity.enums.EquipmentCategory;
import org.techhive.medicalservice.entity.enums.EquipmentStatus;
import org.techhive.medicalservice.repository.EquipmentRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EquipmentServiceTest {

    @Mock
    private EquipmentRepository equipmentRepository;

    @InjectMocks
    private IEquipmentServiceImp equipmentService;

    private Equipment sampleEquipment;
    private EquipmentDTO sampleDTO;

    @BeforeEach
    void setUp() {
        sampleEquipment = new Equipment();
        sampleEquipment.setId(1L);
        sampleEquipment.setName("Wheelchair");
        sampleEquipment.setDescription("Standard wheelchair");
        sampleEquipment.setCategory(EquipmentCategory.MOBILITY);
        sampleEquipment.setStatus(EquipmentStatus.AVAILABLE);
        sampleEquipment.setDonorId(100L);
        sampleEquipment.setDonationDate(LocalDateTime.now());

        sampleDTO = EquipmentDTO.builder()
                .id(1L)
                .name("Wheelchair")
                .description("Standard wheelchair")
                .category(EquipmentCategory.MOBILITY)
                .status(EquipmentStatus.AVAILABLE)
                .donorId(100L)
                .build();
    }

    @Test
    void createEquipment_withValidData_shouldReturnSaved() {
        when(equipmentRepository.save(any(Equipment.class))).thenReturn(sampleEquipment);

        Equipment result = equipmentService.createEquipment(sampleDTO);

        assertNotNull(result);
        assertEquals("Wheelchair", result.getName());
        verify(equipmentRepository).save(any(Equipment.class));
    }

    @Test
    void createEquipment_withNullName_shouldReturnNull() {
        EquipmentDTO invalid = EquipmentDTO.builder()
                .name(null)
                .category(EquipmentCategory.MOBILITY)
                .donorId(100L)
                .build();

        Equipment result = equipmentService.createEquipment(invalid);

        assertNull(result);
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    void createEquipment_withNullCategory_shouldReturnNull() {
        EquipmentDTO invalid = EquipmentDTO.builder()
                .name("Wheelchair")
                .category(null)
                .donorId(100L)
                .build();

        Equipment result = equipmentService.createEquipment(invalid);

        assertNull(result);
    }

    @Test
    void createEquipment_withNullDonorId_shouldReturnNull() {
        EquipmentDTO invalid = EquipmentDTO.builder()
                .name("Wheelchair")
                .category(EquipmentCategory.MOBILITY)
                .donorId(null)
                .build();

        Equipment result = equipmentService.createEquipment(invalid);

        assertNull(result);
    }

    @Test
    void createEquipment_setsDefaultStatusIfNull() {
        EquipmentDTO dto = EquipmentDTO.builder()
                .name("Walker")
                .category(EquipmentCategory.MOBILITY)
                .donorId(100L)
                .status(null)
                .build();

        when(equipmentRepository.save(any(Equipment.class))).thenAnswer(inv -> inv.getArgument(0));

        Equipment result = equipmentService.createEquipment(dto);

        assertNotNull(result);
        assertEquals(EquipmentStatus.AVAILABLE, result.getStatus());
    }

    @Test
    void getEquipmentById_whenExists_shouldReturnEquipment() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(sampleEquipment));

        Equipment result = equipmentService.getEquipmentById(1L);

        assertNotNull(result);
        assertEquals("Wheelchair", result.getName());
    }

    @Test
    void getEquipmentById_whenNotExists_shouldReturnNull() {
        when(equipmentRepository.findById(99L)).thenReturn(Optional.empty());

        Equipment result = equipmentService.getEquipmentById(99L);

        assertNull(result);
    }

    @Test
    void updateEquipment_whenNotExists_shouldReturnNull() {
        when(equipmentRepository.existsById(1L)).thenReturn(false);

        Equipment result = equipmentService.updateEquipment(sampleDTO);

        assertNull(result);
    }

    @Test
    void deleteEquipment_whenExists_shouldDelete() {
        when(equipmentRepository.existsById(1L)).thenReturn(true);

        equipmentService.deleteEquipment(1L);

        verify(equipmentRepository).deleteById(1L);
    }

    @Test
    void getEquipmentByStatus_shouldReturnFilteredList() {
        when(equipmentRepository.findByStatus(EquipmentStatus.AVAILABLE))
                .thenReturn(List.of(sampleEquipment));

        List<Equipment> result = equipmentService.getEquipmentByStatus(EquipmentStatus.AVAILABLE);

        assertEquals(1, result.size());
    }

    @Test
    void searchEquipmentByName_withNullName_shouldReturnEmpty() {
        List<Equipment> result = equipmentService.searchEquipmentByName(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void searchEquipmentByName_withValidName_shouldReturnResults() {
        when(equipmentRepository.findByNameContainingIgnoreCase("Wheel"))
                .thenReturn(List.of(sampleEquipment));

        List<Equipment> result = equipmentService.searchEquipmentByName("Wheel");

        assertEquals(1, result.size());
    }

    @Test
    void isEquipmentAvailable_whenAvailable_shouldReturnTrue() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(sampleEquipment));

        boolean available = equipmentService.isEquipmentAvailable(1L);

        assertTrue(available);
    }

    @Test
    void isEquipmentAvailable_whenLoaned_shouldReturnFalse() {
        sampleEquipment.setStatus(EquipmentStatus.LOANED);
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(sampleEquipment));

        boolean available = equipmentService.isEquipmentAvailable(1L);

        assertFalse(available);
    }

    @Test
    void updateEquipmentStatus_shouldUpdateAndSave() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(sampleEquipment));
        when(equipmentRepository.save(any(Equipment.class))).thenReturn(sampleEquipment);

        Equipment result = equipmentService.updateEquipmentStatus(1L, EquipmentStatus.LOANED);

        assertNotNull(result);
        assertEquals(EquipmentStatus.LOANED, result.getStatus());
    }

    @Test
    void countEquipmentByStatus_shouldReturnCount() {
        when(equipmentRepository.countByStatus(EquipmentStatus.AVAILABLE)).thenReturn(5L);

        long count = equipmentService.countEquipmentByStatus(EquipmentStatus.AVAILABLE);

        assertEquals(5L, count);
    }

    @Test
    void getEquipmentSuggestions_withEmptyKeyword_shouldReturnEmpty() {
        List<EquipmentDTO> result = equipmentService.getEquipmentSuggestions("");

        assertTrue(result.isEmpty());
    }
}
