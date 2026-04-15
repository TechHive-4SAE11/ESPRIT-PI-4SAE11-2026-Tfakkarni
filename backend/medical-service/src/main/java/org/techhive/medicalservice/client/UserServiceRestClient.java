package org.techhive.medicalservice.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class UserServiceRestClient {

    private final RestClient userServiceRestClient;

    public UserServiceRestClient(RestClient userServiceRestClient) {
        this.userServiceRestClient = userServiceRestClient;
    }

    public List<Map<String, Object>> searchUsersByName(String name) {
        return userServiceRestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/users/search")
                        .queryParam("name", name)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
    }
}
