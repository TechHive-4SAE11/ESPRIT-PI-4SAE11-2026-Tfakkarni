package org.techhive.trackingservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.trackingservice.dto.CreateMeetingRequest;
import org.techhive.trackingservice.dto.MeetingResponse;
import org.techhive.trackingservice.dto.MeetingSummaryResponse;
import org.techhive.trackingservice.dto.PartialSummaryResponse;
import org.techhive.trackingservice.entity.MedicalMeeting;
import org.techhive.trackingservice.entity.MeetingStatus;
import org.techhive.trackingservice.repository.MedicalMeetingRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingServiceCoverageTest {

    @Mock MedicalMeetingRepository meetingRepository;
    @Mock DailyRoomService dailyRoomService;
    @Mock MeetingSummaryService meetingSummaryService;
    @Mock MeetingPdfService meetingPdfService;

    private MeetingService meetingService;

    @BeforeEach
    void setUp() {
        meetingService = new MeetingService(meetingRepository, dailyRoomService, meetingSummaryService, meetingPdfService);
    }

    @Test
    void createMeetingTokenTranscriptNotesAndQueries() {
        CreateMeetingRequest request = CreateMeetingRequest.builder()
                .doctorKeycloakId("doctor-kc")
                .patientKeycloakId("patient-kc")
                .scheduledAt(LocalDateTime.now().plusHours(1))
                .build();
        when(dailyRoomService.createRoom(any(String.class))).thenReturn(Map.of("url", "https://daily.test/room", "id", "daily-id"));
        when(meetingRepository.save(any(MedicalMeeting.class))).thenAnswer(inv -> {
            MedicalMeeting meeting = inv.getArgument(0);
            if (meeting.getId() == null) meeting.setId(10L);
            if (meeting.getCreatedAt() == null) meeting.setCreatedAt(LocalDateTime.now());
            return meeting;
        });

        MeetingResponse created = meetingService.createMeeting(request, "Nour Trabelsi", "Sarra Mansouri");

        assertThat(created.getId()).isEqualTo(10L);
        assertThat(created.getStatus()).isEqualTo("SCHEDULED");
        assertThat(created.getRoomUrl()).isEqualTo("https://daily.test/room");

        MedicalMeeting scheduled = meeting(11L, MeetingStatus.SCHEDULED);
        when(meetingRepository.findById(11L)).thenReturn(Optional.of(scheduled));
        when(dailyRoomService.createMeetingToken("room-11", "helper-kc", "Hela", false)).thenReturn("token-helper");
        Map<String, String> token = meetingService.getMeetingToken(11L, "helper-kc", "Hela");
        assertThat(token).containsEntry("token", "token-helper").containsEntry("roomName", "room-11");
        assertThat(scheduled.getHelperKeycloakId()).isEqualTo("helper-kc");
        assertThat(scheduled.getStatus()).isEqualTo(MeetingStatus.ACTIVE);
        assertThat(scheduled.getStartedAt()).isNotNull();

        when(meetingRepository.findById(12L)).thenReturn(Optional.of(meeting(12L, MeetingStatus.ACTIVE)));
        when(meetingSummaryService.generatePartialSummary("line1\n\"quoted\"", "0:00", "Patient 12", "Doctor 12"))
                .thenReturn("Résumé partiel");
        PartialSummaryResponse partial = meetingService.saveTranscript(12L, "line1\n\"quoted\"", true, "0:00");
        assertThat(partial.getSummary()).isEqualTo("Résumé partiel");
        assertThat(partial.getTranscriptSummaries()).contains("Résumé partiel").contains("0:00");

        MedicalMeeting noteMeeting = meeting(13L, MeetingStatus.ACTIVE);
        when(meetingRepository.findById(13L)).thenReturn(Optional.of(noteMeeting));
        assertThat(meetingService.updateNotes(13L, "Notes cliniques").getNotes()).isEqualTo("Notes cliniques");

        when(meetingRepository.findById(14L)).thenReturn(Optional.of(meeting(14L, MeetingStatus.ENDED)));
        assertThat(meetingService.getById(14L).getId()).isEqualTo(14L);
        when(meetingRepository.findByDoctorKeycloakIdOrderByCreatedAtDesc("doctor-kc")).thenReturn(List.of(meeting(15L, MeetingStatus.SCHEDULED)));
        when(meetingRepository.findByPatientOrHelperKeycloakId("patient-kc")).thenReturn(List.of(meeting(16L, MeetingStatus.ACTIVE)));
        assertThat(meetingService.getMeetingsForDoctor("doctor-kc")).extracting(MeetingResponse::getId).containsExactly(15L);
        assertThat(meetingService.getMeetingsForPatient("patient-kc")).extracting(MeetingResponse::getId).containsExactly(16L);
    }

    @Test
    void endRegeneratePdfDeleteAndErrorBranches() throws Exception {
        MedicalMeeting ending = meeting(20L, MeetingStatus.ACTIVE);
        ending.setStartedAt(LocalDateTime.now().minusMinutes(5));
        ending.setNotes("old notes");
        ending.setTranscript("transcript");
        when(meetingRepository.findById(20L)).thenReturn(Optional.of(ending));
        when(meetingSummaryService.generateSummary(any(String.class), eq("Patient 20"), eq("Doctor 20"), any(Integer.class))).thenReturn("Résumé final");
        when(meetingRepository.save(any(MedicalMeeting.class))).thenAnswer(inv -> inv.getArgument(0));

        MeetingSummaryResponse summary = meetingService.endMeeting(20L, "final notes");

        assertThat(summary.getSummary()).isEqualTo("Résumé final");
        assertThat(summary.getDurationMinutes()).isGreaterThanOrEqualTo(1);
        assertThat(ending.getStatus()).isEqualTo(MeetingStatus.ENDED);
        verify(dailyRoomService).deleteRoom("room-20");

        MedicalMeeting aiFail = meeting(21L, MeetingStatus.ACTIVE);
        aiFail.setScheduledAt(LocalDateTime.now().minusMinutes(1));
        when(meetingRepository.findById(21L)).thenReturn(Optional.of(aiFail));
        when(meetingSummaryService.generateSummary(nullable(String.class), eq("Patient 21"), eq("Doctor 21"), any(Integer.class))).thenThrow(new RuntimeException("AI down"));
        doThrow(new RuntimeException("cleanup down")).when(dailyRoomService).deleteRoom("room-21");
        assertThat(meetingService.endMeeting(21L, null).getSummary()).contains("Résumé non disponible");

        MedicalMeeting toDelete = meeting(22L, MeetingStatus.SCHEDULED);
        when(meetingRepository.findById(22L)).thenReturn(Optional.of(toDelete));
        meetingService.deleteMeeting(22L);
        verify(meetingRepository).deleteById(22L);

        MedicalMeeting pdfMeeting = meeting(23L, MeetingStatus.ENDED);
        when(meetingRepository.findById(23L)).thenReturn(Optional.of(pdfMeeting));
        when(meetingPdfService.generateMeetingPdf(pdfMeeting)).thenReturn(new byte[]{1, 2, 3});
        assertThat(meetingService.generatePdf(23L)).containsExactly(1, 2, 3);

        when(meetingSummaryService.testClaudeConnection()).thenReturn(Map.of("ok", true));
        assertThat(meetingService.testClaudeApi()).containsEntry("ok", true);

        MedicalMeeting regenerate = meeting(24L, MeetingStatus.ENDED);
        regenerate.setDurationMinutes(null);
        when(meetingRepository.findById(24L)).thenReturn(Optional.of(regenerate));
        when(meetingSummaryService.generateSummary(null, "Patient 24", "Doctor 24", 1)).thenReturn("Regenerated");
        assertThat(meetingService.regenerateSummary(24L).getSummary()).isEqualTo("Regenerated");

        when(meetingRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> meetingService.getMeetingToken(404L, "x", "x")).hasMessageContaining("Erreur lors de la récupération du token");
        assertThatThrownBy(() -> meetingService.endMeeting(404L, null)).hasMessageContaining("Erreur lors de la clôture");
        assertThatThrownBy(() -> meetingService.getById(404L)).hasMessageContaining("Réunion introuvable");
    }

    private MedicalMeeting meeting(Long id, MeetingStatus status) {
        return MedicalMeeting.builder()
                .id(id)
                .roomName("room-" + id)
                .roomUrl("https://daily.test/room-" + id)
                .dailyRoomId("daily-" + id)
                .doctorKeycloakId("doctor-kc")
                .patientKeycloakId("patient-kc")
                .patientName("Patient " + id)
                .doctorName("Doctor " + id)
                .status(status)
                .scheduledAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
