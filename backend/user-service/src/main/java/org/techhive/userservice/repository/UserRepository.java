package org.techhive.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.techhive.userservice.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByKeycloakId(String keycloakId);

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  @Query("SELECT u FROM User u WHERE LOWER(u.role) = LOWER(:role)")
  List<User> findByRole(@Param("role") String role);

  List<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(String firstName, String lastName);
}
