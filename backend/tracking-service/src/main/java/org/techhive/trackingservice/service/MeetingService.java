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

            String roomUrl = roomData.get("url") != null ? roomData.get("url").toString() : "";
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
     * Accepts doctor, patient, OR helper keycloakId.
     */
    @Transactional
    public Map<String, String> getMeetingToken(Long meetingId, String keycloakId, String userName) {
        try {
            MedicalMeeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new RuntimeException("Réunion introuvable: " + meetingId));

            // Save helperKeycloakId if this is not the doctor or patient
            boolean isDoctor = keycloakId.equals(meeting.getDoctorKeycloakId());
            boolean isPatient = keycloakId.equals(meeting.getPatientKeycloakId());
            if (!isDoctor && !isPatient && meeting.getHelperKeycloakId() == null) {
                meeting.setHelperKeycloakId(keycloakId);
                meetingRepository.save(meeting);
                log.info("Meeting {} - helper registered: {}", meetingId, keycloakId);
            }

            boolean isOwner = isDoctor;

            String token = dailyRoomService.createMeetingToken(
                    meeting.getRoomName(), keycloakId, userName, isOwner
            );

            // Activate meeting on first join
            if (meeting.getStatus() == MeetingStatus.SCHEDULED) {
                meeting.setStatus(MeetingStatus.ACTIVE);
                meeting.setStartedAt(LocalDateTime.now());
                meetingRepository.save(meeting);
                log.info("Meeting {} activated by user {}", meetingId, userName);
            }

            Map<String, String> result = new LinkedHashMap<>();
            result.put("token", token);
            result.put("roomUrl", meeting.getRoomUrl());
            result.put("roomName", meeting.getRoomName());
            return result;

        } catch (Exception e) {
            log.error("Failed to get meeting token for meeting {}: {}", meetingId, e.getMessage());
            throw new RuntimeException("Erreur lors de la récupération du token: " + e.getMessage(), e);
        }
    }

    /**
     * Save live transcript chunk (auto-save from frontend).
     * Optionally generates a Groq mini-summary for the current segment.
     */
    @Transactional
    public PartialSummaryResponse saveTranscript(Long meetingId, String transcript,
                                                  boolean requestPartialSummary,
                                                  String segmentLabel) {
        MedicalMeeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Réunion introuvable: " + meetingId));

        meeting.setTranscript(transcript);

        String miniSummary = null;
        String updatedSummaries = meeting.getTranscriptSummaries();

        if (requestPartialSummary && transcript != null && !transcript.trim().isEmpty()) {
            miniSummary = meetingSummaryService.generatePartialSummary(
                    transcript, segmentLabel,
                    meeting.getPatientName(), meeting.getDoctorName());

            // Append to JSON array stored as plain text
            String entry = "{\"label\":\"" + escJson(segmentLabel)
                    + "\",\"summary\":\"" + escJson(miniSummary) + "\"}";
            if (updatedSummaries == null || updatedSummaries.isBlank()) {
                updatedSummaries = "[" + entry + "]";
            } else {
                updatedSummaries = updatedSummaries.substring(0, updatedSummaries.lastIndexOf(']'))
                        + "," + entry + "]";
            }
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

    /** Escape double-quotes and backslashes for inline JSON embedding. */
    private String escJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    /**
     * Combine notes + transcript into a single text for the final AI summary.
     * If both exist, sections are clearly separated.
     */
    private String building(String notes, String transcript) {
        boolean hasNotes      = notes != null && !notes.isBlank();
        boolean hasTranscript = transcript != null && !transcript.isBlank();
        if (hasNotes && hasTranscript) {
            return "=== NOTES DU MÉDECIN ===\n" + notes
                 + "\n\n=== TRANSCRIPTION EN DIRECT ===\n" + transcript;
        } else if (hasTranscript) {
            return "=== TRANSCRIPTION EN DIRECT ===\n" + transcript;
        } else {
            return notes;
        }
    }

    /**
     * Update meeting notes (auto-save from frontend).
     */
    @Transactional
    public MeetingResponse updateNotes(Long meetingId, String notes) {
        try {
            MedicalMeeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new RuntimeException("Réunion introuvable: " + meetingId));

            meeting.setNotes(notes);
            meeting = meetingRepository.save(meeting);
            log.debug("Notes updated for meeting {}", meetingId);

            return toResponse(meeting);

        } catch (Exception e) {
            log.error("Failed to update notes for meeting {}: {}", meetingId, e.getMessage());
            throw new RuntimeException("Erreur lors de la mise à jour des notes: " + e.getMessage(), e);
        }
    }

    /**
     * End a meeting: compute duration, generate AI summary, cleanup room.
     */
    @Transactional
    public MeetingSummaryResponse endMeeting(Long meetingId, String finalNotes) {
        try {
            MedicalMeeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new RuntimeException("Réunion introuvable: " + meetingId));

            // Update notes if provided
            if (finalNotes != null && !finalNotes.trim().isEmpty()) {
                meeting.setNotes(finalNotes);
            }

            // Calculate duration
            LocalDateTime start = meeting.getStartedAt() != null ? meeting.getStartedAt() : meeting.getScheduledAt();
            int duration = 0;
            if (start != null) {
                duration = (int) Duration.between(start, LocalDateTime.now()).toMinutes();
                if (duration < 1) duration = 1;
            }
            meeting.setDurationMinutes(duration);

            // End the meeting first (so it's always marked as ended even if AI fails)
            meeting.setStatus(MeetingStatus.ENDED);
            meeting.setEndedAt(LocalDateTime.now());
            meetingRepository.save(meeting);

            // Generate AI summary (uses notes + transcript if available)
            String notesForSummary = building(meeting.getNotes(), meeting.getTranscript());
            String summary;
            try {
                summary = meetingSummaryService.generateSummary(
                        notesForSummary,
                        meeting.getPatientName(),
                        meeting.getDoctorName(),
                        duration
                );
            } catch (Exception aiEx) {
                log.error("AI summary generation failed for meeting {}: {}", meetingId, aiEx.getMessage());
                summary = "Résumé non disponible — erreur lors de la génération AI.";
            }
            meeting.setAiSummary(summary);
            meetingRepository.save(meeting);

            log.info("Meeting {} ended. Duration: {} min", meetingId, duration);

            // Cleanup Daily.co room (non-blocking)
            try {
                dailyRoomService.deleteRoom(meeting.getRoomName());
            } catch (Exception e) {
                log.warn("Non-blocking: failed to delete Daily room '{}': {}", meeting.getRoomName(), e.getMessage());
            }

            return MeetingSummaryResponse.builder()
                    .meetingId(meeting.getId())
                    .summary(summary)
                    .durationMinutes(duration)
                    .build();

        } catch (Exception e) {
            log.error("Failed to end meeting {}: {}", meetingId, e.getMessage());
            throw new RuntimeException("Erreur lors de la clôture de la réunion: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a meeting by ID (also tries to cleanup the Daily.co room).
     */
    @Transactional
    public void deleteMeeting(Long meetingId) {
        MedicalMeeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Réunion introuvable: " + meetingId));
        // Try to cleanup Daily.co room
        try {
            if (meeting.getRoomName() != null && meeting.getStatus() != MeetingStatus.ENDED) {
                dailyRoomService.deleteRoom(meeting.getRoomName());
            }
        } catch (Exception e) {
            log.warn("Could not delete Daily.co room '{}': {}", meeting.getRoomName(), e.getMessage());
        }
        meetingRepository.deleteById(meetingId);
        log.info("Meeting {} deleted", meetingId);
    }

    /**
     * Generate PDF bytes for a meeting.
     */
    public byte[] generatePdf(Long meetingId) throws Exception {
        MedicalMeeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Réunion introuvable: " + meetingId));
        return meetingPdfService.generateMeetingPdf(meeting);
    }

    /**
     * Delegate Claude API connection test to MeetingSummaryService.
     */
    public Map<String, Object> testClaudeApi() {
        return meetingSummaryService.testClaudeConnection();
    }

    /**
     * Regenerate AI summary for an already-ended meeting.
     * Useful when Claude API was unavailable during the original end call.
     */
    @Transactional
    public MeetingSummaryResponse regenerateSummary(Long meetingId) {
        MedicalMeeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Réunion introuvable: " + meetingId));

        String summary = meetingSummaryService.generateSummary(
                meeting.getNotes(),
                meeting.getPatientName(),
                meeting.getDoctorName(),
                meeting.getDurationMinutes() != null ? meeting.getDurationMinutes() : 1
        );
        meeting.setAiSummary(summary);
        meetingRepository.save(meeting);
        log.info("Summary regenerated for meeting {}", meetingId);

        return MeetingSummaryResponse.builder()
                .meetingId(meeting.getId())
                .summary(summary)
                .durationMinutes(meeting.getDurationMinutes() != null ? meeting.getDurationMinutes() : 0)
                .build();
    }

    /**
     * Get a meeting by ID.
     */
    public MeetingResponse getById(Long id) {
        MedicalMeeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réunion introuvable: " + id));
        return toResponse(meeting);
    }

    /**
     * Get all meetings for a doctor.
     */
    public List<MeetingResponse> getMeetingsForDoctor(String doctorKeycloakId) {
        return meetingRepository.findByDoctorKeycloakIdOrderByCreatedAtDesc(doctorKeycloakId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all meetings for a patient.
     */
    public List<MeetingResponse> getMeetingsForPatient(String keycloakId) {
        // Search by patientKeycloakId OR helperKeycloakId
        // so the helper (connected person) sees meetings created for their patient
        return meetingRepository.findByPatientOrHelperKeycloakId(keycloakId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Map entity to response DTO.
     */
    private MeetingResponse toResponse(MedicalMeeting m) {
        return MeetingResponse.builder()
                .id(m.getId())
                .roomName(m.getRoomName())
                .roomUrl(m.getRoomUrl())
                .status(m.getStatus().name())
                .patientName(m.getPatientName())
                .doctorName(m.getDoctorName())
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
