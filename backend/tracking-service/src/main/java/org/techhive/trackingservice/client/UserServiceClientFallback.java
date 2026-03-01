package org.techhive.trackingservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Fallback for UserServiceClient — returns null/empty if user-service is unavailable.
 */
@Slf4j
@Component
public class UserServiceClientFallback implements UserServiceClient {

    @Override
    public Map<String, Object> getUserByKeycloakId(String keycloakId) {
        log.warn("User-service unavailable. Cannot fetch user for keycloakId '{}'", keycloakId);
        return null;
    }

    @Override
    public byte[] getDoctorSignature(Long id) {
        log.warn("User-service unavailable. Cannot fetch signature for user #{}", id);
        return null;
    }
}
