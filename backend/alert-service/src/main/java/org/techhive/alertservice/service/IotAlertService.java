package org.techhive.alertservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.techhive.alertservice.dto.IotAlertRequestDTO;
import org.techhive.alertservice.dto.IotAlertResponseDTO;
import org.techhive.alertservice.entity.IotAlert;
import org.techhive.alertservice.repository.IotAlertRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IotAlertService {

    private final IotAlertRepository repository;

    public IotAlertResponseDTO createAlert(IotAlertRequestDTO dto) {
        IotAlert entity = IotAlert.builder()
                .patientId(dto.getPatientId())
                .alertType(dto.getAlertType())
                .value(dto.getValue())
                .message(dto.getMessage())
                .acknowledged(false)
                .build();

        IotAlert saved = repository.save(entity);
        log.info("🚨 IoT alert recorded — patient={} type={} bpm={}",
                dto.getPatientId(), dto.getAlertType(), dto.getValue());
        return toResponseDTO(saved);
    }

    public List<IotAlertResponseDTO> getAlerts(String patientId) {
        return repository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<IotAlertResponseDTO> getUnacknowledgedAlerts(String patientId) {
        return repository.findByPatientIdAndAcknowledgedFalse(patientId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public IotAlertResponseDTO acknowledge(Long id) {
        IotAlert entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("IotAlert not found with id: " + id));
        entity.setAcknowledged(true);
        entity.setAcknowledgedAt(LocalDateTime.now());
        return toResponseDTO(repository.save(entity));
    }

    private IotAlertResponseDTO toResponseDTO(IotAlert entity) {
        return IotAlertResponseDTO.builder()
                .id(entity.getId())
                .patientId(entity.getPatientId())
                .alertType(entity.getAlertType())
                .value(entity.getValue())
                .message(entity.getMessage())
                .acknowledged(entity.isAcknowledged())
                .acknowledgedAt(entity.getAcknowledgedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
