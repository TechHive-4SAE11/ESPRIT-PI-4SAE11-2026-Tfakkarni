package org.techhive.medicalservice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.medicalservice.client.GameServiceClient;
import org.techhive.medicalservice.dto.PatientBadgeDto;
import org.techhive.medicalservice.entity.PatientBadge;
import org.techhive.medicalservice.repository.PatientBadgeRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientBadgeServiceImplTest {

    @Mock
    private PatientBadgeRepository badgeRepository;

    @Mock
    private GameServiceClient gameServiceClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private PatientBadgeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PatientBadgeServiceImpl(badgeRepository, gameServiceClient, objectMapper);
    }

    @Test
    void getBadgesForPatientMapsRepositoryEntitiesToDtos() {
        PatientBadge badge = PatientBadge.builder()
                .id(7L)
                .patientId("patient-a")
                .badgeCode("MEMORY_STAR")
                .badgeTitle("Memory Star")
                .description("Perfect score")
                .awardedAt(LocalDateTime.of(2026, 5, 3, 10, 0))
                .sourceGameType("MEMORY")
                .sourceAttemptId(99L)
                .build();
        when(badgeRepository.findByPatientIdOrderByAwardedAtDesc("patient-a")).thenReturn(List.of(badge));

        List<PatientBadgeDto> badges = service.getBadgesForPatient("patient-a");

        assertEquals(1, badges.size());
        assertEquals(7L, badges.get(0).getId());
        assertEquals("patient-a", badges.get(0).getPatientId());
        assertEquals("MEMORY_STAR", badges.get(0).getBadgeCode());
        assertEquals("Memory Star", badges.get(0).getBadgeTitle());
        assertEquals("Perfect score", badges.get(0).getDescription());
        assertEquals("MEMORY", badges.get(0).getSourceGameType());
        assertEquals(99L, badges.get(0).getSourceAttemptId());
    }

    @Test
    void evaluateAndAwardBadgesReturnsEmptyWhenCleanupOrClientOrAnalyticsAreUnavailable() throws Exception {
        doThrow(new RuntimeException("cleanup failed")).when(badgeRepository).cleanupDuplicates();
        when(gameServiceClient.getPatientAnalytics("patient-a")).thenThrow(new RuntimeException("game down"));

        assertTrue(service.evaluateAndAwardBadges("patient-a").isEmpty());

        reset(badgeRepository, gameServiceClient);
        when(gameServiceClient.getPatientAnalytics("patient-a")).thenReturn(null);
        assertTrue(service.evaluateAndAwardBadges("patient-a").isEmpty());

        reset(badgeRepository, gameServiceClient);
        when(gameServiceClient.getPatientAnalytics("patient-a")).thenReturn(objectMapper.readTree("{\"other\":[]}"));
        assertTrue(service.evaluateAndAwardBadges("patient-a").isEmpty());

        reset(badgeRepository, gameServiceClient);
        when(gameServiceClient.getPatientAnalytics("patient-a")).thenReturn(objectMapper.readTree("{\"scoreHistory\":[]}"));
        assertTrue(service.evaluateAndAwardBadges("patient-a").isEmpty());

        verify(badgeRepository, never()).saveAll(anyList());
    }

    @Test
    void evaluateAndAwardBadgesAwardsEveryRuleWhenAttemptsQualify() throws Exception {
        when(gameServiceClient.getPatientAnalytics("patient-a")).thenReturn(analyticsWithQualifyingAttempts());
        when(badgeRepository.existsByPatientIdAndBadgeCode("patient-a", "FIRST_GAME")).thenReturn(false);
        when(badgeRepository.existsByPatientIdAndBadgeCode("patient-a", "MEMORY_STAR")).thenReturn(false);
        when(badgeRepository.existsByPatientIdAndBadgeCode("patient-a", "THREE_DAY_STREAK")).thenReturn(false);
        when(badgeRepository.existsByPatientIdAndBadgeCode("patient-a", "IMPROVEMENT_BADGE")).thenReturn(false);
        when(badgeRepository.existsByPatientIdAndBadgeCode("patient-a", "FAST_RECALL")).thenReturn(false);
        when(badgeRepository.existsByPatientIdAndBadgeCode("patient-a", "FOCUS_CHAMPION")).thenReturn(false);
        when(badgeRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<PatientBadgeDto> awarded = service.evaluateAndAwardBadges("patient-a");

        assertEquals(6, awarded.size());
        assertTrue(awarded.stream().anyMatch(b -> b.getBadgeCode().equals("FIRST_GAME")));
        assertTrue(awarded.stream().anyMatch(b -> b.getBadgeCode().equals("MEMORY_STAR") && b.getSourceAttemptId().equals(1L)));
        assertTrue(awarded.stream().anyMatch(b -> b.getBadgeCode().equals("THREE_DAY_STREAK") && b.getSourceAttemptId() == null));
        assertTrue(awarded.stream().anyMatch(b -> b.getBadgeCode().equals("IMPROVEMENT_BADGE") && b.getSourceAttemptId().equals(4L)));
        assertTrue(awarded.stream().anyMatch(b -> b.getBadgeCode().equals("FAST_RECALL") && b.getSourceAttemptId().equals(1L)));
        assertTrue(awarded.stream().anyMatch(b -> b.getBadgeCode().equals("FOCUS_CHAMPION") && b.getSourceAttemptId().equals(1L)));

        ArgumentCaptor<List<PatientBadge>> saved = ArgumentCaptor.forClass(List.class);
        verify(badgeRepository).saveAll(saved.capture());
        assertEquals(6, saved.getValue().size());
    }

    @Test
    void evaluateAndAwardBadgesSkipsExistingBadgesAndIgnoresMalformedDatesOrNonQualifyingAttempts() throws Exception {
        JsonNode analytics = objectMapper.readTree("""
                {
                  "scoreHistory": [
                    {"attemptId":0,"gameType":"QUIZ","gameTitle":"Bad attempt","score":0,"totalQuestions":10,"percentage":30,"durationSeconds":null,"completedAt":"bad-date"},
                    {"attemptId":5,"gameType":"QUIZ","gameTitle":"Average","score":6,"totalQuestions":10,"percentage":60,"durationSeconds":90,"completedAt":"2026-05-01T09:00:00Z"},
                    {"attemptId":6,"gameType":"QUIZ","gameTitle":"Average","score":6,"totalQuestions":10,"percentage":60,"durationSeconds":90,"completedAt":"2026-05-03T09:00:00.000Z"},
                    {"attemptId":7,"gameType":"QUIZ","gameTitle":"Average","score":6,"totalQuestions":10,"percentage":60,"durationSeconds":90,"completedAt":"2026-05-07T09:00:00Z"}
                  ]
                }
                """);
        when(gameServiceClient.getPatientAnalytics("patient-b")).thenReturn(analytics);
        when(badgeRepository.existsByPatientIdAndBadgeCode("patient-b", "FIRST_GAME")).thenReturn(true);
        when(badgeRepository.existsByPatientIdAndBadgeCode("patient-b", "MEMORY_STAR")).thenReturn(true);
        when(badgeRepository.existsByPatientIdAndBadgeCode("patient-b", "THREE_DAY_STREAK")).thenReturn(false);
        when(badgeRepository.existsByPatientIdAndBadgeCode("patient-b", "IMPROVEMENT_BADGE")).thenReturn(false);
        when(badgeRepository.existsByPatientIdAndBadgeCode("patient-b", "FAST_RECALL")).thenReturn(true);
        when(badgeRepository.existsByPatientIdAndBadgeCode("patient-b", "FOCUS_CHAMPION")).thenReturn(true);

        List<PatientBadgeDto> awarded = service.evaluateAndAwardBadges("patient-b");

        assertTrue(awarded.isEmpty());
        verify(badgeRepository, never()).saveAll(anyList());
    }

    private JsonNode analyticsWithQualifyingAttempts() throws Exception {
        return objectMapper.readTree("""
                {
                  "scoreHistory": [
                    {"attemptId":1,"gameType":"MEMORY","gameTitle":"Memory A","score":10,"totalQuestions":10,"percentage":100,"durationSeconds":45,"completedAt":"2026-05-01T09:00:00Z"},
                    {"attemptId":2,"gameType":"MEMORY","gameTitle":"Memory B","score":5,"totalQuestions":10,"percentage":50,"durationSeconds":120,"completedAt":"2026-05-02T09:00:00Z"},
                    {"attemptId":3,"gameType":"MEMORY","gameTitle":"Memory C","score":6,"totalQuestions":10,"percentage":60,"durationSeconds":90,"completedAt":"2026-05-03T09:00:00.000Z"},
                    {"attemptId":4,"gameType":"MEMORY","gameTitle":"Memory D","score":9,"totalQuestions":10,"percentage":90,"durationSeconds":75,"completedAt":"2026-05-04T09:00:00Z"}
                  ]
                }
                """);
    }
}
