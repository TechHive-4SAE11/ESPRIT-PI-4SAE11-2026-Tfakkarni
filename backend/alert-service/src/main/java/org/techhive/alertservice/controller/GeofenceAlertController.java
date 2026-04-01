package org.techhive.alertservice.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.alertservice.dto.GeofenceAlertRequestDTO;
import org.techhive.alertservice.dto.GeofenceAlertResponseDTO;
import org.techhive.alertservice.service.GeofenceAlertService;

import java.util.List;

@RestController
@RequestMapping("/api/alerts/geofence-violations")
public class GeofenceAlertController {

  private final GeofenceAlertService service;

  public GeofenceAlertController(GeofenceAlertService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<GeofenceAlertResponseDTO> reportViolation(
      @Valid @RequestBody GeofenceAlertRequestDTO dto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.reportViolation(dto));
  }

  @GetMapping("/{patientId}")
  public ResponseEntity<List<GeofenceAlertResponseDTO>> getAlerts(@PathVariable String patientId) {
    return ResponseEntity.ok(service.getAlerts(patientId));
  }

  @GetMapping("/{patientId}/unacknowledged")
  public ResponseEntity<List<GeofenceAlertResponseDTO>> getUnacknowledgedAlerts(
      @PathVariable String patientId) {
    return ResponseEntity.ok(service.getUnacknowledgedAlerts(patientId));
  }

  @PatchMapping("/{id}/acknowledge")
  public ResponseEntity<GeofenceAlertResponseDTO> acknowledge(@PathVariable Long id) {
    return ResponseEntity.ok(service.acknowledge(id));
  }
}
