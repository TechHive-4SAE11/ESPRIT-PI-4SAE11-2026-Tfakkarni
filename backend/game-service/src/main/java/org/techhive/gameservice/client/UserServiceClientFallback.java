package org.techhive.gameservice.client;

import org.springframework.stereotype.Component;
import org.techhive.gameservice.dto.UserResponse;

@Component
public class UserServiceClientFallback implements UserServiceClient {

    @Override
    public UserResponse getUserByKeycloakId(String keycloakId) {
        return null;
    }
}
