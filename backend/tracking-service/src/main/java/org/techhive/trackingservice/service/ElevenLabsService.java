package org.techhive.trackingservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Service for converting audio to text via ElevenLabs Speech-to-Text API.
 * Docs: https://elevenlabs.io/docs/overview/capabilities/speech-to-text
 */
@Slf4j
@Service
public class ElevenLabsService {

    private static final String ELEVENLABS_STT_URL = "https://api.elevenlabs.io/v1/speech-to-text";

    @Value("${elevenlabs.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Transcribes the given audio file to text using ElevenLabs STT.
     *
     * @param audioFile the audio file (webm, mp4, mp3, wav, etc.)
     * @return the transcribed text
     */
    @SuppressWarnings("unchecked")
    public String transcribeAudio(MultipartFile audioFile) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("xi-api-key", apiKey);

            // Build the multipart body
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // Wrap the file bytes in a named resource
            ByteArrayResource fileResource = new ByteArrayResource(audioFile.getBytes()) {
                @Override
                public String getFilename() {
                    String original = audioFile.getOriginalFilename();
                    return (original != null && !original.isBlank()) ? original : "voice-note.webm";
                }
            };

            body.add("file", fileResource);
            body.add("model_id", "scribe_v1"); // ElevenLabs Scribe model

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    ELEVENLABS_STT_URL,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new RuntimeException("Réponse vide de l'API ElevenLabs");
            }

            String text = (String) responseBody.get("text");
            if (text == null || text.isBlank()) {
                throw new RuntimeException("Aucun texte transcrit dans la réponse");
            }

            log.info("Audio transcribed successfully ({} characters)", text.length());
            return text.trim();

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("ElevenLabs transcription error", e);
            throw new RuntimeException("Erreur lors de la transcription audio : " + e.getMessage());
        }
    }
}
