package org.techhive.trackingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.techhive.trackingservice.dto.*;
import org.techhive.trackingservice.entity.MedicalMeeting;
import org.techhive.trackingservice.entity.MeetingStatus;
import org.techhive.trackingservice.repository.MedicalMeetingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MeetingService {

    private final MedicalMeetingRepository meetingRepository;
    private final DailyRoomService dailyRoomService;
    private final MeetingSummaryService meetingSummaryService;
    private final MeetingPdfService meetingPdfService;

    /**
     * Create a new meeting with a Daily.co room.
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public MeetingResponse createMeeting(CreateMeetingRequest request, String patientName, String doctorName) {
        try {
            String roomName = "tfakk-" + UUID.randomUUID().toString().substring(0, 8);

            Map<String, Object> roomData = dailyRoomService.createRoom(roomName);

            String roomUrl    = roomData.get("url") != null ? roomData.get("url").toString() : "";
            String dailyRoomId = roomData.get("id") != null ? roomData.get("id").toString() : "";

            MedicalMeeting meeting = MedicalMeeting.builder()
                    .roomName(roomName)
                    .roomUrl(roomUrl)
                    .dailyRoomId(dailyRoomId)
                    .doctorKeycloakId(request.getDoctorKeycloakId())
                    .patientKeycloakId(request.getPatientKeycloakId())
                    .patientName(patientName)
                    .doctorName(doctorName)
                    .status(MeetingStatus.SCHEDULED)
                    .scheduledAt(request.getScheduledAt() != null ? request.getScheduledAt() : LocalDateTime.now())
                    .build();

            meeting = meetingRepository.save(meeting);
            log.info("Meeting created: id={}, room={}, patient={}", meeting.getId(), roomName, patientName);

            return toResponse(meeting);

        } catch (Exception e) {
            log.error("Failed to create meeting: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de la création de la réunion: " + e.getMessage(), e);
        }
    }

    /**
     * Get a meeting token for a participant and activate the meeting if needed.
     */
    @Transactional
    public Map<String, String> getMeetingToken(Long meetingId, String keycloakId, String userName) {
        try {
            MedicalMeeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new RuntimeException("Réunion introuvable: " + meetingId));

            boolean isDoctor  = keycloakId.equals(meeting.getDoctorKeycloakId());
            boolean isPatient = keycloakId.equals(meeting.getPatientKeycloakId());
            if (!isDoctor && !isPatient && meeting.getHelperKeycloakId() == null) {
                meeting.setHelperKeycloakId(keycloakId);
                meetingRepository.save(meeting);
                log.info("Meeting {} - helper registered: {}", meetingId, keycloakId);
            }

            String token = dailyRoomService.createMeetingToken(
                    meeting.getRoomName(), keycloakId, userName, isDoctor);

            if (meeting.getStatus() == MeetingStatus.SCHEDULED) {
                meeting.setStatus(MeetingStatus.ACTIVE);
                meeting.setStartedAt(LocalDateTime.now());
                meetingRepository.save(meeting);
            }

            Map<String, String> result = new LinkedHashMap<>();
            result.put("token",    token);
            result.put("roomUrl",  meeting.getRoomUrl());
            result.put("roomName", meeting.getRoomName());
            return result;

        } catch (Exception e) {
            log.error("Failed to get meeting token: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de la récupération du token: " + e.getMessage(), e);
        }
    }

    /**
     * Save live transcript + optional Groq partial summary.
     */
    @Transactional
    public PartialSummaryResponse saveTranscript(Long meetingId, String transcript,
                                                  boolean requestPartialSummary,
                                                  String segmentLabel) {
        MedicalMeeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Réunion introuvable: " + meetingId));

        meeting.setTranscript(transcript);

        String miniSummary    = null;
        String updatedSummaries = meeting.getTranscriptSummaries();

        if (requestPartialSummary && transcript != null && !transcript.trim().isEmpty()) {
            miniSummary = meetingSummaryService.generatePartialSummary(
                    transcript, segmentLabel,
                    meeting.getPatientName(), meeting.getDoctorName());

            String entry = "{\"label\":\"" + escJson(segmentLabel)
                    + "\",\"summary\":\"" + escJson(miniSummary) + "\"}";
            updatedSummaries = (updatedSummaries == null || updatedSummaries.isBlank())
                    ? "[" + entry + "]"
                    : updatedSummaries.substring(0, updatedSummaries.lastIndexOf(']')) + "," + entry + "]";
            meeting.setTranscriptSummaries(updatedSummaries);
        }

        meetingRepository.save(meeting);
        log.debug("Transcript saved for meeting {} ({} chars)", meetingId,
                transcript == null ? 0 : transcript.length());

        return PartialSummaryResponse.builder()
                .meetingId(meetingId)
                .segmentLabel(segmentLabel)
                .summary(miniSummary)
                .transcriptSummaries(updatedSummaries)
                .build();
    }

    private String escJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String building(String notes, String transcript) {
        boolean hasNotes      = notes != null && !notes.isBlank();
        boolean hasTranscript = transcript != null && !transcript.isBlank();
        if (hasNotes && hasTranscript)
            return "=== NOTES DU MÉDECIN ===\n" + notes + "\n\n=== TRANSCRIPTION EN DIRECT ===\n" + transcript;
        if (hasTranscript)
            return "=== TRANSCRIPTION EN DIRECT ===\n" + transcript;
        return notes;
    }

    /**
     * Update meeting notes (auto-save).
     */
    @Transactional
    public MeetingResponse updateNotes(Long meetingId, String notes) {
        MedicalMeeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Réunion introuvable: " + meetingId));
        meeting.setNotes(notes);
        return toResponse(meetingRepository.save(meeting));
    }

    /**
     * End meeting + generate Groq AI summary.
     */
    @Transactional
    public MeetingSummaryResponse endMeeting(Long meetingId, String finalNotes) {
        try {
            MedicalMeeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new RuntimeException("Réunion introuvable: " + meetingId));

            if (finalNotes != null && !finalNotes.trim().isEmpty()) meeting.setNotes(finalNotes);

            LocalDateTime start = meeting.getStartedAt() != null ? meeting.getStartedAt() : meeting.getScheduledAt();
            int duration = 0;
            if (start != null) {
                duration = (int) Duration.between(start, LocalDateTime.now()).toMinutes();
                if (duration < 1) duration = 1;
            }
            meeting.setDurationMinutes(duration);
            meeting.setStatus(MeetingStatus.ENDED);
            meeting.setEndedAt(LocalDateTime.now());
            meetingRepository.save(meeting);

            String summary;
            try {
                summary = meetingSummaryService.generateSummary(
                        building(meeting.getNotes(), meeting.getTranscript()),
                        meeting.getPatientName(), meeting.getDoctorName(), duration);
            } catch (Exception e) {
                log.error("AI summary failed for meeting {}: {}", meetingId, e.getMessage());
                summary = "Résumé non disponible — erreur lors de la génération AI.";
            }
            meeting.setAiSummary(summary);
            meetingRepository.save(meeting);

            try { dailyRoomService.deleteRoom(meeting.getRoomName()); }
            catch (Exception e) { log.warn("Daily room cleanup failed: {}", e.getMessage()); }

            return MeetingSummaryResponse.builder()
                    .meetingId(meeting.getId()).summary(summary).durationMinutes(duration).build();

        } catch (Exception e) {
            log.error("Failed to end meeting {}: {}", meetingId, e.getMessage());
            throw new RuntimeException("Erreur lors de la clôture: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteMeeting(Long meetingId) {
        MedicalMeeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Réunion introuvable: " + meetingId));
        try {
            if (meeting.getRoomName() != null && meeting.getStatus() != MeetingStatus.ENDED)
                dailyRoomService.deleteRoom(meeting.getRoomName());
        } catch (Exception e) { log.warn("Daily room cleanup failed: {}", e.getMessage()); }
        meetingRepository.deleteById(meetingId);
    }

    public byte[] generatePdf(Long meetingId) throws Exception {
        return meetingPdfService.generateMeetingPdf(
                meetingRepository.findById(meetingId)
                        .orElseThrow(() -> new RuntimeException("Réunion introuvable: " + meetingId)));
    }

    public Map<String, Object> testClaudeApi() {
        return meetingSummaryService.testClaudeConnection();
    }

    @Transactional
    public MeetingSummaryResponse regenerateSummary(Long meetingId) {
        MedicalMeeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Réunion introuvable: " + meetingId));
        String summary = meetingSummaryService.generateSummary(
                meeting.getNotes(), meeting.getPatientName(), meeting.getDoctorName(),
                meeting.getDurationMinutes() != null ? meeting.getDurationMinutes() : 1);
        meeting.setAiSummary(summary);
        meetingRepository.save(meeting);
        return MeetingSummaryResponse.builder()
                .meetingId(meeting.getId()).summary(summary)
                .durationMinutes(meeting.getDurationMinutes() != null ? meeting.getDurationMinutes() : 0).build();
    }

    public MeetingResponse getById(Long id) {
        return toResponse(meetingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réunion introuvable: " + id)));
    }

    public List<MeetingResponse> getMeetingsForDoctor(String doctorKeycloakId) {
        return meetingRepository.findByDoctorKeycloakIdOrderByCreatedAtDesc(doctorKeycloakId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<MeetingResponse> getMeetingsForPatient(String keycloakId) {
        return meetingRepository.findByPatientOrHelperKeycloakId(keycloakId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── DTO mapper ────────────────────────────────────────────────────────────

    private MeetingResponse toResponse(MedicalMeeting m) {
        return MeetingResponse.builder()
                .id(m.getId())
                .roomName(m.getRoomName())
                .roomUrl(m.getRoomUrl())
                .status(m.getStatus().name())
                .patientName(m.getPatientName())
                .doctorName(m.getDoctorName())
                // ── IDs nécessaires pour l'évaluation patient ──
                .doctorKeycloakId(m.getDoctorKeycloakId())
                .patientKeycloakId(m.getPatientKeycloakId())
                .notes(m.getNotes())
                .aiSummary(m.getAiSummary())
                .transcript(m.getTranscript())
                .transcriptSummaries(m.getTranscriptSummaries())
                .scheduledAt(m.getScheduledAt())
                .startedAt(m.getStartedAt())
                .endedAt(m.getEndedAt())
                .durationMinutes(m.getDurationMinutes())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
