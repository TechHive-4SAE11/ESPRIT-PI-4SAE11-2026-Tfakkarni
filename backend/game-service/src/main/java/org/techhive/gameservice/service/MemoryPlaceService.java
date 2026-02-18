package org.techhive.gameservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.gameservice.dto.CreatePlaceRequest;
import org.techhive.gameservice.dto.PlaceQuizResponse;
import org.techhive.gameservice.dto.PlaceResponse;
import org.techhive.gameservice.entity.MemoryPlace;
import org.techhive.gameservice.repository.MemoryPlaceRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryPlaceService {

  private final MemoryPlaceRepository memoryPlaceRepository;

  @Transactional
  public PlaceResponse createPlace(String patientKeycloakId, CreatePlaceRequest request) {
    MemoryPlace place = new MemoryPlace(
        request.getName(),
        request.getLatitude(),
        request.getLongitude(),
        request.getHint(),
        patientKeycloakId);
    place = memoryPlaceRepository.save(place);
    log.info("Created memory place '{}' (id={}) for patient '{}'", place.getName(), place.getId(), patientKeycloakId);
    return toResponse(place);
  }

  public List<PlaceResponse> getPlacesByPatient(String patientKeycloakId) {
    return memoryPlaceRepository.findByPatientKeycloakId(patientKeycloakId)
        .stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  @Transactional
  public PlaceResponse editPlace(Long id, CreatePlaceRequest request) {
    MemoryPlace place = memoryPlaceRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Place not found: " + id));
    place.setName(request.getName());
    place.setLatitude(request.getLatitude());
    place.setLongitude(request.getLongitude());
    place.setHint(request.getHint());
    place = memoryPlaceRepository.save(place);
    log.info("Edited memory place '{}' (id={})", place.getName(), place.getId());
    return toResponse(place);
  }

  @Transactional
  public void deletePlace(Long id) {
    memoryPlaceRepository.deleteById(id);
    log.info("Deleted memory place id={}", id);
  }

  /**
   * Generate a quiz: 1 random correct place + 2 random wrong place names as
   * distractors.
   * Requires at least 3 places to exist for the patient.
   */
  public PlaceQuizResponse generateQuiz(String patientKeycloakId) {
    long count = memoryPlaceRepository.countByPatientKeycloakId(patientKeycloakId);
    if (count < 3) {
      throw new RuntimeException("Need at least 3 saved places to play the quiz. Currently have " + count + ".");
    }

    // Get 3 random places for this patient
    List<MemoryPlace> randomPlaces = memoryPlaceRepository.findRandomByPatientKeycloakId(patientKeycloakId, 3);

    // First one is the correct answer
    MemoryPlace correct = randomPlaces.get(0);

    // Build shuffled choices list
    List<String> choices = new ArrayList<>();
    for (MemoryPlace p : randomPlaces) {
      choices.add(p.getName());
    }
    Collections.shuffle(choices);

    PlaceQuizResponse quiz = new PlaceQuizResponse();
    quiz.setCorrectPlaceId(correct.getId());
    quiz.setCorrectName(correct.getName());
    quiz.setLatitude(correct.getLatitude());
    quiz.setLongitude(correct.getLongitude());
    quiz.setHint(correct.getHint());
    quiz.setChoices(choices);

    return quiz;
  }

  private PlaceResponse toResponse(MemoryPlace place) {
    return new PlaceResponse(
        place.getId(),
        place.getName(),
        place.getLatitude(),
        place.getLongitude(),
        place.getHint(),
        place.getCreatedAt() != null ? place.getCreatedAt().toString() : null);
  }
}
