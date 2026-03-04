package org.techhive.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.techhive.userservice.dto.UpdateProfileRequest;
import org.techhive.userservice.entity.User;
import org.techhive.userservice.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  public List<User> getAllUsers() {
    return userRepository.findAll();
  }

  public List<User> getUsersByRole(String role) {
    return userRepository.findByRole(role);
  }

  public Optional<User> getUserByKeycloakId(String keycloakId) {
    return userRepository.findByKeycloakId(keycloakId);
  }

  public Optional<User> getUserById(Long id) {
    return userRepository.findById(id);
  }

  public Optional<User> getUserByEmail(String email) {
    return userRepository.findByEmail(email);
  }

  public void deleteUser(String keycloakId) {
    User user = userRepository.findByKeycloakId(keycloakId)
        .orElseThrow(() -> new RuntimeException("User not found with keycloakId: " + keycloakId));
    userRepository.delete(user);
    log.info("User '{}' deleted from local database", keycloakId);
  }

  public User updateRole(String keycloakId, String newRole) {
    User user = userRepository.findByKeycloakId(keycloakId)
        .orElseThrow(() -> new RuntimeException("User not found with keycloakId: " + keycloakId));
    user.setRole(newRole);
    return userRepository.save(user);
  }

  public User updateProfile(String keycloakId, UpdateProfileRequest request) {
    User user = userRepository.findByKeycloakId(keycloakId)
        .orElseThrow(() -> new RuntimeException("User not found with keycloakId: " + keycloakId));

    if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
      user.setFirstName(request.getFirstName().trim());
    }
    if (request.getLastName() != null && !request.getLastName().isBlank()) {
      user.setLastName(request.getLastName().trim());
    }
    if (request.getEmail() != null && !request.getEmail().isBlank()) {
      user.setEmail(request.getEmail().trim());
    }
    if (request.getPhone() != null) {
      user.setPhone(request.getPhone().trim().isEmpty() ? null : request.getPhone().trim());
    }

    return userRepository.save(user);
  }

  /**
   * Toggle user enabled status in local database.
   */
  public User toggleEnabled(String keycloakId, boolean enabled) {
    User user = userRepository.findByKeycloakId(keycloakId)
        .orElseThrow(() -> new RuntimeException("User not found with keycloakId: " + keycloakId));
    user.setEnabled(enabled);
    return userRepository.save(user);
  }

  /**
   * Save/update a user entity directly.
   */
  public User save(User user) {
    return userRepository.save(user);
  }
}
