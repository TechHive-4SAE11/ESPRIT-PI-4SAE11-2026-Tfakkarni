package org.techhive.gameservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.techhive.gameservice.dto.AudioGenerateRequest;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
public class AudioTranslationService {

  private final WebClient webClient;

  @Value("${google.translate.api-key}")
  private String googleTranslateApiKey;

  @Value("${elevenlabs.api-key}")
  private String elevenLabsApiKey;

  @Value("${elevenlabs.voice-id-en}")
  private String voiceIdEn;

  @Value("${elevenlabs.voice-id-tn}")
  private String voiceIdTn;

  @Value("${elevenlabs.model-id}")
  private String modelId;

  // ── Fixed opening phrases per game type ──────────────────────────────────

  private static final List<String> PHOTO_EN = List.of(
      "Who is this person in the picture?",
      "Can you tell me who this is?",
      "Do you remember this person?",
      "Look at this photo — who is it?",
      "Who do you see in this picture?");

  private static final List<String> PHOTO_TN = List.of(
      "شكون هذا في التصويرة؟",
      "تعرف شكون هذا؟",
      "أذكر شكون هذا؟",
      "شوف التصويرة — شكون هذا؟",
      "شكون هذا الي في الصورة؟");

  private static final List<String> PLACE_EN = List.of(
      "Can you name this place?",
      "Where do you think this is?",
      "Do you recognize this location?",
      "Look around — what place is this?",
      "Can you tell me where we are?");

  private static final List<String> PLACE_TN = List.of(
      "وين هذي البلاصة؟",
      "تعرف هذي البلاصة؟",
      "أذكر وين هذا المكان؟",
      "شوف — شنوا البلاصة هذي؟",
      "تنجم تقلي وين إحنا؟");

  private static final List<String> MOVIE_EN = List.of(
      "What movie is this?",
      "Can you name this film?",
      "Do you remember this movie?",
      "Which movie is shown here?",
      "Can you tell me the name of this movie?");

  private static final List<String> MOVIE_TN = List.of(
      "شنوا اسم هالفيلم؟",
      "تذكر هالفيلم؟",
      "شنوا هالفيلم؟",
      "تعرف هالفيلم — شنوا اسمو؟",
      "قلي شنوا اسم الفيلم هذا؟");

  private final Random random = new Random();

  public AudioTranslationService(WebClient.Builder webClientBuilder) {
    this.webClient = webClientBuilder.build();
  }

  // ── Public orchestrator ──────────────────────────────────────────────────

  /**
   * Generate TTS audio for a game question/prompt.
   * For QUESTION type: translates the text then generates speech.
   * For PHOTO/PLACE/MOVIE: picks a fixed phrase in the target language, no
   * translation needed.
   */
  public byte[] generateQuestionAudio(AudioGenerateRequest request) {
    String textToSpeak = buildPromptForGameType(
        request.getGameType(),
        request.getOriginalText(),
        request.getTargetLanguageCode(),
        request.getPatientName(),
        request.getPatientGender());

    log.info("[TTS] Text to speak (lang={}): {}", request.getTargetLanguageCode(), textToSpeak);

    // Pick voice based on language: explicit voiceId in request overrides the
    // default
    String voiceId;
    if (request.getVoiceId() != null && !request.getVoiceId().isBlank()) {
      voiceId = request.getVoiceId();
    } else {
      voiceId = "tn".equalsIgnoreCase(request.getTargetLanguageCode()) ? voiceIdTn : voiceIdEn;
    }

    return generateSpeech(textToSpeak, voiceId);
  }

  // ── Prompt builder ───────────────────────────────────────────────────────

