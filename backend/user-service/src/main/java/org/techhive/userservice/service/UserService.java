package org.techhive.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
    log.info("Querying database for users with role: {}", role);
    try {
      List<User> users = userRepository.findByRole(role);
      log.info("Query successful. Found {} users with role: {}", users.size(), role);
      return users;
    } catch (Exception e) {
      log.error("Database error while fetching users by role: {}", role, e);
      throw e;
    }
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
}
