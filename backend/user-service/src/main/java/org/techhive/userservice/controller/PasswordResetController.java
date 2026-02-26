package org.techhive.userservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.userservice.service.PasswordResetService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/password-reset")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    /**
     * Step 1 – Send OTP to email.
     * POST /api/password-reset/forgot
     * Body: { "email": "user@example.com" }
     */
    @PostMapping("/forgot")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "L'email est requis"));
        }
        try {
            passwordResetService.sendOtp(email.trim());
            // Always return 200 (don't reveal if email exists)
            return ResponseEntity.ok(Map.of("message", "Si cet email existe, un code vous a été envoyé."));
        } catch (RuntimeException e) {
            log.error("Error sending OTP: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Step 2 – Verify OTP and reset password.
     * POST /api/password-reset/verify
     * Body: { "email": "...", "code": "123456", "newPassword": "..." }
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyAndReset(@RequestBody Map<String, String> body) {
        String email       = body.get("email");
        String code        = body.get("code");
        String newPassword = body.get("newPassword");

        if (email == null || email.isBlank() || code == null || code.isBlank() || newPassword == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "email, code et newPassword sont requis"));
        }
        try {
            passwordResetService.verifyAndReset(email.trim(), code.trim(), newPassword);
            return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé avec succès"));
        } catch (RuntimeException e) {
            log.error("Password reset verify failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }
}
