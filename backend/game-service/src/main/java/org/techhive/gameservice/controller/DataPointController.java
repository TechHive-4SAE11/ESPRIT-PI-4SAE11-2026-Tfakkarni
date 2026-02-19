package org.techhive.gameservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.techhive.gameservice.dto.*;
import org.techhive.gameservice.entity.DataPointType;
import org.techhive.gameservice.service.DataPointService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/games/data")
@RequiredArgsConstructor
public class DataPointController {

  private final DataPointService dataPointService;

  // ===================== PHOTO =====================

  @PostMapping("/photos/{keycloakId}")
  public ResponseEntity<DataPointSummary> createPhoto(
      @PathVariable String keycloakId,
      @Valid @RequestBody CreatePhotoRequest request) {
    return ResponseEntity.ok(dataPointService.createPhoto(keycloakId, request));
  }

  @DeleteMapping("/photos/{id}")
  public ResponseEntity<Void> deletePhoto(@PathVariable Long id) {
    dataPointService.deletePhoto(id);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/photos/{id}")
  public ResponseEntity<DataPointSummary> updatePhoto(
      @PathVariable Long id,
      @RequestBody UpdateDataPointRequest request) {
    return ResponseEntity.ok(dataPointService.updatePhoto(id, request));
  }

  // ===================== PLACE =====================

  @PostMapping("/places/{keycloakId}")
  public ResponseEntity<DataPointSummary> createPlace(
      @PathVariable String keycloakId,
      @Valid @RequestBody CreateMemoryPlaceRequest request) {
    return ResponseEntity.ok(dataPointService.createPlace(keycloakId, request));
  }

  @DeleteMapping("/places/{id}")
  public ResponseEntity<Void> deletePlace(@PathVariable Long id) {
    dataPointService.deletePlace(id);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/places/{id}")
  public ResponseEntity<DataPointSummary> updatePlace(
      @PathVariable Long id,
      @RequestBody UpdateDataPointRequest request) {
    return ResponseEntity.ok(dataPointService.updatePlace(id, request));
  }

  // ===================== MOVIE =====================

  @PostMapping("/movies/{keycloakId}")
  public ResponseEntity<DataPointSummary> createMovie(
      @PathVariable String keycloakId,
      @Valid @RequestBody CreateMovieMemoryRequest request) {
    return ResponseEntity.ok(dataPointService.createMovie(keycloakId, request));
  }

  @DeleteMapping("/movies/{id}")
  public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
    dataPointService.deleteMovie(id);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/movies/{id}")
  public ResponseEntity<DataPointSummary> updateMovie(
      @PathVariable Long id,
      @RequestBody UpdateDataPointRequest request) {
    return ResponseEntity.ok(dataPointService.updateMovie(id, request));
  }

  // ===================== QUESTION =====================

  @PostMapping("/questions/{keycloakId}")
  public ResponseEntity<DataPointSummary> createQuestion(
      @PathVariable String keycloakId,
      @Valid @RequestBody CreateQuestionMemoryRequest request) {
    return ResponseEntity.ok(dataPointService.createQuestion(keycloakId, request));
  }

  @DeleteMapping("/questions/{id}")
  public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
    dataPointService.deleteQuestion(id);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/questions/{id}")
  public ResponseEntity<DataPointSummary> updateQuestion(
      @PathVariable Long id,
      @RequestBody UpdateDataPointRequest request) {
    return ResponseEntity.ok(dataPointService.updateQuestion(id, request));
  }

  // ===================== LIST ALL =====================

  @GetMapping("/{keycloakId}")
  public ResponseEntity<List<DataPointSummary>> getAllDataPoints(
      @PathVariable String keycloakId,
      @RequestParam(required = false) List<DataPointType> types,
      @RequestParam(required = false) List<Long> tagIds) {
    return ResponseEntity.ok(dataPointService.getAllDataPoints(keycloakId, types, tagIds));
  }

  @GetMapping("/{keycloakId}/counts")
  public ResponseEntity<Map<String, Long>> getCounts(@PathVariable String keycloakId) {
    return ResponseEntity.ok(dataPointService.getCounts(keycloakId));
  }
}
