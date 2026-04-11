package org.techhive.medicalservice.controller;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
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
import org.springframework.web.server.ResponseStatusException;
import org.techhive.medicalservice.dto.CreateMedicalFolderRequest;
import org.techhive.medicalservice.dto.MedicalFolderResponse;
import org.techhive.medicalservice.dto.MedicalFolderStatsResponse;
import org.techhive.medicalservice.dto.UpdateMedicalFolderRequest;
import org.techhive.medicalservice.service.MedicalFolderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/medical-folders")
@RequiredArgsConstructor
@Slf4j
public class MedicalFolderController {

	private final MedicalFolderService medicalFolderService;
	private final ObjectMapper objectMapper;

	/**
	 * Get medical folders with pagination.
	 * Query params: page (0-based), size (default 10), sort (e.g. createdAt,desc or patientId,asc).
	 *
	 * @param pageable page, size, sort from request
	 * @return paginated list of medical folder responses with status 200
	 */
	@GetMapping
	public ResponseEntity<Page<MedicalFolderResponse>> getMedicalFolders(
			@PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
			@RequestParam(required = false) String search) {
		log.info("GET /api/medical-folders - Fetching medical folders page: {} size: {} search: {}", pageable.getPageNumber(), pageable.getPageSize(), search);
		Page<MedicalFolderResponse> page = medicalFolderService.getMedicalFolders(pageable, search);
		return ResponseEntity.ok(page);
	}

	/**
	 * Get aggregate stats for medical folders (total, this month, this week, patient count).
	 *
	 * @return stats with status 200
	 */
	@GetMapping("/stats")
	public ResponseEntity<MedicalFolderStatsResponse> getMedicalFolderStats() {
		log.info("GET /api/medical-folders/stats - Fetching medical folder stats");
		MedicalFolderStatsResponse stats = medicalFolderService.getMedicalFolderStats();
		return ResponseEntity.ok(stats);
	}

	/**
	 * Get medical folders for a specific doctor
	 *
	 * @param doctorId the doctor ID
	 * @return list of medical folder responses with status 200
	 */
	@GetMapping("/doctor/{doctorId}")
	public ResponseEntity<List<MedicalFolderResponse>> getMedicalFoldersByDoctorId(@PathVariable String doctorId) {
		log.info("GET /api/medical-folders/doctor/{} - Fetching medical folders for doctor", doctorId);
		List<MedicalFolderResponse> responses = medicalFolderService.getMedicalFoldersByDoctorId(doctorId);
		return ResponseEntity.ok(responses);
	}

	/**
	 * Get medical folders for a specific patient
	 *
	 * @param patientId the patient ID (e.g. Keycloak subject)
	 * @return list of medical folder responses with status 200
	 */
	@GetMapping("/patient/{patientId}")
	public ResponseEntity<List<MedicalFolderResponse>> getMedicalFoldersByPatientId(@PathVariable String patientId) {
		log.info("GET /api/medical-folders/patient/{} - Fetching medical folders for patient", patientId);
		List<MedicalFolderResponse> responses = medicalFolderService.getMedicalFoldersByPatientId(patientId);
		return ResponseEntity.ok(responses);
	}

	/**
	 * Get medical folders for a specific patient and doctor
	 *
	 * @param patientId the patient ID
	 * @param doctorId the doctor ID
	 * @return list of medical folder responses with status 200
	 */
	@GetMapping("/patient/{patientId}/doctor/{doctorId}")
	public ResponseEntity<List<MedicalFolderResponse>> getMedicalFoldersByPatientAndDoctor(
			@PathVariable String patientId,
			@PathVariable String doctorId) {
		log.info("GET /api/medical-folders/patient/{}/doctor/{} - Fetching medical folders", patientId, doctorId);
		List<MedicalFolderResponse> responses = medicalFolderService.getMedicalFoldersByPatientIdAndDoctorId(patientId, doctorId);
		return ResponseEntity.ok(responses);
	}

