package org.techhive.userservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.techhive.userservice.dto.AdminResetPasswordRequest;
import org.techhive.userservice.dto.ChangePasswordRequest;
import org.techhive.userservice.dto.RegisterRequest;
import org.techhive.userservice.dto.UpdateProfileRequest;
import org.techhive.userservice.entity.User;
import org.techhive.userservice.service.DiditKycService;
import org.techhive.userservice.service.KeycloakUserService;
import org.techhive.userservice.service.UserService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final KeycloakUserService keycloakUserService;
  private final UserService userService;
  private final DiditKycService diditKycService;

  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
    log.info("Registration request received for email: '{}', role: '{}'",
        request.getEmail(), request.getRole());

    try {
      keycloakUserService.registerUser(request);
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(Map.of("message", "User registered successfully"));
    } catch (HttpClientErrorException.Conflict e) {
      log.warn("User already exists: {}", request.getEmail());
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(Map.of("error", "User already exists with this username or email"));
    } catch (HttpClientErrorException e) {
      log.error("Keycloak error during registration: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
      return ResponseEntity.status(e.getStatusCode())
          .body(Map.of("error", "Registration failed: " + e.getResponseBodyAsString()));
    } catch (Exception e) {
      log.error("Unexpected error during registration", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Registration failed: " + e.getMessage()));
    }
  }

  @GetMapping
  public ResponseEntity<List<User>> getAllUsers() {
    return ResponseEntity.ok(userService.getAllUsers());
  }

  @GetMapping("/role/{role}")
  public ResponseEntity<?> getUsersByRole(@PathVariable String role) {
    try {
      List<User> users = userService.getUsersByRole(role);
      return ResponseEntity.ok(users);
    } catch (Exception e) {
      log.error("Error fetching users by role: {}", role, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Failed to fetch users: " + e.getMessage()));
    }
  }

  @GetMapping("/keycloak/{keycloakId}")
  public ResponseEntity<?> getUserByKeycloakId(@PathVariable String keycloakId) {
    return userService.getUserByKeycloakId(keycloakId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body((User) null));
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> getUserById(@PathVariable Long id) {
    return userService.getUserById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body((User) null));
  }

  @PutMapping("/profile/{keycloakId}")
  public ResponseEntity<?> updateProfile(
      @PathVariable String keycloakId,
      @RequestBody UpdateProfileRequest request) {
    try {
      User updatedUser = userService.updateProfile(keycloakId, request);
      keycloakUserService.updateKeycloakUser(keycloakId, request);
      return ResponseEntity.ok(updatedUser);
    } catch (RuntimeException e) {
      log.error("Profile update failed for keycloakId: {}", keycloakId, e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
      log.error("Unexpected error updating profile", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Échec de la mise à jour du profil"));
    }
  }

  @DeleteMapping("/keycloak/{keycloakId}")
  public ResponseEntity<?> deleteUser(@PathVariable String keycloakId) {
    try {
      userService.deleteUser(keycloakId);
      keycloakUserService.deleteKeycloakUser(keycloakId);
      return ResponseEntity.ok(Map.of("message", "Utilisateur supprimé avec succès"));
    } catch (RuntimeException e) {
      log.error("Delete user failed for keycloakId: {}", keycloakId, e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
      log.error("Unexpected error deleting user", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Échec de la suppression de l'utilisateur"));
    }
  }

  @PutMapping("/role/{keycloakId}")
  public ResponseEntity<?> updateRole(
      @PathVariable String keycloakId,
      @RequestBody Map<String, String> body) {
    try {
      String newRole = body.get("role");
      if (newRole == null || newRole.isBlank()) {
        return ResponseEntity.badRequest().body(Map.of("error", "Le rôle est requis"));
      }
      User user = userService.getUserByKeycloakId(keycloakId)
          .orElseThrow(() -> new RuntimeException("User not found"));
      String oldRole = user.getRole();

      User updatedUser = userService.updateRole(keycloakId, newRole);
      keycloakUserService.updateKeycloakUserRole(keycloakId, oldRole, newRole);
      return ResponseEntity.ok(updatedUser);
    } catch (RuntimeException e) {
      log.error("Update role failed for keycloakId: {}", keycloakId, e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
      log.error("Unexpected error updating role", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Échec de la mise à jour du rôle"));
    }
  }

  /**
   * Change user password (verifies current password, then sets new one).
   */
  @PutMapping("/password/{keycloakId}")
  public ResponseEntity<?> changePassword(
      @PathVariable String keycloakId,
      @RequestBody ChangePasswordRequest request) {
    try {
      keycloakUserService.changePassword(keycloakId, request);
      return ResponseEntity.ok(Map.of("message", "Mot de passe modifié avec succès"));
    } catch (RuntimeException e) {
      log.error("Password change failed for keycloakId: {}", keycloakId, e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
      log.error("Unexpected error changing password", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Échec du changement de mot de passe"));
    }
  }

  /**
   * Admin reset password — no current password verification.
   */
  @PutMapping("/admin-reset-password/{keycloakId}")
  public ResponseEntity<?> adminResetPassword(
      @PathVariable String keycloakId,
      @RequestBody AdminResetPasswordRequest request) {
    try {
      if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", "Le mot de passe doit contenir au moins 6 caractères"));
      }
      keycloakUserService.adminResetPassword(keycloakId, request.getNewPassword());
      return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé avec succès"));
    } catch (RuntimeException e) {
      log.error("Admin password reset failed for keycloakId: {}", keycloakId, e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
      log.error("Unexpected error resetting password", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Échec de la réinitialisation du mot de passe"));
    }
  }

  /**
   * Enable or disable a user account.
   */
  @PutMapping("/toggle-enabled/{keycloakId}")
  public ResponseEntity<?> toggleEnabled(
      @PathVariable String keycloakId,
      @RequestBody Map<String, Boolean> body) {
    try {
      Boolean enabled = body.get("enabled");
      if (enabled == null) {
        return ResponseEntity.badRequest().body(Map.of("error", "Le champ 'enabled' est requis"));
      }
      User updatedUser = userService.toggleEnabled(keycloakId, enabled);
      keycloakUserService.setUserEnabled(keycloakId, enabled);
      return ResponseEntity.ok(updatedUser);
    } catch (RuntimeException e) {
      log.error("Toggle enabled failed for keycloakId: {}", keycloakId, e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
      log.error("Unexpected error toggling user", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Échec de la modification du statut"));
    }
  }

  // ─── KYC Endpoints ────────────────────────────────────────────

  /**
   * Start a Didit KYC verification session for a doctor.
   * Returns session_id, verification url, and status.
   */
  @PostMapping("/kyc/start/{keycloakId}")
  public ResponseEntity<?> startKyc(@PathVariable String keycloakId) {
    try {
      Map<String, String> result = diditKycService.createSession(keycloakId);
      return ResponseEntity.ok(result);
    } catch (RuntimeException e) {
      log.error("KYC start failed for keycloakId: {}", keycloakId, e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Check the KYC verification status for a user.
   */
  @GetMapping("/kyc/status/{keycloakId}")
  public ResponseEntity<?> getKycStatus(@PathVariable String keycloakId) {
    try {
      Map<String, String> result = diditKycService.getSessionStatus(keycloakId);
      return ResponseEntity.ok(result);
    } catch (RuntimeException e) {
      log.error("KYC status check failed for keycloakId: {}", keycloakId, e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Skip KYC verification (dev/testing only).
   */
  @PutMapping("/kyc/skip/{keycloakId}")
  public ResponseEntity<?> skipKyc(@PathVariable String keycloakId) {
    try {
      User user = diditKycService.skipKyc(keycloakId);
      return ResponseEntity.ok(Map.of("message", "KYC skipped", "kycStatus", user.getKycStatus()));
    } catch (RuntimeException e) {
      log.error("KYC skip failed for keycloakId: {}", keycloakId, e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", e.getMessage()));
    }
  }
}
