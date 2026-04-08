package org.techhive.iotservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.techhive.iotservice.dto.HeartbeatReadingDTO;
import org.techhive.iotservice.entity.HeartbeatReading;
import org.techhive.iotservice.repository.HeartbeatReadingRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IotService {

    private final HeartbeatReadingRepository heartbeatRepo;
    private final HeartbeatAlertService alertService;

    /**
     * Get all heartbeat readings for a patient on a given night.
     * A "night" is defined as 20:00 on the given date to 12:00 the next day.
     */
    public List<HeartbeatReadingDTO> getHeartbeatReadings(String patientId, LocalDate date) {
        LocalDateTime start = date.atTime(LocalTime.of(20, 0));
        LocalDateTime end = date.plusDays(1).atTime(LocalTime.of(12, 0));

        return heartbeatRepo.findByPatientIdAndTimestampBetweenOrderByTimestampAsc(patientId, start, end)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Record a new heartbeat reading (for real-time IoT ingestion).
     * Triggers Telegram alert if BPM is abnormal.
     */
    public HeartbeatReadingDTO recordHeartbeat(HeartbeatReadingDTO dto) {
        HeartbeatReading reading = HeartbeatReading.builder()
                .patientId(dto.getPatientId())
                .bpm(dto.getBpm())
                .timestamp(dto.getTimestamp() != null ? dto.getTimestamp() : LocalDateTime.now())
                .build();
        HeartbeatReading saved = heartbeatRepo.save(reading);

        // Check for abnormal BPM and alert
        alertService.checkAndAlert(saved.getPatientId(), saved.getBpm());

        return toDTO(saved);
    }

    /**
     * Get the latest heartbeat reading for a patient.
     */
    public HeartbeatReadingDTO getLatestReading(String patientId) {
        return heartbeatRepo.findFirstByPatientIdOrderByTimestampDesc(patientId)
                .map(this::toDTO)
                .orElse(null);
    }

    private HeartbeatReadingDTO toDTO(HeartbeatReading entity) {
        return HeartbeatReadingDTO.builder()
                .id(entity.getId())
                .patientId(entity.getPatientId())
                .bpm(entity.getBpm())
                .timestamp(entity.getTimestamp())
                .build();
    }
}
