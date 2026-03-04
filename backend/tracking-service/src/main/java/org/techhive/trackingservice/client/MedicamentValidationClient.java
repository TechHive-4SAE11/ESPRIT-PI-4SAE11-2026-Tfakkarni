package org.techhive.trackingservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.techhive.trackingservice.dto.MedicamentValidationResultDTO;

/**
 * Feign client for communicating with the medicament-validation-service.
 * Uses Eureka service discovery to resolve the service URL.
 */
@FeignClient(
    name = "medicament-validation-service",
    fallback = MedicamentValidationClientFallback.class
)
public interface MedicamentValidationClient {

    @GetMapping("/api/medicament-validation/validate")
    MedicamentValidationResultDTO validateMedicament(@RequestParam("name") String name);
}
