package org.techhive.medicalservice.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.techhive.medicalservice.dto.CreateSessionRequest;
import org.techhive.medicalservice.dto.SessionResponse;
import org.techhive.medicalservice.dto.UpdateSessionRequest;
import org.techhive.medicalservice.service.SessionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@Slf4j
public class SessionController {

	private final SessionService sessionService;

	/**
	 * Create a new session
	 * 
	 * @param request the create session request
	 * @return the created session response with status 201
	 */
	@PostMapping
	public ResponseEntity<SessionResponse> createSession(@Valid @RequestBody CreateSessionRequest request) {
		log.info("POST /api/sessions - Creating new session");
		SessionResponse response = sessionService.createSession(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * Get a session by ID
	 * 
	 * @param id the session ID
	 * @return the session response with status 200
	 */
	@GetMapping("/{id}")
	public ResponseEntity<SessionResponse> getSessionById(@PathVariable Long id) {
		log.info("GET /api/sessions/{} - Fetching session", id);
		SessionResponse response = sessionService.getSessionById(id);
		return ResponseEntity.ok(response);
	}

	/**
	 * Get all sessions for a medical folder
	 * 
	 * @param medicalFolderId the medical folder ID
	 * @return list of sessions with status 200
	 */
	@GetMapping
	public ResponseEntity<List<SessionResponse>> getSessionsByMedicalFolder(
			@RequestParam Long medicalFolderId,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size) {
		log.info("GET /api/sessions?medicalFolderId={} - Fetching sessions", medicalFolderId);

		if (page != null && size != null) {
			Pageable pageable = PageRequest.of(page, size);
			Page<SessionResponse> responsePage = sessionService.getSessionsByMedicalFolderPaginated(medicalFolderId,
					pageable);
			return ResponseEntity.ok(responsePage.getContent());
		}

		List<SessionResponse> responses = sessionService.getAllSessionsByMedicalFolder(medicalFolderId);
		return ResponseEntity.ok(responses);
	}

	/**
	 * Get paginated sessions for a medical folder
	 * 
	 * @param medicalFolderId the medical folder ID
	 * @param page the page number
	 * @param size the page size
	 * @return paginated sessions with status 200
	 */
	@GetMapping("/paginated")
	public ResponseEntity<Page<SessionResponse>> getSessionsPaginated(
			@RequestParam Long medicalFolderId,
			@RequestParam(defaultValue = "0") Integer page,
			@RequestParam(defaultValue = "10") Integer size) {
		log.info("GET /api/sessions/paginated - Fetching paginated sessions for medicalFolderId: {}", medicalFolderId);
		Pageable pageable = PageRequest.of(page, size);
		Page<SessionResponse> responsePage = sessionService.getSessionsByMedicalFolderPaginated(medicalFolderId,
				pageable);
		return ResponseEntity.ok(responsePage);
	}

	/**
	 * Update a session (full update)
	 * 
	 * @param id the session ID
	 * @param request the update session request
	 * @return the updated session response with status 200
	 */
	@PutMapping("/{id}")
	public ResponseEntity<SessionResponse> updateSession(@PathVariable Long id,
			@Valid @RequestBody UpdateSessionRequest request) {
		log.info("PUT /api/sessions/{} - Updating session", id);
		SessionResponse response = sessionService.updateSession(id, request);
		return ResponseEntity.ok(response);
	}

	/**
	 * Partially update a session
	 * 
	 * @param id the session ID
	 * @param request the update session request
	 * @return the updated session response with status 200
	 */
	@PatchMapping("/{id}")
	public ResponseEntity<SessionResponse> partialUpdateSession(@PathVariable Long id,
			@RequestBody UpdateSessionRequest request) {
		log.info("PATCH /api/sessions/{} - Partially updating session", id);
		SessionResponse response = sessionService.partialUpdateSession(id, request);
		return ResponseEntity.ok(response);
	}

	/**
	 * Delete a session
	 * 
	 * @param id the session ID
	 * @return status 204 (No Content)
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteSession(@PathVariable Long id) {
		log.info("DELETE /api/sessions/{} - Deleting session", id);
		sessionService.deleteSession(id);
		return ResponseEntity.noContent().build();
	}
}
