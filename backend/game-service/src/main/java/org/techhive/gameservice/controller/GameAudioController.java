package org.techhive.gameservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.gameservice.dto.AudioGenerateRequest;
import org.techhive.gameservice.service.AudioTranslationService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/games/audio")
@RequiredArgsConstructor
public class GameAudioController {

  private final AudioTranslationService audioTranslationService;

  /**
   * Generate TTS audio for a game question or prompt.
   *
   * POST /api/games/audio/generate-question
   * Body: { originalText, targetLanguageCode, voiceId?, gameType, patientName?,
   * patientGender? }
   * Returns: audio/mpeg bytes
   */
  @PostMapping("/generate-question")
  public ResponseEntity<?> generateQuestionAudio(@RequestBody AudioGenerateRequest request) {
    log.info("[GameAudio] Request: type={}, lang={}, text='{}'",
        request.getGameType(), request.getTargetLanguageCode(),
        request.getOriginalText() != null
            ? request.getOriginalText().substring(0, Math.min(50, request.getOriginalText().length()))
            : "null");

    // Validation
    if (request.getTargetLanguageCode() == null || request.getTargetLanguageCode().isBlank()) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "targetLanguageCode is required (e.g., 'en' or 'tn')"));
    }
    if (request.getGameType() == null || request.getGameType().isBlank()) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "gameType is required (PHOTO, PLACE, MOVIE, or QUESTION)"));
    }
    if ("QUESTION".equalsIgnoreCase(request.getGameType())
        && (request.getOriginalText() == null || request.getOriginalText().isBlank())) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "originalText is required for QUESTION game type"));
    }

    try {
      byte[] audioBytes = audioTranslationService.generateQuestionAudio(request);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.valueOf("audio/mpeg"));
      headers.setContentLength(audioBytes.length);
      headers.set("Cache-Control", "no-cache");

      return new ResponseEntity<>(audioBytes, headers, HttpStatus.OK);

    } catch (Exception e) {
      log.error("[GameAudio] Failed to generate audio: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
          .body(Map.of("error", "Failed to generate audio: " + e.getMessage()));
    }
  }

  /**
   * Translate text only (without TTS) — useful for debugging or displaying
   * translated text.
   *
   * POST /api/games/audio/translate
   * Body: { originalText, targetLanguageCode }
   * Returns: { translatedText: "..." }
   */
  @PostMapping("/translate")
  public ResponseEntity<?> translateOnly(@RequestBody AudioGenerateRequest request) {
    if (request.getOriginalText() == null || request.getOriginalText().isBlank()) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "originalText is required"));
    }
    if (request.getTargetLanguageCode() == null || request.getTargetLanguageCode().isBlank()) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "targetLanguageCode is required"));
    }

    try {
      String targetLang = "tn".equalsIgnoreCase(request.getTargetLanguageCode())
          ? "ar"
          : request.getTargetLanguageCode();
      String translated = audioTranslationService.translateText(request.getOriginalText(), targetLang);
      return ResponseEntity.ok(Map.of("translatedText", translated));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
          .body(Map.of("error", "Translation failed: " + e.getMessage()));
    }
  }
}
