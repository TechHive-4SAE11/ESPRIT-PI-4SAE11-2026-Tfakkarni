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
import org.techhive.medicalservice.dto.CreateDiagnosticsRequest;
import org.techhive.medicalservice.dto.DiagnosticsResponse;
import org.techhive.medicalservice.dto.UpdateDiagnosticsRequest;
import org.techhive.medicalservice.service.DiagnosticsService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/diagnostics")
@RequiredArgsConstructor
@Slf4j
public class DiagnosticsController {

	private final DiagnosticsService diagnosticsService;

	@PostMapping
	public ResponseEntity<DiagnosticsResponse> createDiagnostics(
			@Valid @RequestBody CreateDiagnosticsRequest request) {
		log.info("POST /api/diagnostics - Creating new diagnostics");
		DiagnosticsResponse response = diagnosticsService.createDiagnostics(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{id}")
	public ResponseEntity<DiagnosticsResponse> getDiagnosticsById(@PathVariable Long id) {
		log.info("GET /api/diagnostics/{} - Retrieving diagnostics", id);
		DiagnosticsResponse response = diagnosticsService.getDiagnosticsById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping
	public ResponseEntity<List<DiagnosticsResponse>> getDiagnosticsByMedicalFolder(
			@RequestParam(name = "medicalFolderId") Long medicalFolderId) {
		log.info("GET /api/diagnostics?medicalFolderId={} - Retrieving diagnostics for medical folder", medicalFolderId);
		List<DiagnosticsResponse> responses = diagnosticsService.getDiagnosticsByMedicalFolder(medicalFolderId);
		return ResponseEntity.ok(responses);
	}

	@PutMapping("/{id}")
	public ResponseEntity<DiagnosticsResponse> updateDiagnostics(
			@PathVariable Long id,
			@Valid @RequestBody UpdateDiagnosticsRequest request) {
		log.info("PUT /api/diagnostics/{} - Updating diagnostics", id);
		DiagnosticsResponse response = diagnosticsService.updateDiagnostics(id, request);
		return ResponseEntity.ok(response);
	}

	@PatchMapping("/{id}")
	public ResponseEntity<DiagnosticsResponse> partialUpdateDiagnostics(
			@PathVariable Long id,
			@RequestBody UpdateDiagnosticsRequest request) {
		log.info("PATCH /api/diagnostics/{} - Partially updating diagnostics", id);
		DiagnosticsResponse response = diagnosticsService.partialUpdateDiagnostics(id, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteDiagnostics(@PathVariable Long id) {
		log.info("DELETE /api/diagnostics/{} - Deleting diagnostics", id);
		diagnosticsService.deleteDiagnostics(id);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}
}
