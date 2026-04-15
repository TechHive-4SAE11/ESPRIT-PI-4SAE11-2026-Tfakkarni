package org.techhive.iotservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
public class AlertServiceClientFallback implements AlertServiceClient {

    @Override
    public Map<String, Object> createIotAlert(Map<String, Object> request) {
        log.warn("Alert-service unavailable — IoT alert not persisted for patient {}",
                request.get("patientId"));
        return Collections.emptyMap();
    }
}
