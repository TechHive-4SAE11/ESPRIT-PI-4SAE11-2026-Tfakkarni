package org.techhive.assistantservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import org.techhive.assistantservice.client.dto.EquipmentDTO;
import org.techhive.assistantservice.client.dto.EquipmentLoanDTO;
import org.techhive.assistantservice.dto.MedicalFolderDTO;
import org.techhive.assistantservice.dto.PatientDTO;

import java.util.List;

@FeignClient(name = "medical-service", url = "${feign.medical-service.url:http://localhost:18086}")
public interface MedicalServiceClient {

    // ── Equipment endpoints ──
    @GetMapping("/api/medical/equipment")
    List<EquipmentDTO> getAllEquipment();

    @GetMapping("/api/medical/equipment/{id}")
    EquipmentDTO getEquipmentById(@PathVariable("id") Long id);

    @PostMapping("/api/medical/equipment")
    EquipmentDTO createEquipment(@RequestBody EquipmentDTO equipmentDTO);

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

    // ── Patient and Medical Folder endpoints ──
    @GetMapping("/api/medical/patients/search")
    PatientDTO findPatientByName(@RequestParam("name") String name);

    @GetMapping("/api/medical-folders/patient/{patientId}")
    List<MedicalFolderDTO> getMedicalFolderByPatient(@PathVariable("patientId") String patientId);

    @GetMapping(value = "/api/medical-folders/patient/{patientId}", consumes = "application/json")
    String getMedicalFolderRaw(@PathVariable("patientId") String patientId);

    @GetMapping("/api/ai-reports/latest")
    org.techhive.assistantservice.dto.AIReportDTO getLatestAIReport(@RequestParam("folderId") Long folderId);
}
