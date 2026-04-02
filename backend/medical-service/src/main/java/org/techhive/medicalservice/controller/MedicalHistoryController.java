package org.techhive.medicalservice.controller;

import java.util.List;

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
import org.techhive.medicalservice.dto.CreateMedicalHistoryRequest;
import org.techhive.medicalservice.dto.MedicalHistoryResponse;
import org.techhive.medicalservice.dto.UpdateMedicalHistoryRequest;
import org.techhive.medicalservice.service.MedicalHistoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/medical-history")
@RequiredArgsConstructor
@Slf4j
public class MedicalHistoryController {

	private final MedicalHistoryService medicalHistoryService;

	@PostMapping
	public ResponseEntity<MedicalHistoryResponse> createMedicalHistory(
			@Valid @RequestBody CreateMedicalHistoryRequest request) {
		log.info("POST /api/medical-history - Creating new medical history");
		MedicalHistoryResponse response = medicalHistoryService.createMedicalHistory(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	public ResponseEntity<MedicalHistoryResponse> getMedicalHistoryById(@PathVariable Long id) {
		log.info("GET /api/medical-history/{} - Retrieving medical history", id);
		MedicalHistoryResponse response = medicalHistoryService.getMedicalHistoryById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping
	public ResponseEntity<List<MedicalHistoryResponse>> getMedicalHistoryByMedicalFolder(
			@RequestParam(name = "medicalFolderId") Long medicalFolderId) {
		log.info("GET /api/medical-history?medicalFolderId={} - Retrieving medical history for medical folder", medicalFolderId);
		List<MedicalHistoryResponse> responses = medicalHistoryService.getMedicalHistoryByMedicalFolder(medicalFolderId);
		return ResponseEntity.ok(responses);
	}

	@PutMapping("/{id}")
	public ResponseEntity<MedicalHistoryResponse> updateMedicalHistory(
			@PathVariable Long id,
			@Valid @RequestBody UpdateMedicalHistoryRequest request) {
		log.info("PUT /api/medical-history/{} - Updating medical history", id);
		MedicalHistoryResponse response = medicalHistoryService.updateMedicalHistory(id, request);
		return ResponseEntity.ok(response);
	}

	@PatchMapping("/{id}")
	public ResponseEntity<MedicalHistoryResponse> partialUpdateMedicalHistory(
			@PathVariable Long id,
			@RequestBody UpdateMedicalHistoryRequest request) {
		log.info("PATCH /api/medical-history/{} - Partially updating medical history", id);
		MedicalHistoryResponse response = medicalHistoryService.partialUpdateMedicalHistory(id, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteMedicalHistory(@PathVariable Long id) {
		log.info("DELETE /api/medical-history/{} - Deleting medical history", id);
		medicalHistoryService.deleteMedicalHistory(id);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}
}
