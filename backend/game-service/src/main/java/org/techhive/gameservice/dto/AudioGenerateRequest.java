package org.techhive.gameservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioGenerateRequest {

  /**
   * The text to speak. For QUESTION type this is the question text itself.
   * For PHOTO/PLACE/MOVIE types this can be null — the service will pick a fixed
   * opening phrase.
   */
  private String originalText;

  /**
   * Target language code: "en" for English, "tn" for Tunisian Arabic.
   */
  private String targetLanguageCode;

  /**
   * ElevenLabs voice ID. If null, the default voice from config is used.
   */
  private String voiceId;

  /**
   * The type of game data point: PHOTO, PLACE, MOVIE, or QUESTION.
   */
  private String gameType;

  /**
   * Patient's first name — used for personalized address (30% chance).
   */
  private String patientName;

  /**
   * Patient's gender: "male" or "female" — determines prefix ("بابا" vs "ماما").
   */
  private String patientGender;
}
