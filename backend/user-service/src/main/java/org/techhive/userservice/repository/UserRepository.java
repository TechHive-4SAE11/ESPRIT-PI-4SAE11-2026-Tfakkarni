package org.techhive.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.techhive.userservice.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByKeycloakId(String keycloakId);

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  List<User> findByRole(String role);
}
