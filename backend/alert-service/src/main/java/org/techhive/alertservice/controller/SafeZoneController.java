package org.techhive.alertservice.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.alertservice.dto.SafeZoneRequestDTO;
import org.techhive.alertservice.dto.SafeZoneResponseDTO;
import org.techhive.alertservice.service.SafeZoneService;

import java.util.List;

@RestController
@RequestMapping("/api/alerts/safe-zones")
public class SafeZoneController {

  private final SafeZoneService service;

  public SafeZoneController(SafeZoneService service) {
    this.service = service;
  }

  @PostMapping("/{patientId}")
  public ResponseEntity<SafeZoneResponseDTO> create(
      @PathVariable String patientId,
      @Valid @RequestBody SafeZoneRequestDTO dto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(patientId, dto));
  }

  @GetMapping("/{patientId}")
  public ResponseEntity<List<SafeZoneResponseDTO>> getByPatientId(@PathVariable String patientId) {
    return ResponseEntity.ok(service.getByPatientId(patientId));
  }

  @GetMapping("/{patientId}/active")
  public ResponseEntity<List<SafeZoneResponseDTO>> getActiveByPatientId(@PathVariable String patientId) {
    return ResponseEntity.ok(service.getActiveByPatientId(patientId));
  }

  @PutMapping("/{patientId}/{id}")
  public ResponseEntity<SafeZoneResponseDTO> update(
      @PathVariable String patientId,
      @PathVariable Long id,
      @Valid @RequestBody SafeZoneRequestDTO dto) {
    return ResponseEntity.ok(service.update(patientId, id, dto));
  }

  @DeleteMapping("/{patientId}/{id}")
  public ResponseEntity<Void> delete(
      @PathVariable String patientId,
      @PathVariable Long id) {
    service.delete(patientId, id);
    return ResponseEntity.noContent().build();
  }
}
