package org.techhive.gameservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.techhive.gameservice.dto.AudioGenerateRequest;
import org.techhive.gameservice.service.AudioTranslationService;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameAudioControllerTest {

  private final AudioTranslationService audioTranslationService = mock(AudioTranslationService.class);
  private final GameAudioController controller = new GameAudioController(audioTranslationService);

  @Test
  void generateQuestionAudioRejectsMissingRequiredFields() {
    AudioGenerateRequest missingLanguage = AudioGenerateRequest.builder()
        .gameType("QUESTION")
        .originalText("Who are you?")
        .build();

    ResponseEntity<?> languageResponse = controller.generateQuestionAudio(missingLanguage);

    assertEquals(400, languageResponse.getStatusCode().value());
    assertBodyContains(languageResponse, "targetLanguageCode");

    AudioGenerateRequest missingType = AudioGenerateRequest.builder()
        .targetLanguageCode("en")
        .originalText("Who are you?")
        .build();

    ResponseEntity<?> typeResponse = controller.generateQuestionAudio(missingType);

    assertEquals(400, typeResponse.getStatusCode().value());
    assertBodyContains(typeResponse, "gameType");

    AudioGenerateRequest missingQuestionText = AudioGenerateRequest.builder()
        .targetLanguageCode("en")
        .gameType("QUESTION")
        .originalText(" ")
        .build();

    ResponseEntity<?> textResponse = controller.generateQuestionAudio(missingQuestionText);

    assertEquals(400, textResponse.getStatusCode().value());
    assertBodyContains(textResponse, "originalText");
    verifyNoInteractions(audioTranslationService);
  }

  @Test
  void generateQuestionAudioReturnsAudioBytesAndHeaders() {
    AudioGenerateRequest request = AudioGenerateRequest.builder()
        .targetLanguageCode("en")
        .gameType("PHOTO")
        .build();
    byte[] audio = new byte[] {1, 2, 3};
    when(audioTranslationService.generateQuestionAudio(request)).thenReturn(audio);

    ResponseEntity<?> response = controller.generateQuestionAudio(request);

    assertEquals(200, response.getStatusCode().value());
    assertArrayEquals(audio, (byte[]) response.getBody());
    assertEquals("audio/mpeg", response.getHeaders().getContentType().toString());
    assertEquals(3, response.getHeaders().getContentLength());
    assertEquals("no-cache", response.getHeaders().getFirst("Cache-Control"));
  }

  @Test
  void generateQuestionAudioReturnsBadGatewayWhenServiceFails() {
    AudioGenerateRequest request = AudioGenerateRequest.builder()
        .targetLanguageCode("en")
        .gameType("PHOTO")
        .build();
    when(audioTranslationService.generateQuestionAudio(request)).thenThrow(new RuntimeException("tts down"));

    ResponseEntity<?> response = controller.generateQuestionAudio(request);

    assertEquals(502, response.getStatusCode().value());
    assertBodyContains(response, "tts down");
  }

  @Test
  void translateOnlyValidatesRequestTranslatesTunisianAsArabicAndHandlesFailure() {
    ResponseEntity<?> missingText = controller.translateOnly(AudioGenerateRequest.builder()
        .targetLanguageCode("en")
        .originalText(" ")
        .build());
    assertEquals(400, missingText.getStatusCode().value());
    assertBodyContains(missingText, "originalText");

    ResponseEntity<?> missingLanguage = controller.translateOnly(AudioGenerateRequest.builder()
        .originalText("Bonjour")
        .targetLanguageCode(" ")
        .build());
    assertEquals(400, missingLanguage.getStatusCode().value());
    assertBodyContains(missingLanguage, "targetLanguageCode");

    AudioGenerateRequest tunisianRequest = AudioGenerateRequest.builder()
        .originalText("Hello")
        .targetLanguageCode("tn")
        .build();
    when(audioTranslationService.translateText("Hello", "ar")).thenReturn("مرحبا");

    ResponseEntity<?> translated = controller.translateOnly(tunisianRequest);

    assertEquals(200, translated.getStatusCode().value());
    assertEquals("مرحبا", ((Map<?, ?>) translated.getBody()).get("translatedText"));

    AudioGenerateRequest failingRequest = AudioGenerateRequest.builder()
        .originalText("Hello")
        .targetLanguageCode("fr")
        .build();
    when(audioTranslationService.translateText("Hello", "fr")).thenThrow(new RuntimeException("translate down"));

    ResponseEntity<?> failed = controller.translateOnly(failingRequest);

    assertEquals(502, failed.getStatusCode().value());
    assertBodyContains(failed, "translate down");
  }

  private void assertBodyContains(ResponseEntity<?> response, String expected) {
    assertTrue(response.getBody() instanceof Map<?, ?>);
    assertTrue(((Map<?, ?>) response.getBody()).values().stream()
        .map(String::valueOf)
        .anyMatch(value -> value.contains(expected)),
        "Expected response body to contain: " + expected + " but was " + response.getBody());
  }
}
