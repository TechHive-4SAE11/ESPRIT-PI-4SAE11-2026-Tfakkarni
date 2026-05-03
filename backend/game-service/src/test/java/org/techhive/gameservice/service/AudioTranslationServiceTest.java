package org.techhive.gameservice.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AudioTranslationServiceTest {

  @Test
  void buildPromptReturnsKnownVariantsForStaticGameTypes() {
    AudioTranslationService service = newService();

    assertContainsOneOf(service.buildPromptForGameType("PHOTO", null, "en", null, null),
        Set.of("Who is this person in the picture?", "Can you tell me who this is?", "Do you remember this person?",
            "Look at this photo — who is it?", "Who do you see in this picture?"));
    assertContainsOneOf(service.buildPromptForGameType("PLACE", null, "tn", null, null),
        Set.of("وين هذي البلاصة؟", "تعرف هذي البلاصة؟", "أذكر وين هذا المكان؟", "شوف — شنوا البلاصة هذي؟", "تنجم تقلي وين إحنا؟"));
    assertContainsOneOf(service.buildPromptForGameType("MOVIE", null, "en", null, null),
        Set.of("What movie is this?", "Can you name this film?", "Do you remember this movie?", "Which movie is shown here?",
            "Can you tell me the name of this movie?"));
  }

  @Test
  void buildPromptUsesOriginalQuestionForEnglishAndFallbackForUnknownType() {
    AudioTranslationService service = newService();

    assertEquals("What is your name?", service.buildPromptForGameType("QUESTION", "What is your name?", "en", null, null));
    assertEquals("Let me ask you something.", service.buildPromptForGameType("UNKNOWN", null, "en", null, null));
    assertEquals("شنحكيلك حاجة؟", service.buildPromptForGameType("UNKNOWN", null, "tn", null, null));
  }

  @Test
  void translateTextReturnsOriginalWhenBlankOrRemoteFails() throws Exception {
    AudioTranslationService service = newService();
    setField(service, "googleTranslateApiKey", "bad-key");

    assertNull(service.translateText(null, "fr"));
    assertEquals("", service.translateText("", "fr"));
    assertEquals("hello", service.translateText("hello", "fr"));
  }

  private AudioTranslationService newService() {
    return new AudioTranslationService(WebClient.builder());
  }

  private void assertContainsOneOf(String actual, Set<String> allowed) {
    assertTrue(allowed.contains(actual), "Unexpected prompt: " + actual);
  }

  private void setField(Object target, String name, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }
}
