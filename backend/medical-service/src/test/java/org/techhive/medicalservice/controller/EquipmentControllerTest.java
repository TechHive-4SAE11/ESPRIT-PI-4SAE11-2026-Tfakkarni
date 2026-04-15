package org.techhive.medicalservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.techhive.medicalservice.entity.Equipment;
import org.techhive.medicalservice.entity.enums.EquipmentCategory;
import org.techhive.medicalservice.entity.enums.EquipmentCondition;
import org.techhive.medicalservice.entity.enums.EquipmentStatus;
import org.techhive.medicalservice.service.IEquipmentService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EquipmentController.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class EquipmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IEquipmentService equipmentService;

    @Autowired
    private ObjectMapper objectMapper;

    private Equipment sampleEquipment;

    @BeforeEach
    void setUp() {
        sampleEquipment = new Equipment();
        sampleEquipment.setId(1L);
        sampleEquipment.setName("Wheelchair");
        sampleEquipment.setDescription("Standard wheelchair");
        sampleEquipment.setCategory(EquipmentCategory.MOBILITY);
        sampleEquipment.setStatus(EquipmentStatus.AVAILABLE);
        sampleEquipment.setCondition(EquipmentCondition.NEW);
        sampleEquipment.setDonorId(100L);
        sampleEquipment.setDonationDate(LocalDateTime.now());
    }

    @Test
    void createEquipment_shouldReturn201() throws Exception {
        when(equipmentService.createEquipment(any())).thenReturn(sampleEquipment);

        String json = """
                {
                  "name": "Wheelchair",
                  "description": "Standard wheelchair",
                  "category": "MOBILITY",
                  "status": "AVAILABLE",
                  "condition": "NEW",
                  "donorId": 100
                }
                """;

        mockMvc.perform(post("/api/medical/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Wheelchair"));
    }

    @Test
    void getEquipmentById_whenExists_shouldReturn200() throws Exception {
        when(equipmentService.getEquipmentById(1L)).thenReturn(sampleEquipment);

        mockMvc.perform(get("/api/medical/equipment/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Wheelchair"));
    }

    @Test
    void getEquipmentById_whenNotExists_shouldReturn404() throws Exception {
        when(equipmentService.getEquipmentById(99L)).thenReturn(null);

        mockMvc.perform(get("/api/medical/equipment/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllEquipment_shouldReturn200() throws Exception {
        when(equipmentService.getAllEquipment()).thenReturn(List.of(sampleEquipment));

        mockMvc.perform(get("/api/medical/equipment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Wheelchair"));
    }

    @Test
    void deleteEquipment_shouldReturn204() throws Exception {
        doNothing().when(equipmentService).deleteEquipment(1L);

        mockMvc.perform(delete("/api/medical/equipment/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getEquipmentByStatus_shouldReturn200() throws Exception {
        when(equipmentService.getEquipmentByStatus(EquipmentStatus.AVAILABLE))
                .thenReturn(List.of(sampleEquipment));

        mockMvc.perform(get("/api/medical/equipment/status/AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test
    void getAvailableEquipment_shouldReturn200() throws Exception {
        when(equipmentService.getAvailableEquipment()).thenReturn(List.of(sampleEquipment));

        mockMvc.perform(get("/api/medical/equipment/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test
    void searchEquipment_shouldReturn200() throws Exception {
        when(equipmentService.searchEquipmentByName("Wheel"))
                .thenReturn(List.of(sampleEquipment));

        mockMvc.perform(get("/api/medical/equipment/search").param("name", "Wheel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Wheelchair"));
    }

    @Test
    void isEquipmentAvailable_shouldReturn200() throws Exception {
        when(equipmentService.isEquipmentAvailable(1L)).thenReturn(true);

        mockMvc.perform(get("/api/medical/equipment/1/available"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void countEquipmentByStatus_shouldReturn200() throws Exception {
        when(equipmentService.countEquipmentByStatus(EquipmentStatus.AVAILABLE)).thenReturn(5L);

        mockMvc.perform(get("/api/medical/equipment/status/AVAILABLE/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    void updateEquipmentStatus_shouldReturn200() throws Exception {
        sampleEquipment.setStatus(EquipmentStatus.LOANED);
        when(equipmentService.updateEquipmentStatus(1L, EquipmentStatus.LOANED))
                .thenReturn(sampleEquipment);

        mockMvc.perform(patch("/api/medical/equipment/1/status")
                        .param("status", "LOANED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOANED"));
    }
}
