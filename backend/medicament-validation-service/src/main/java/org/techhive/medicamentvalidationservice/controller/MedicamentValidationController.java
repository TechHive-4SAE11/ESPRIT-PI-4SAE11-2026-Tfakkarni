package org.techhive.medicamentvalidationservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.medicamentvalidationservice.batch.MedicamentBatchScheduler;
import org.techhive.medicamentvalidationservice.dto.BatchValidationResultDTO;
import org.techhive.medicamentvalidationservice.dto.ValidationResultDTO;
import org.techhive.medicamentvalidationservice.service.MedicamentValidationService;

import java.util.List;
import java.util.Map;

/**
 * REST controller for medicament validation endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/medicament-validation")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MedicamentValidationController {

    private final MedicamentValidationService validationService;
    private final MedicamentBatchScheduler batchScheduler;

    @GetMapping("/validate")
    public ResponseEntity<ValidationResultDTO> validateDrug(@RequestParam String name) {
        log.info("Validating medicament: {}", name);
        ValidationResultDTO result = validationService.validateDrugName(name);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/validate-batch")
    public ResponseEntity<BatchValidationResultDTO> validateBatch(@RequestBody List<String> names) {
        log.info("Batch validating {} medicaments", names.size());
        BatchValidationResultDTO result = validationService.validateBatch(names);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        long count = validationService.getMedicamentCount();
        return ResponseEntity.ok(Map.of(
                "status", count > 0 ? "READY" : "LOADING",
                "totalMedicaments", count,
                "message", count > 0
                        ? "Database loaded with " + count + " validated medicaments"
                        : "Database is empty. Batch job may still be running or hasn't started yet."
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshDatabase() {
        log.info("Manual refresh of medicament database triggered");
        batchScheduler.runJob();
        return ResponseEntity.ok(Map.of(
                "message", "Medicament database refresh triggered. Check /status for progress."
        ));
    }
}
