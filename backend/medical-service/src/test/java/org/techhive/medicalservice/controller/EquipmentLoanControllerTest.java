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
import org.techhive.medicalservice.entity.EquipmentLoan;
import org.techhive.medicalservice.entity.enums.EquipmentStatus;
import org.techhive.medicalservice.entity.enums.LoanStatus;
import org.techhive.medicalservice.service.IEquipmentLoanService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EquipmentLoanController.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class EquipmentLoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IEquipmentLoanService loanService;

    @Autowired
    private ObjectMapper objectMapper;

    private EquipmentLoan sampleLoan;
    private Equipment sampleEquipment;

    @BeforeEach
    void setUp() {
        sampleEquipment = new Equipment();
        sampleEquipment.setId(1L);
        sampleEquipment.setName("Wheelchair");
        sampleEquipment.setStatus(EquipmentStatus.LOANED);

        sampleLoan = new EquipmentLoan();
        sampleLoan.setId(1L);
        sampleLoan.setEquipment(sampleEquipment);
        sampleLoan.setBorrowerId(50L);
        sampleLoan.setLoanDate(LocalDateTime.now());
        sampleLoan.setDueDate(LocalDateTime.now().plusDays(14));
        sampleLoan.setStatus(LoanStatus.ACTIVE);
        sampleLoan.setPurpose("Patient rehabilitation");
    }

    @Test
    void createLoan_shouldReturn201() throws Exception {
        when(loanService.createLoan(any())).thenReturn(sampleLoan);

        String json = """
                {
                  "equipmentId": 1,
                  "borrowerId": 50,
                  "loanDate": "2026-04-15T00:00:00",
                  "dueDate": "2027-04-30T00:00:00",
                  "purpose": "Patient rehabilitation",
                  "status": "ACTIVE"
                }
                """;

        mockMvc.perform(post("/api/medical/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.borrowerId").value(50));
    }

    @Test
    void getLoanById_whenExists_shouldReturn200() throws Exception {
        when(loanService.getLoanById(1L)).thenReturn(sampleLoan);

        mockMvc.perform(get("/api/medical/loans/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.borrowerId").value(50));
    }

    @Test
    void getLoanById_whenNotExists_shouldReturn404() throws Exception {
        when(loanService.getLoanById(99L)).thenReturn(null);

        mockMvc.perform(get("/api/medical/loans/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllLoans_shouldReturn200() throws Exception {
        when(loanService.getAllLoans()).thenReturn(List.of(sampleLoan));

        mockMvc.perform(get("/api/medical/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].borrowerId").value(50));
    }

    @Test
    void deleteLoan_shouldReturn204() throws Exception {
        doNothing().when(loanService).deleteLoan(1L);

        mockMvc.perform(delete("/api/medical/loans/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getLoansByBorrowerId_shouldReturn200() throws Exception {
        when(loanService.getLoansByBorrowerId(50L)).thenReturn(List.of(sampleLoan));

        mockMvc.perform(get("/api/medical/loans/borrower/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].borrowerId").value(50));
    }

    @Test
    void getLoansByStatus_shouldReturn200() throws Exception {
        when(loanService.getLoansByStatus(LoanStatus.ACTIVE)).thenReturn(List.of(sampleLoan));

        mockMvc.perform(get("/api/medical/loans/status/ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void returnEquipment_shouldReturn200() throws Exception {
        sampleLoan.setStatus(LoanStatus.RETURNED);
        when(loanService.returnEquipment(1L)).thenReturn(sampleLoan);

        mockMvc.perform(post("/api/medical/loans/1/return"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));
    }

    @Test
    void returnEquipment_whenNotFound_shouldReturn404() throws Exception {
        when(loanService.returnEquipment(99L)).thenReturn(null);

        mockMvc.perform(post("/api/medical/loans/99/return"))
                .andExpect(status().isNotFound());
    }

    @Test
    void extendLoan_shouldReturn200() throws Exception {
        when(loanService.extendLoan(1L, 7)).thenReturn(sampleLoan);

        String json = """
                {"days": 7}
                """;

        mockMvc.perform(post("/api/medical/loans/1/extend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }

    @Test
    void cancelLoan_shouldReturn200() throws Exception {
        sampleLoan.setStatus(LoanStatus.CANCELLED);
        when(loanService.cancelLoan(1L)).thenReturn(sampleLoan);

        mockMvc.perform(post("/api/medical/loans/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void isEquipmentLoaned_shouldReturn200() throws Exception {
        when(loanService.isEquipmentLoaned(1L)).thenReturn(true);

        mockMvc.perform(get("/api/medical/loans/equipment/1/loaned"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void countActiveLoansByBorrower_shouldReturn200() throws Exception {
        when(loanService.countActiveLoansByBorrower(50L)).thenReturn(3L);

        mockMvc.perform(get("/api/medical/loans/borrower/50/active/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }

    @Test
    void checkAndUpdateOverdueLoans_shouldReturn200() throws Exception {
        doNothing().when(loanService).checkAndUpdateOverdueLoans();

        mockMvc.perform(post("/api/medical/loans/check-overdue"))
                .andExpect(status().isOk())
                .andExpect(content().string("Overdue loans updated"));
    }
}