  public String buildPromptForGameType(String gameType, String originalText, String targetLang,
      String patientName, String patientGender) {
    String prompt;

    if ("QUESTION".equalsIgnoreCase(gameType)) {
      // Translate the question text to the target language
      if ("tn".equalsIgnoreCase(targetLang)) {
        // Translate to Arabic for Tunisian (Google doesn't have Tunisian dialect
        // natively)
        prompt = translateText(originalText, "ar");
      } else {
        // Already English or translate to English
        prompt = "en".equalsIgnoreCase(targetLang) ? originalText : translateText(originalText, targetLang);
      }
    } else {
      // For PHOTO, PLACE, MOVIE — pick a random fixed phrase
      boolean isTunisian = "tn".equalsIgnoreCase(targetLang);
      List<String> variants = getVariantsForType(gameType, isTunisian);
      prompt = variants.get(random.nextInt(variants.size()));
    }

    // 30% chance to prepend patient name with gendered prefix
    prompt = maybeAddPatientName(prompt, patientName, patientGender, targetLang);

    return prompt;
  }

  private List<String> getVariantsForType(String gameType, boolean isTunisian) {
    return switch (gameType.toUpperCase()) {
      case "PHOTO" -> isTunisian ? PHOTO_TN : PHOTO_EN;
      case "PLACE" -> isTunisian ? PLACE_TN : PLACE_EN;
      case "MOVIE" -> isTunisian ? MOVIE_TN : MOVIE_EN;
      default -> isTunisian
          ? List.of("شنحكيلك حاجة؟")
          : List.of("Let me ask you something.");
    };
  }

  private String maybeAddPatientName(String prompt, String patientName, String patientGender, String targetLang) {
    if (patientName == null || patientName.isBlank())
      return prompt;
    if (random.nextDouble() >= 0.3)
      return prompt;

    boolean isTunisian = "tn".equalsIgnoreCase(targetLang);
    boolean isMale = !"female".equalsIgnoreCase(patientGender);

    if (isTunisian) {
      String prefix = isMale ? "بابا " : "ماما ";
      return prefix + patientName + "، " + prompt;
    } else {
      return patientName + ", " + prompt;
    }
  }

  // ── Google Cloud Translation API (Basic v2 with API key) ─────────────────

  public String translateText(String text, String targetLanguageCode) {
    if (text == null || text.isBlank())
      return text;

    try {
      String url = "https://translation.googleapis.com/language/translate/v2?key=" + googleTranslateApiKey;

      Map<String, Object> body = Map.of(
          "q", text,
          "target", targetLanguageCode,
          "format", "text");

      @SuppressWarnings("unchecked")
      Map<String, Object> response = webClient.post()
          .uri(url)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(body)
          .retrieve()
          .bodyToMono(Map.class)
          .timeout(Duration.ofSeconds(15))
          .block();

      if (response != null) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        if (data != null) {
          @SuppressWarnings("unchecked")
          List<Map<String, String>> translations = (List<Map<String, String>>) data.get("translations");
          if (translations != null && !translations.isEmpty()) {
            String translated = translations.get(0).get("translatedText");
            log.info("[Translate] '{}' → '{}' (target={})", text, translated, targetLanguageCode);
            return translated;
          }
        }
      }

      log.warn("[Translate] No translation returned, using original text");
      return text;
    } catch (Exception e) {
      log.error("[Translate] Translation failed, using original text: {}", e.getMessage());
      return text;
    }
  }

  // ── ElevenLabs TTS API ───────────────────────────────────────────────────

  public byte[] generateSpeech(String text, String voiceId) {
    String url = "https://api.elevenlabs.io/v1/text-to-speech/" + voiceId;

    Map<String, Object> body = Map.of(
        "text", text,
        "model_id", modelId,
        "voice_settings", Map.of(
            "stability", 0.5,
            "similarity_boost", 0.75));

    try {
      byte[] audioBytes = webClient.post()
          .uri(url)
          .header("xi-api-key", elevenLabsApiKey)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.valueOf("audio/mpeg"))
          .bodyValue(body)
          .retrieve()
          .bodyToMono(byte[].class)
          .timeout(Duration.ofSeconds(30))
          .block();

      if (audioBytes != null) {
        log.info("[ElevenLabs] Generated {} bytes of audio for voice '{}'", audioBytes.length, voiceId);
        return audioBytes;
      }

      throw new RuntimeException("ElevenLabs returned no audio data");
    } catch (Exception e) {
      log.error("[ElevenLabs] Speech generation failed: {}", e.getMessage());
      throw new RuntimeException("Failed to generate speech audio: " + e.getMessage(), e);
    }
  }
}
