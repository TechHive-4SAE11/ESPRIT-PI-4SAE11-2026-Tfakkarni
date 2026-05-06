package org.techhive.trackingservice.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.techhive.trackingservice.dto.PatientAnswerDTO;
import org.techhive.trackingservice.dto.QuestionnaireSubmissionDTO;
import org.techhive.trackingservice.entity.MedicalMeeting;
import org.techhive.trackingservice.entity.MeetingStatus;
import org.techhive.trackingservice.entity.Medication;
import org.techhive.trackingservice.entity.Prescription;
import org.techhive.trackingservice.entity.Question;
import org.techhive.trackingservice.enums.MedicationStatus;
import org.techhive.trackingservice.repository.PatientAnswerRepository;
import org.techhive.trackingservice.repository.QuestionRepository;
import org.techhive.trackingservice.repository.QuestionnaireRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MeetingReportingCoverageTest {

    @Test
    void meetingPdfCoversAllOptionalSectionsAndStatusLabels() throws Exception {
        MeetingPdfService service = new MeetingPdfService();

        byte[] ended = service.generateMeetingPdf(meeting(MeetingStatus.ENDED));
        byte[] active = service.generateMeetingPdf(meeting(MeetingStatus.ACTIVE));
        byte[] scheduled = service.generateMeetingPdf(meeting(MeetingStatus.SCHEDULED));
        MedicalMeeting sparse = meeting(MeetingStatus.SCHEDULED);
        sparse.setNotes(" ");
        sparse.setTranscript(null);
        sparse.setTranscriptSummaries("[{\"label\":\"broken\"}]");
        sparse.setAiSummary("");
        sparse.setCreatedAt(null);
        sparse.setEndedAt(null);
        sparse.setDurationMinutes(null);

        assertThat(ended).hasSizeGreaterThan(1_000);
        assertThat(active).hasSizeGreaterThan(1_000);
        assertThat(scheduled).hasSizeGreaterThan(1_000);
        assertThat(service.generateMeetingPdf(sparse)).hasSizeGreaterThan(1_000);
    }

    @Test
    void prescriptionPdfCoversMedicationRowsSignatureFallbackAndNoSignatureOverload() throws Exception {
        PrescriptionPdfService service = new PrescriptionPdfService();
        Prescription prescription = new Prescription();
        prescription.setCreatedAt(LocalDateTime.of(2026, 5, 6, 9, 0));
        Medication withInstructions = medication("Donepezil", "5mg", "Daily", "After dinner");
        Medication withoutInstructions = medication("Memantine", "10mg", "Twice daily", null);
        prescription.setMedications(List.of(withInstructions, withoutInstructions));

        assertThat(service.generatePrescriptionPdf(prescription)).hasSizeGreaterThan(1_000);
        assertThat(service.generatePrescriptionPdf(prescription, new byte[]{1, 2, 3})).hasSizeGreaterThan(1_000);
        prescription.setMedications(null);
        assertThat(service.generatePrescriptionPdf(prescription, new byte[0])).hasSizeGreaterThan(1_000);
    }

    @Test
    void meetingSummaryCoversSuccessFallbacksAndPartialSummaryBranches() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        MeetingSummaryService service = new MeetingSummaryService(
                "abcdefghijklmnopqrstuvwxyz",
                "https://api.groq.com/openai/v1/chat/completions",
                "llama-test",
                restTemplate
        );
        Map<String, Object> body = Map.of("choices", List.of(Map.of("message", Map.of("content", "  Résumé OK  "))));

        when(restTemplate.exchange(eq("https://api.groq.com/openai/v1/chat/completions"), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(body))
                .thenReturn(ResponseEntity.ok(body))
                .thenReturn(ResponseEntity.ok(Map.of("choices", List.of(Map.of("message", Map.of())))))
                .thenThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "bad", null, "bad request".getBytes(), null))
                .thenThrow(HttpServerErrorException.create(HttpStatus.BAD_GATEWAY, "bad gateway", null, "server".getBytes(), null))
                .thenThrow(new ResourceAccessException("timeout"))
                .thenReturn(ResponseEntity.ok(body))
                .thenThrow(new IllegalStateException("unexpected"));

        assertThat(service.testClaudeConnection()).containsEntry("provider", "Groq (free)");
        assertThat(service.generateSummary("notes\nfollow up", "Nour", "Driss", 30)).isEqualTo("Résumé OK");
        assertThat(service.generateSummary("", "Nour", "Driss", 30)).contains("Aucune note");
        assertThat(service.generateSummary("notes", "Nour", "Driss", 30)).contains("## Résumé de la réunion");
        assertThat(service.generateSummary("notes", "Nour", "Driss", 30)).contains("## Résumé de la réunion");
        assertThat(service.generateSummary("notes", "Nour", "Driss", 30)).contains("## Résumé de la réunion");
        assertThat(service.generateSummary("notes", "Nour", "Driss", 30)).contains("## Résumé de la réunion");
        assertThat(service.generatePartialSummary("patient is stable", "0-5 min", "Nour", "Driss")).isEqualTo("Résumé OK");
        assertThat(service.generatePartialSummary("patient is stable", "0-5 min", "Nour", "Driss")).contains("indisponible");
        assertThat(service.generatePartialSummary(" ", "5-10 min", "Nour", "Driss")).contains("Aucune parole");
    }

    @Test
    void questionnaireRecommendationCoversGeminiJsonMarkdownFallbackAndSubmission() {
        QuestionnaireRepository questionnaireRepository = mock(QuestionnaireRepository.class);
        QuestionRepository questionRepository = mock(QuestionRepository.class);
        PatientAnswerRepository patientAnswerRepository = mock(PatientAnswerRepository.class);
        GeminiService geminiService = mock(GeminiService.class);
        QuestionnaireService service = new QuestionnaireService(questionnaireRepository, questionRepository, patientAnswerRepository, geminiService);

        Question sleep = Question.builder().id(1L).text("How is sleep?").build();
        Question exercise = Question.builder().id(2L).text("Physical exercise?").build();
        Question social = Question.builder().id(3L).text("Social friends?").build();
        Question diet = Question.builder().id(4L).text("Diet and nutrition?").build();
        when(questionRepository.findById(1L)).thenReturn(Optional.of(sleep));
        when(questionRepository.findById(2L)).thenReturn(Optional.of(exercise));
        when(questionRepository.findById(3L)).thenReturn(Optional.of(social));
        when(questionRepository.findById(4L)).thenReturn(Optional.of(diet));

        QuestionnaireSubmissionDTO submission = QuestionnaireSubmissionDTO.builder()
                .patientId(1L)
                .answers(List.of(
                        PatientAnswerDTO.builder().questionId(1L).answer("less than 6 hours").build(),
                        PatientAnswerDTO.builder().questionId(2L).answer("no exercise").build(),
                        PatientAnswerDTO.builder().questionId(3L).answer("rarely sees friends").build(),
                        PatientAnswerDTO.builder().questionId(4L).answer("balanced").build()
                ))
                .build();

        when(geminiService.generateRecommendation(submission.getAnswers()))
                .thenReturn("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"```json\\n[{\\\"activityType\\\":\\\"SLEEP\\\",\\\"activityName\\\":\\\"Sleep routine\\\",\\\"description\\\":\\\"Rest\\\",\\\"frequency\\\":\\\"Daily\\\",\\\"duration\\\":\\\"8 hours\\\"}]\\n```\"}]}}]}")
                .thenReturn("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"not-json\"}]}}]}")
                .thenReturn(null);

        assertThat(service.getAllQuestionnaires()).isEmpty();
        service.submitAnswers(submission);
        assertThat(service.recommendCarePlan(submission).getActivities()).hasSize(1);
        assertThat(service.recommendCarePlan(submission).getActivities()).hasSizeGreaterThanOrEqualTo(4);
        assertThat(service.recommendCarePlan(QuestionnaireSubmissionDTO.builder()
                .patientId(1L)
                .answers(List.of(PatientAnswerDTO.builder().questionId(99L).answer("ok").build()))
                .build()).getActivities()).hasSize(1);
    }

    private static MedicalMeeting meeting(MeetingStatus status) {
        return MedicalMeeting.builder()
                .id(42L)
                .patientName("Nour")
                .doctorName("Driss")
                .status(status)
                .notes("Patient reports improved sleep.")
                .transcript("Doctor: Bonjour\\nPatient: Je vais bien")
                .transcriptSummaries("[{\"label\":\"0-5 min\",\"summary\":\"Stable\\\\nNo concern\"},{\"label\":\"5-10 min\",\"summary\":\"Follow-up\"}]")
                .aiSummary("## Résumé\\nStable\\n## Suivi\\nContinue plan")
                .createdAt(LocalDateTime.of(2026, 5, 6, 9, 0))
                .endedAt(LocalDateTime.of(2026, 5, 6, 9, 30))
                .durationMinutes(30)
                .build();
    }

    private static Medication medication(String name, String dosage, String frequency, String instructions) {
        Medication medication = new Medication();
        medication.setMedicationName(name);
        medication.setDosage(dosage);
        medication.setFrequency(frequency);
        medication.setInstructions(instructions);
        medication.setStatus(MedicationStatus.ACTIVE);
        return medication;
    }
}
