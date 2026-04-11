package org.techhive.medicalservice.client;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.cloud.openfeign.FeignClient;
import java.util.List;
import org.techhive.medicalservice.dto.game.GameStatsDTO;

@FeignClient(name = "game-service", path = "/api/game")
public interface GameServiceClient {

    @GetMapping("/stats/patient/{patientId}")
    GameStatsDTO getPatientGameStats(@PathVariable("patientId") String patientId);
}

