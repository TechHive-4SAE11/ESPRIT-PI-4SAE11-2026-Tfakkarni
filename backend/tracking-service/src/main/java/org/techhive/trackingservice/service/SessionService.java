package org.techhive.trackingservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.trackingservice.entity.Session;
import org.techhive.trackingservice.repository.SessionRepository;
import org.techhive.trackingservice.repository.MedicalFolderRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class SessionService {

    private final SessionRepository sessionRepository;
    private final MedicalFolderRepository medicalFolderRepository;

    public Session createSession(Session session) {
        return sessionRepository.save(session);
    }

    public Session createSessionForMedicalFolder(Long medicalFolderId, Session session) {
        return medicalFolderRepository.findById(medicalFolderId)
                .map(folder -> {
                    session.setMedicalFolder(folder);
                    return sessionRepository.save(session);
                })
                .orElseGet(() -> {
                    // If the folder is not found by ID, it might be that the ID passed is invalid 
                    // or we need a more robust lookup. For now, throw explicit error for the ID.
                    throw new RuntimeException("Medical Folder not found in tracking-service with id: " + medicalFolderId);
                });
    }

    @Transactional(readOnly = true)
    public List<Session> getAllSessions() {
        return sessionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Session> getSessionById(Long id) {
        return sessionRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Session> getSessionsByMedicalFolder(Long medicalFolderId) {
        return sessionRepository.findByMedicalFolderIdOrderBySessionDateDesc(medicalFolderId);
    }

    @Transactional(readOnly = true)
    public List<Session> getSessionsWithoutPrescriptions(Long medicalFolderId) {
        return sessionRepository.findByMedicalFolderIdAndPrescriptionsIsEmpty(medicalFolderId);
    }

    @Transactional(readOnly = true)
    public List<Session> getSessionsWithoutCarePlans(Long medicalFolderId) {
        return sessionRepository.findByMedicalFolderIdAndCarePlansIsEmpty(medicalFolderId);
    }

    public Session updateSession(Long id, Session session) {
        return sessionRepository.findById(id)
                .map(existing -> {
                    existing.setSessionDate(session.getSessionDate());
                    existing.setNotes(session.getNotes());
                    return sessionRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Session not found with id: " + id));
    }

    public void deleteSession(Long id) {
        sessionRepository.deleteById(id);
    }
}