	/**
	 * Create a new medical folder
	 * Doctor ID is automatically extracted from the JWT token (no need to send it in request body)
	 * 
	 * @param request the create medical folder request (only patientId is required)
	 * @return the created medical folder response with status 201
	 */
	@PostMapping
	public ResponseEntity<MedicalFolderResponse> createMedicalFolder(
			@Valid @RequestBody CreateMedicalFolderRequest request,
			Authentication authentication,
			HttpServletRequest httpServletRequest) {
		log.info("POST /api/medical-folders - Creating new medical folder for patient: {}", request.getPatientId());

		String keycloakId = extractKeycloakId(authentication, httpServletRequest);
		log.debug("Extracted Keycloak ID from token: {}", keycloakId);

		// Set the doctor keycloak ID (this will be looked up to get the numeric doctorId)
		request.setDoctorId(keycloakId);

		MedicalFolderResponse response = medicalFolderService.createMedicalFolder(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	private String extractKeycloakId(Authentication authentication, HttpServletRequest httpServletRequest) {
		if (authentication != null && authentication.getPrincipal() != null
				&& StringUtils.hasText(authentication.getPrincipal().toString())) {
			return authentication.getPrincipal().toString();
		}

		String authHeader = httpServletRequest.getHeader("Authorization");
		if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
					"Missing or invalid Authorization header");
		}

		String token = authHeader.substring(7);
		try {
			String[] parts = token.split("\\.");
			if (parts.length < 2) {
				throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT token format");
			}

			String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
			Map<String, Object> claims = objectMapper.readValue(payloadJson, Map.class);
			Object subClaim = claims.get("sub");

			if (subClaim == null || !StringUtils.hasText(subClaim.toString())) {
				throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT token missing subject (sub) claim");
			}

			return subClaim.toString();
		} catch (ResponseStatusException ex) {
			throw ex;
		} catch (Exception ex) {
			log.warn("Failed to extract subject from bearer token", ex);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to parse JWT token", ex);
		}
	}

	/**
	 * Get a medical folder by ID
	 * 
	 * @param id the medical folder ID
	 * @return the medical folder response with status 200
	 */
	@GetMapping("/{id}")
	public ResponseEntity<MedicalFolderResponse> getMedicalFolderById(@PathVariable Long id) {
		log.info("GET /api/medical-folders/{} - Fetching medical folder", id);
		MedicalFolderResponse response = medicalFolderService.getMedicalFolderById(id);
		return ResponseEntity.ok(response);
	}

	/**
	 * Update a medical folder (Full update)
	 * 
	 * @param id the medical folder ID
	 * @param request the update medical folder request
	 * @return the updated medical folder response with status 200
	 */
	@PutMapping("/{id}")
	public ResponseEntity<MedicalFolderResponse> updateMedicalFolder(
			@PathVariable Long id,
			@RequestBody UpdateMedicalFolderRequest request) {
		log.info("PUT /api/medical-folders/{} - Updating medical folder", id);
		MedicalFolderResponse response = medicalFolderService.updateMedicalFolder(id, request);
		return ResponseEntity.ok(response);
	}

	/**
	 * Partially update a medical folder
	 * 
	 * @param id the medical folder ID
	 * @param request the update medical folder request
	 * @return the updated medical folder response with status 200
	 */
	@PatchMapping("/{id}")
	public ResponseEntity<MedicalFolderResponse> partialUpdateMedicalFolder(
			@PathVariable Long id,
			@RequestBody UpdateMedicalFolderRequest request) {
		log.info("PATCH /api/medical-folders/{} - Partially updating medical folder", id);
		MedicalFolderResponse response = medicalFolderService.partialUpdateMedicalFolder(id, request);
		return ResponseEntity.ok(response);
	}

	/**
	 * Delete a medical folder
	 * 
	 * @param id the medical folder ID
	 * @return status 204 (No Content)
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteMedicalFolder(@PathVariable Long id) {
		log.info("DELETE /api/medical-folders/{} - Deleting medical folder", id);
		medicalFolderService.deleteMedicalFolder(id);
		return ResponseEntity.noContent().build();
	}
}
