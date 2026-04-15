package org.techhive.alertservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.alertservice.dto.IotAlertRequestDTO;
import org.techhive.alertservice.dto.IotAlertResponseDTO;
import org.techhive.alertservice.service.IotAlertService;

import java.util.List;

@RestController
@RequestMapping("/api/alerts/iot-alerts")
@RequiredArgsConstructor
public class IotAlertController {

    private final IotAlertService service;

    @PostMapping
    public ResponseEntity<IotAlertResponseDTO> createAlert(
            @Valid @RequestBody IotAlertRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createAlert(dto));
    }

    @GetMapping("/{patientId}")
    public ResponseEntity<List<IotAlertResponseDTO>> getAlerts(@PathVariable String patientId) {
        return ResponseEntity.ok(service.getAlerts(patientId));
    }

    @GetMapping("/{patientId}/unacknowledged")
    public ResponseEntity<List<IotAlertResponseDTO>> getUnacknowledgedAlerts(
            @PathVariable String patientId) {
        return ResponseEntity.ok(service.getUnacknowledgedAlerts(patientId));
    }

    @PatchMapping("/{id}/acknowledge")
    public ResponseEntity<IotAlertResponseDTO> acknowledge(@PathVariable Long id) {
        return ResponseEntity.ok(service.acknowledge(id));
    }
}
