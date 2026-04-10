package org.techhive.analyticsservice.client;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class UserServiceClientFallback implements UserServiceClient {

    @Override
    public Map<String, Object> getUserByKeycloakId(String keycloakId) {
        return Collections.emptyMap();
    }

    @Override
    public List<Map<String, Object>> getUsersByRole(String role) {
        return Collections.emptyList();
    }
}
