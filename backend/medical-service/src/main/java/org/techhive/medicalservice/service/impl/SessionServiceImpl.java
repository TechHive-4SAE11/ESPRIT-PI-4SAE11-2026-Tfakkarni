package org.techhive.medicalservice.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.medicalservice.dto.CreateSessionRequest;
import org.techhive.medicalservice.dto.SessionResponse;
import org.techhive.medicalservice.dto.UpdateSessionRequest;
import org.techhive.medicalservice.entity.MedicalFolder;
import org.techhive.medicalservice.entity.Session;
import org.techhive.medicalservice.exception.ResourceNotFoundException;
import org.techhive.medicalservice.mapper.SessionMapper;
import org.techhive.medicalservice.repository.MedicalFolderRepository;
import org.techhive.medicalservice.repository.SessionRepository;
import org.techhive.medicalservice.service.SessionService;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class SessionServiceImpl implements SessionService {

	@Autowired
	private SessionRepository sessionRepository;

	@Autowired
	private SessionMapper sessionMapper;

	@Autowired
	private MedicalFolderRepository medicalFolderRepository;

	@Override
	public SessionResponse createSession(CreateSessionRequest request) {
		log.debug("Creating new session with medicalFolderId: {}", request.getMedicalFolderId());
		
		// Load the medical folder
		MedicalFolder medicalFolder = medicalFolderRepository.findById(request.getMedicalFolderId())
				.orElseThrow(() -> new ResourceNotFoundException("Medical folder not found with id: " + request.getMedicalFolderId()));
		
		// Create session with loaded folder
		Session session = Session.builder()
				.medicalFolder(medicalFolder)
				.sessionDate(request.getSessionDate())
				.notes(request.getNotes())
				.build();
		
		Session savedSession = sessionRepository.save(session);
		log.info("Session created successfully with id: {}", savedSession.getId());
		return sessionMapper.toResponse(savedSession);
	}

	@Override
	public SessionResponse getSessionById(Long id) {
		log.debug("Fetching session with id: {}", id);
		Session session = sessionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + id));
		return sessionMapper.toResponse(session);
	}

	@Override
	public List<SessionResponse> getAllSessionsByMedicalFolder(Long medicalFolderId) {
		log.debug("Fetching all sessions for medicalFolderId: {}", medicalFolderId);
		List<Session> sessions = sessionRepository.findByMedicalFolderId(medicalFolderId);
		return sessions.stream()
				.map(sessionMapper::toResponse)
				.collect(Collectors.toList());
	}

	@Override
	public Page<SessionResponse> getSessionsByMedicalFolderPaginated(Long medicalFolderId, Pageable pageable) {
		log.debug("Fetching sessions paginated for medicalFolderId: {} with page: {}", medicalFolderId,
				pageable.getPageNumber());
		Page<Session> sessionPage = sessionRepository.findByMedicalFolderId(medicalFolderId, pageable);
		List<SessionResponse> responses = sessionPage.getContent().stream()
				.map(sessionMapper::toResponse)
				.collect(Collectors.toList());
		return new PageImpl<>(responses, pageable, sessionPage.getTotalElements());
	}

	@Override
	public SessionResponse updateSession(Long id, UpdateSessionRequest request) {
		log.debug("Updating session with id: {}", id);
		Session existingSession = sessionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + id));
		
		if (request.getMedicalFolderId() != null) {
			MedicalFolder medicalFolder = medicalFolderRepository.findById(request.getMedicalFolderId())
					.orElseThrow(() -> new ResourceNotFoundException("Medical folder not found with id: " + request.getMedicalFolderId()));
			existingSession.setMedicalFolder(medicalFolder);
		}
		
		if (request.getSessionDate() != null) {
			existingSession.setSessionDate(request.getSessionDate());
		}
		
		if (request.getNotes() != null) {
			existingSession.setNotes(request.getNotes());
		}
		
		Session savedSession = sessionRepository.save(existingSession);
		log.info("Session updated successfully with id: {}", id);
		return sessionMapper.toResponse(savedSession);
	}

	@Override
	public SessionResponse partialUpdateSession(Long id, UpdateSessionRequest request) {
		log.debug("Partially updating session with id: {}", id);
		Session existingSession = sessionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + id));
		
		if (request.getMedicalFolderId() != null) {
			MedicalFolder medicalFolder = medicalFolderRepository.findById(request.getMedicalFolderId())
					.orElseThrow(() -> new ResourceNotFoundException("Medical folder not found with id: " + request.getMedicalFolderId()));
			existingSession.setMedicalFolder(medicalFolder);
		}
		
		if (request.getSessionDate() != null) {
			existingSession.setSessionDate(request.getSessionDate());
		}
		
		if (request.getNotes() != null) {
			existingSession.setNotes(request.getNotes());
		}
		
		Session savedSession = sessionRepository.save(existingSession);
		log.info("Session partially updated successfully with id: {}", id);
		return sessionMapper.toResponse(savedSession);
	}

	@Override
	public void deleteSession(Long id) {
		log.debug("Deleting session with id: {}", id);
		Session session = sessionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + id));
		sessionRepository.delete(session);
		log.info("Session deleted successfully with id: {}", id);
	}
}
