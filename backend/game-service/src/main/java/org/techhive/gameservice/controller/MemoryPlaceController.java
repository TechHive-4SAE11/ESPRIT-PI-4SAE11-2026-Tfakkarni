package org.techhive.gameservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.gameservice.dto.CreatePlaceRequest;
import org.techhive.gameservice.dto.PlaceQuizResponse;
import org.techhive.gameservice.dto.PlaceResponse;
import org.techhive.gameservice.service.MemoryPlaceService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/games/places")
@RequiredArgsConstructor
public class MemoryPlaceController {

  private final MemoryPlaceService memoryPlaceService;

  /**
   * Save a new memory place for a patient.
   */
  @PostMapping
  public ResponseEntity<?> createPlace(
      @RequestHeader("X-User-Id") String patientKeycloakId,
      @RequestBody CreatePlaceRequest request) {
    try {
      PlaceResponse place = memoryPlaceService.createPlace(patientKeycloakId, request);
      return ResponseEntity.status(HttpStatus.CREATED).body(place);
    } catch (Exception e) {
      log.error("Error creating memory place", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Failed to create place: " + e.getMessage()));
    }
  }

  /**
   * List all saved places for a patient.
   */
  @GetMapping("/patient/{keycloakId}")
  public ResponseEntity<List<PlaceResponse>> getPlacesByPatient(@PathVariable String keycloakId) {
    return ResponseEntity.ok(memoryPlaceService.getPlacesByPatient(keycloakId));
  }

  /**
   * Generate a place quiz: 1 correct place (lat/lng) + 3 shuffled name choices.
   */
  @GetMapping("/game/{keycloakId}")
  public ResponseEntity<?> getPlaceQuiz(@PathVariable String keycloakId) {
    try {
      PlaceQuizResponse quiz = memoryPlaceService.generateQuiz(keycloakId);
      return ResponseEntity.ok(quiz);
    } catch (RuntimeException e) {
      log.warn("Cannot generate quiz for patient '{}': {}", keycloakId, e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Delete a memory place.
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<?> deletePlace(@PathVariable Long id) {
    try {
      memoryPlaceService.deletePlace(id);
      return ResponseEntity.noContent().build();
    } catch (Exception e) {
      log.error("Error deleting memory place {}", id, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Failed to delete place: " + e.getMessage()));
    }
  }
}
