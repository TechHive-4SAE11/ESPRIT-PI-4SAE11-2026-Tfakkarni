package org.techhive.medicalservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.cloud.openfeign.FeignClient;
import org.techhive.medicalservice.dto.game.GameStatsDTO;

@FeignClient(name = "game-service", path = "/api/games")
public interface GameServiceClient {

    @GetMapping("/stats/patient/{patientId}")
    GameStatsDTO getPatientGameStats(@PathVariable("patientId") String patientId);

    @GetMapping("/stats/analytics/{patientId}")
    JsonNode getPatientAnalytics(@PathVariable("patientId") String patientId);
}