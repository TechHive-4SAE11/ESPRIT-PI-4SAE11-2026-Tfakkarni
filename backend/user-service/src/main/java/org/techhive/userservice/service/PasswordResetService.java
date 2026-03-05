package org.techhive.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.techhive.userservice.repository.UserRepository;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final RestTemplate restTemplate;
    private final KeycloakUserService keycloakUserService;
    private final UserRepository userRepository;

    @Value("${mailtrap.token}")
    private String mailtrapToken;

    @Value("${mailtrap.inbox-id}")
    private String inboxId;

    @Value("${mailtrap.from:noreply@tfakkarni.com}")
    private String fromEmail;

    // OTP store : email → {code, expiry}
    private final ConcurrentHashMap<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    private record OtpEntry(String code, Instant expiry) {}

    // ─── Public API ────────────────────────────────────────────────────────────

    /**
     * Generate a 6-digit OTP and send it to the user's email.
     * Silently ignores unknown emails (security best practice).
     */
    public void sendOtp(String email) {
        var userOpt = userRepository.findByEmail(email.toLowerCase().trim());
        if (userOpt.isEmpty()) {
            log.warn("Password reset requested for unknown email: {}", email);
            // Do NOT throw — prevents user enumeration
            return;
        }

        String code = String.format("%06d", new Random().nextInt(1_000_000));
        otpStore.put(email.toLowerCase().trim(), new OtpEntry(code, Instant.now().plusSeconds(900)));

        try {
            sendEmail(email, code);
            log.info("Password reset OTP sent to {}", email);
        } catch (Exception e) {
            otpStore.remove(email.toLowerCase().trim());
            throw e;
        }
    }

    /**
     * Verify OTP and reset the password.
     */
    public void verifyAndReset(String email, String code, String newPassword) {
        String key = email.toLowerCase().trim();
        OtpEntry entry = otpStore.get(key);

        if (entry == null) {
            throw new RuntimeException("Aucun code envoyé pour cet email. Veuillez demander un nouveau code.");
        }
        if (Instant.now().isAfter(entry.expiry())) {
            otpStore.remove(key);
            throw new RuntimeException("Le code a expiré (15 min). Veuillez demander un nouveau code.");
        }
        if (!entry.code().equals(code.trim())) {
            throw new RuntimeException("Code incorrect. Vérifiez votre email et réessayez.");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("Le mot de passe doit contenir au moins 6 caractères.");
        }

        var user = userRepository.findByEmail(key)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

        keycloakUserService.adminResetPassword(user.getKeycloakId(), newPassword);
        otpStore.remove(key);
        log.info("Password successfully reset for {}", email);
    }

    // ─── Private helpers ────────────────────────────────────────────────────────

    private void sendEmail(String to, String code) {
        // Mailtrap Sandbox endpoint: https://sandbox.api.mailtrap.io/api/send/{inboxId}
        String url = "https://sandbox.api.mailtrap.io/api/send/" + inboxId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(mailtrapToken);

        Map<String, Object> body = new HashMap<>();
        body.put("from",    Map.of("email", fromEmail, "name", "Tfakkarni"));
        body.put("to",      List.of(Map.of("email", to)));
        body.put("subject", "Réinitialisation de mot de passe – Tfakkarni");
        body.put("html",    buildHtml(code));
        body.put("text",    "Votre code de réinitialisation : " + code + "\n\nCe code expire dans 15 minutes.");

        try {
            restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
        } catch (Exception e) {
            log.error("Mailtrap send failed: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de l'envoi de l'email de réinitialisation.");
        }
    }

    private String buildHtml(String code) {
        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
            <body style="margin:0;padding:0;background:#f4f4f8;font-family:Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f4f8;padding:40px 20px;">
                <tr><td align="center">
                  <table width="480" cellpadding="0" cellspacing="0"
                    style="background:white;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.10);">
                    <!-- Header -->
                    <tr>
                      <td style="background:linear-gradient(135deg,#6d28d9,#7c3aed);padding:32px 24px;text-align:center;">
                        <h1 style="color:white;margin:0;font-size:28px;font-weight:700;letter-spacing:-0.5px;">tfakkarni</h1>
                        <p style="color:rgba(255,255,255,0.85);margin:8px 0 0;font-size:14px;">Plateforme de suivi Alzheimer</p>
                      </td>
                    </tr>
                    <!-- Body -->
                    <tr>
                      <td style="padding:40px 32px;text-align:center;">
                        <p style="color:#374151;font-size:17px;margin:0 0 8px;font-weight:600;">Réinitialisation de mot de passe</p>
                        <p style="color:#6b7280;font-size:14px;margin:0 0 32px;">Utilisez ce code pour réinitialiser votre mot de passe&nbsp;:</p>
                        <div style="background:#faf5ff;border:2px dashed #7c3aed;border-radius:12px;padding:28px 16px;margin:0 0 28px;">
                          <span style="font-size:44px;font-weight:800;letter-spacing:14px;color:#6d28d9;display:block;line-height:1;">%s</span>
                        </div>
                        <p style="color:#9ca3af;font-size:13px;margin:0;">
                          ⏱ Ce code expire dans <strong style="color:#374151;">15 minutes</strong>.<br>
                          Si vous n'avez pas fait cette demande, ignorez cet email.
                        </p>
                      </td>
                    </tr>
                    <!-- Footer -->
                    <tr>
                      <td style="background:#f9fafb;border-top:1px solid #e5e7eb;padding:16px 24px;text-align:center;">
                        <p style="color:#9ca3af;font-size:12px;margin:0;">© 2026 Tfakkarni – Document confidentiel</p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(code);
    }
}
