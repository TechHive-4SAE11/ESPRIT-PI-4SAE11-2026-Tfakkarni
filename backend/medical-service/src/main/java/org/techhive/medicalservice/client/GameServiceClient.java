package org.techhive.medicalservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "game-service")
public interface GameServiceClient {

    @GetMapping("/api/games/stats/analytics/{patientId}")
    JsonNode getPatientAnalytics(@PathVariable("patientId") String patientId);
}
