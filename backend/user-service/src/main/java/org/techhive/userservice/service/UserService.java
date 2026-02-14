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
    return userRepository.findByRole(role);
  }

  public Optional<User> getUserByKeycloakId(String keycloakId) {
    return userRepository.findByKeycloakId(keycloakId);
  }

  public Optional<User> getUserByEmail(String email) {
    return userRepository.findByEmail(email);
  }
}
