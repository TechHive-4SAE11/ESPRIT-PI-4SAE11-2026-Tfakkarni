package org.techhive.analyticsservice.client;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class IotServiceClientFallback implements IotServiceClient {

    @Override
    public List<Map<String, Object>> getHeartbeatReadings(String patientId, String date) {
        return Collections.emptyList();
    }

    @Override
    public Map<String, Object> getSleepAnalysis(String patientId, String date) {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Object> getLatestReading(String patientId) {
        return Collections.emptyMap();
    }
}
