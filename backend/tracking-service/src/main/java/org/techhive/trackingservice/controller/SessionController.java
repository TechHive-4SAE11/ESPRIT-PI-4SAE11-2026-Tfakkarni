package org.techhive.trackingservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.trackingservice.dto.SessionRequestDTO;
import org.techhive.trackingservice.dto.SessionResponseDTO;
import org.techhive.trackingservice.entity.Session;
import org.techhive.trackingservice.service.SessionService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    public ResponseEntity<SessionResponseDTO> createSession(@RequestBody SessionRequestDTO requestDTO) {
        try {
            Session session = new Session();
            session.setSessionDate(requestDTO.getSessionDate());
            session.setNotes(requestDTO.getNotes());
            
            Session saved = sessionService.createSessionForMedicalFolder(requestDTO.getMedicalFolderId(), session);
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(saved));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<SessionResponseDTO>> getAllSessions() {
        List<Session> sessions = sessionService.getAllSessions();
        List<SessionResponseDTO> responseDTOs = sessions.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponseDTO> getSessionById(@PathVariable Long id) {
        return sessionService.getSessionById(id)
                .map(session -> ResponseEntity.ok(toResponseDTO(session)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/medical-folder/{medicalFolderId}")
    public ResponseEntity<List<SessionResponseDTO>> getSessionsByMedicalFolder(@PathVariable Long medicalFolderId) {
        List<Session> sessions = sessionService.getSessionsByMedicalFolder(medicalFolderId);
        List<SessionResponseDTO> responseDTOs = sessions.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/medical-folder/{medicalFolderId}/no-prescription")
    public ResponseEntity<List<SessionResponseDTO>> getSessionsWithoutPrescriptions(@PathVariable Long medicalFolderId) {
        List<Session> sessions = sessionService.getSessionsWithoutPrescriptions(medicalFolderId);
        List<SessionResponseDTO> responseDTOs = sessions.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/medical-folder/{medicalFolderId}/no-care-plan")
    public ResponseEntity<List<SessionResponseDTO>> getSessionsWithoutCarePlans(@PathVariable Long medicalFolderId) {
        List<Session> sessions = sessionService.getSessionsWithoutCarePlans(medicalFolderId);
        List<SessionResponseDTO> responseDTOs = sessions.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<SessionResponseDTO>> getSessionsByPatient(@PathVariable String patientId) {
        // Find all sessions for any folder belonging to this patient
        List<Session> sessions = sessionService.getAllSessions().stream()
                .filter(s -> s.getMedicalFolder().getIdPatient().equals(patientId))
                .collect(Collectors.toList());
        List<SessionResponseDTO> responseDTOs = sessions.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SessionResponseDTO> updateSession(
            @PathVariable Long id,
            @RequestBody SessionRequestDTO requestDTO) {
        Session session = new Session();
        session.setSessionDate(requestDTO.getSessionDate());
        session.setNotes(requestDTO.getNotes());
        
        try {
            Session updated = sessionService.updateSession(id, session);
            return ResponseEntity.ok(toResponseDTO(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id) {
        sessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }

    private SessionResponseDTO toResponseDTO(Session session) {
        return new SessionResponseDTO(
                session.getId(),
                session.getMedicalFolder().getId(),
                session.getSessionDate(),
                session.getNotes(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
