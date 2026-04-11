package org.techhive.analyticsservice.client;

import org.techhive.analyticsservice.dto.GameStatsResponse;
import org.techhive.analyticsservice.dto.ScoreAnalyticsResponse;
import org.techhive.analyticsservice.dto.DataPointPerformanceDTO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class GameServiceClientFallback implements GameServiceClient {

    @Override
    public GameStatsResponse getPlayerStats(String keycloakId) {
        return new GameStatsResponse();
    }

    @Override
    public ScoreAnalyticsResponse getScoreAnalytics(String keycloakId) {
        return new ScoreAnalyticsResponse();
    }

    @Override
    public List<DataPointPerformanceDTO> getPerformanceData(String keycloakId) {
        return Collections.emptyList();
    }

    @Override
    public List<Map<String, Object>> getPatientTags(String keycloakId) {
        return Collections.emptyList();
    }
}
