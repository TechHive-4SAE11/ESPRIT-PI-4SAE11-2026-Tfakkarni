package org.techhive.assistantservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import org.techhive.assistantservice.client.dto.EquipmentDTO;
import org.techhive.assistantservice.client.dto.EquipmentLoanDTO;

import java.util.List;

@FeignClient(name = "medical-service", url = "${feign.medical-service.url:http://localhost:18083}")
public interface MedicalServiceClient {

    // ── Equipment endpoints ──
    @GetMapping("/api/medical/equipment")
    List<EquipmentDTO> getAllEquipment();

    @GetMapping("/api/medical/equipment/{id}")
    EquipmentDTO getEquipmentById(@PathVariable("id") Long id);

    @GetMapping("/api/medical/equipment/available")
    List<EquipmentDTO> getAvailableEquipment();

    @GetMapping("/api/medical/equipment/category/{category}")
    List<EquipmentDTO> getEquipmentByCategory(@PathVariable("category") String category);

    @GetMapping("/api/medical/equipment/search")
    List<EquipmentDTO> searchEquipment(@RequestParam("name") String name);

    // ── Loan endpoints ──
    @PostMapping("/api/medical/loans/borrow")
    EquipmentLoanDTO borrowEquipment(@RequestBody EquipmentLoanDTO loanDTO);

    @PostMapping("/api/medical/loans/{id}/return")
    EquipmentLoanDTO returnEquipment(@PathVariable("id") Long id);

    @GetMapping("/api/medical/loans/borrower/{borrowerId}/active")
    List<EquipmentLoanDTO> getActiveLoansByBorrower(@PathVariable("borrowerId") Long borrowerId);

    @GetMapping("/api/medical/loans/borrower/{borrowerId}")
    List<EquipmentLoanDTO> getLoansByBorrowerId(@PathVariable("borrowerId") Long borrowerId);

    @GetMapping("/api/medical/loans")
    List<EquipmentLoanDTO> getAllLoans();
}
