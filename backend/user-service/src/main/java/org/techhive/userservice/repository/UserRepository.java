package org.techhive.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.techhive.userservice.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByKeycloakId(String keycloakId);

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  @Query("SELECT u FROM User u WHERE LOWER(u.role) = LOWER(:role)")
  List<User> findByRole(@Param("role") String role);

  /**
   * Find enabled patient users who have been inactive for too long.
   * A patient is considered inactive if:
   *   - lastActiveAt is null AND createdAt is before the threshold (never logged in after feature rolled out), OR
   *   - lastActiveAt is not null AND lastActiveAt is before the threshold
   */
  @Query("""
      SELECT u FROM User u
      WHERE LOWER(u.role) = 'patient'
        AND u.enabled = true
        AND (
          (u.lastActiveAt IS NULL AND u.createdAt < :threshold)
          OR u.lastActiveAt < :threshold
        )
      """)
  List<User> findInactivePatients(@Param("threshold") LocalDateTime threshold);
}
