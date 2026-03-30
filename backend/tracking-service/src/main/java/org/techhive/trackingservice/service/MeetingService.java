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

            // Generate AI summary (non-blocking for meeting end)
            String summary;
            try {
                summary = meetingSummaryService.generateSummary(
                        meeting.getNotes(),
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
                .scheduledAt(m.getScheduledAt())
                .startedAt(m.getStartedAt())
                .endedAt(m.getEndedAt())
                .durationMinutes(m.getDurationMinutes())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
