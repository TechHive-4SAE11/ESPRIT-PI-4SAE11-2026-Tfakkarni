package org.techhive.mlservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.techhive.mlservice.client.GameServiceClient;
import org.techhive.mlservice.dto.GameStatsResponse;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final GameServiceClient gameServiceClient;

    public Map<String, Object> getAlerts(String keycloakId) {
        GameStatsResponse stats = gameServiceClient.getGameStats(keycloakId);
        
        Map<String, Object> response = new HashMap<>();
        
        if (stats == null) {
            response.put("severity", "INFO");
            response.put("message", "Aucune donnée");
            return response;
        }

        double score = stats.getAverageScore();
        response.put("score", score);
        
        if (score < 50) {
            response.put("severity", "CRITIQUE");
            response.put("message", "Score très bas");
            response.put("action", "Prendre rendez-vous");
        } else if (score <= 70) {
            response.put("severity", "MODEREE");
            response.put("message", "Score moyen");
            response.put("action", "Planifier suivi");
        } else {
            response.put("severity", "INFO");
            response.put("message", "Score normal");
            response.put("action", null);
        }
        
        return response;
    }
}
