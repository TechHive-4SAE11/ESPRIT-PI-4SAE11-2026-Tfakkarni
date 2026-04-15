package org.techhive.gameservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String keycloakId;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private String gender;
}
