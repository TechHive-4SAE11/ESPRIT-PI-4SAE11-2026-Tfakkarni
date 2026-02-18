package org.techhive.medicalservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.techhive.medicalservice.dto.CreateMedicalFolderRequest;
import org.techhive.medicalservice.dto.MedicalFolderResponse;
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

	/**
	 * Create a new medical folder
	 * 
	 * @param request the create medical folder request
	 * @return the created medical folder response with status 201
	 */
	@PostMapping
	public ResponseEntity<MedicalFolderResponse> createMedicalFolder(
			@Valid @RequestBody CreateMedicalFolderRequest request) {
		log.info("POST /api/medical-folders - Creating new medical folder");
		MedicalFolderResponse response = medicalFolderService.createMedicalFolder(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
