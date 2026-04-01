package org.techhive.alertservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.techhive.alertservice.dto.LatLngDTO;
import org.techhive.alertservice.dto.SafeZoneRequestDTO;
import org.techhive.alertservice.dto.SafeZoneResponseDTO;
import org.techhive.alertservice.entity.SafeZone;
import org.techhive.alertservice.repository.SafeZoneRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SafeZoneService {

  private final SafeZoneRepository repository;
  private final ObjectMapper objectMapper;

  public SafeZoneService(SafeZoneRepository repository, ObjectMapper objectMapper) {
    this.repository = repository;
    this.objectMapper = objectMapper;
  }

  public SafeZoneResponseDTO create(String patientId, SafeZoneRequestDTO dto) {
    SafeZone entity = new SafeZone();
    entity.setPatientId(patientId);
    entity.setName(dto.getName());
    entity.setPoints(toJson(dto.getPoints()));
    entity.setActive(dto.isActive());
    return toResponseDTO(repository.save(entity));
  }

  public List<SafeZoneResponseDTO> getByPatientId(String patientId) {
    return repository.findByPatientId(patientId).stream()
        .map(this::toResponseDTO)
        .collect(Collectors.toList());
  }

  public List<SafeZoneResponseDTO> getActiveByPatientId(String patientId) {
    return repository.findByPatientIdAndActiveTrue(patientId).stream()
        .map(this::toResponseDTO)
        .collect(Collectors.toList());
  }

  public SafeZoneResponseDTO getById(Long id) {
    SafeZone entity = repository.findById(id)
        .orElseThrow(() -> new RuntimeException("SafeZone not found with id: " + id));
    return toResponseDTO(entity);
  }

  public SafeZoneResponseDTO update(String patientId, Long id, SafeZoneRequestDTO dto) {
    SafeZone entity = repository.findById(id)
        .orElseThrow(() -> new RuntimeException("SafeZone not found with id: " + id));

    if (!entity.getPatientId().equals(patientId)) {
      throw new RuntimeException("SafeZone does not belong to patient: " + patientId);
    }

    entity.setName(dto.getName());
    entity.setPoints(toJson(dto.getPoints()));
    entity.setActive(dto.isActive());
    return toResponseDTO(repository.save(entity));
  }

  public void delete(String patientId, Long id) {
    SafeZone entity = repository.findById(id)
        .orElseThrow(() -> new RuntimeException("SafeZone not found with id: " + id));

    if (!entity.getPatientId().equals(patientId)) {
      throw new RuntimeException("SafeZone does not belong to patient: " + patientId);
    }

    repository.deleteById(id);
  }

  private SafeZoneResponseDTO toResponseDTO(SafeZone entity) {
    return SafeZoneResponseDTO.builder()
        .id(entity.getId())
        .patientId(entity.getPatientId())
        .name(entity.getName())
        .points(fromJson(entity.getPoints()))
        .active(entity.isActive())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }

  private String toJson(List<LatLngDTO> points) {
    try {
      return objectMapper.writeValueAsString(points);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize points to JSON", e);
    }
  }

  private List<LatLngDTO> fromJson(String json) {
    try {
      return objectMapper.readValue(json, new TypeReference<List<LatLngDTO>>() {
      });
    } catch (JsonProcessingException e) {
      log.error("Failed to deserialize points JSON: {}", json, e);
      return List.of();
    }
  }
}
