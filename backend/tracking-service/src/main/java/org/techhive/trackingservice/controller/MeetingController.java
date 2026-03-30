package org.techhive.trackingservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.techhive.trackingservice.dto.*;
import org.techhive.trackingservice.service.MeetingService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meetings")
@CrossOrigin(origins = "*")
@Slf4j
public class MeetingController {

    private final MeetingService meetingService;
    private final RestTemplate lbRestTemplate;

    public MeetingController(
            MeetingService meetingService,
            @Qualifier("lbRestTemplate") RestTemplate lbRestTemplate) {
        this.meetingService = meetingService;
        this.lbRestTemplate = lbRestTemplate;
    }

    /**
     * POST /api/meetings — Create a new meeting
     */
    @PostMapping
    public ResponseEntity<MeetingResponse> createMeeting(@RequestBody CreateMeetingRequest request) {
        try {
            String patientName = fetchUserName(request.getPatientKeycloakId());
            String doctorName = fetchUserName(request.getDoctorKeycloakId());

            MeetingResponse response = meetingService.createMeeting(request, patientName, doctorName);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Error creating meeting: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/meetings/{id} — Get meeting details
     */
    @GetMapping("/{id}")
    public ResponseEntity<MeetingResponse> getMeeting(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(meetingService.getById(id));
        } catch (Exception e) {
            log.error("Error fetching meeting {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/meetings/{id}/token — Get meeting token for a participant
     */
    @GetMapping("/{id}/token")
    public ResponseEntity<Map<String, String>> getMeetingToken(
            @PathVariable Long id,
            @RequestParam String keycloakId,
            @RequestParam String userName) {
        try {
            Map<String, String> tokenData = meetingService.getMeetingToken(id, keycloakId, userName);
            return ResponseEntity.ok(tokenData);
        } catch (Exception e) {
            log.error("Error getting token for meeting {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PUT /api/meetings/{id}/notes — Update meeting notes (auto-save)
     */
    @PutMapping("/{id}/notes")
    public ResponseEntity<MeetingResponse> updateNotes(
            @PathVariable Long id,
            @RequestBody UpdateNotesRequest request) {
        try {
            return ResponseEntity.ok(meetingService.updateNotes(id, request.getNotes()));
        } catch (Exception e) {
            log.error("Error updating notes for meeting {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PUT /api/meetings/{id}/end — End meeting and generate AI summary
     */
    @PutMapping("/{id}/end")
    public ResponseEntity<MeetingSummaryResponse> endMeeting(
            @PathVariable Long id,
            @RequestBody(required = false) EndMeetingRequest request) {
        try {
            String notes = request != null ? request.getNotes() : null;
            return ResponseEntity.ok(meetingService.endMeeting(id, notes));
        } catch (Exception e) {
            log.error("Error ending meeting {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/meetings/doctor/{doctorKeycloakId} — Get all meetings for a doctor
     */
    @GetMapping("/doctor/{doctorKeycloakId}")
    public ResponseEntity<List<MeetingResponse>> getMeetingsForDoctor(@PathVariable String doctorKeycloakId) {
        try {
            return ResponseEntity.ok(meetingService.getMeetingsForDoctor(doctorKeycloakId));
        } catch (Exception e) {
            log.error("Error fetching meetings for doctor {}: {}", doctorKeycloakId, e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * GET /api/meetings/patient/{patientKeycloakId} — Get all meetings for a patient
     */
    @GetMapping("/patient/{patientKeycloakId}")
    public ResponseEntity<List<MeetingResponse>> getMeetingsForPatient(@PathVariable String patientKeycloakId) {
        try {
            return ResponseEntity.ok(meetingService.getMeetingsForPatient(patientKeycloakId));
        } catch (Exception e) {
            log.error("Error fetching meetings for patient {}: {}", patientKeycloakId, e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * Fetch user name from user-service via load-balanced RestTemplate.
     */
    @SuppressWarnings("unchecked")
    private String fetchUserName(String keycloakId) {
        try {
            ResponseEntity<Map> response = lbRestTemplate.getForEntity(
                    "http://user-service/api/users/keycloak/" + keycloakId,
                    Map.class
            );
            Map<String, Object> userData = response.getBody();
            if (userData != null) {
                String firstName = userData.getOrDefault("firstName", "").toString();
                String lastName = userData.getOrDefault("lastName", "").toString();
                String fullName = (firstName + " " + lastName).trim();
                return fullName.isEmpty() ? "Utilisateur" : fullName;
            }
            return "Utilisateur";
        } catch (Exception e) {
            log.warn("Could not fetch user name for keycloakId {}: {}", keycloakId, e.getMessage());
            return "Utilisateur";
        }
    }
}
