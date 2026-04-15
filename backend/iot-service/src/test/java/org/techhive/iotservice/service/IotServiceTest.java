package org.techhive.iotservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.iotservice.dto.HeartbeatReadingDTO;
import org.techhive.iotservice.entity.HeartbeatReading;
import org.techhive.iotservice.repository.HeartbeatReadingRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IotServiceTest {

    @Mock
    private HeartbeatReadingRepository heartbeatRepo;

    @Mock
    private HeartbeatAlertService alertService;

    @InjectMocks
    private IotService iotService;

    private static final String PATIENT_ID = "patient-123";

    @Test
    void getHeartbeatReadings_returnsReadingsForNight() {
        LocalDate date = LocalDate.of(2026, 4, 10);
        LocalDateTime expectedStart = date.atTime(20, 0);
        LocalDateTime expectedEnd = date.plusDays(1).atTime(12, 0);

        HeartbeatReading r1 = HeartbeatReading.builder()
                .id(1L).patientId(PATIENT_ID).bpm(65).timestamp(date.atTime(22, 30)).build();
        HeartbeatReading r2 = HeartbeatReading.builder()
                .id(2L).patientId(PATIENT_ID).bpm(58).timestamp(date.atTime(23, 45)).build();

        when(heartbeatRepo.findByPatientIdAndTimestampBetweenOrderByTimestampAsc(
                PATIENT_ID, expectedStart, expectedEnd))
                .thenReturn(List.of(r1, r2));

        List<HeartbeatReadingDTO> result = iotService.getHeartbeatReadings(PATIENT_ID, date);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getBpm()).isEqualTo(65);
        assertThat(result.get(1).getBpm()).isEqualTo(58);
        assertThat(result.get(0).getPatientId()).isEqualTo(PATIENT_ID);
    }

    @Test
    void getHeartbeatReadings_emptyResult() {
        LocalDate date = LocalDate.of(2026, 4, 10);
        when(heartbeatRepo.findByPatientIdAndTimestampBetweenOrderByTimestampAsc(
                eq(PATIENT_ID), any(), any()))
                .thenReturn(List.of());

        List<HeartbeatReadingDTO> result = iotService.getHeartbeatReadings(PATIENT_ID, date);

        assertThat(result).isEmpty();
    }

    @Test
    void recordHeartbeat_savesAndReturnsDTO() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 4, 10, 22, 30);
        HeartbeatReadingDTO dto = HeartbeatReadingDTO.builder()
                .patientId(PATIENT_ID).bpm(72).timestamp(timestamp).build();

        HeartbeatReading saved = HeartbeatReading.builder()
                .id(1L).patientId(PATIENT_ID).bpm(72).timestamp(timestamp).build();

        when(heartbeatRepo.save(any(HeartbeatReading.class))).thenReturn(saved);

        HeartbeatReadingDTO result = iotService.recordHeartbeat(dto);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getBpm()).isEqualTo(72);
        assertThat(result.getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(result.getTimestamp()).isEqualTo(timestamp);

        verify(heartbeatRepo).save(any(HeartbeatReading.class));
    }

    @Test
    void recordHeartbeat_triggersAlertCheck() {
        HeartbeatReadingDTO dto = HeartbeatReadingDTO.builder()
                .patientId(PATIENT_ID).bpm(130).timestamp(LocalDateTime.now()).build();

        HeartbeatReading saved = HeartbeatReading.builder()
                .id(1L).patientId(PATIENT_ID).bpm(130).timestamp(LocalDateTime.now()).build();

        when(heartbeatRepo.save(any())).thenReturn(saved);

        iotService.recordHeartbeat(dto);

        verify(alertService).checkAndAlert(PATIENT_ID, 130);
    }

    @Test
    void recordHeartbeat_usesCurrentTimestampWhenNull() {
        HeartbeatReadingDTO dto = HeartbeatReadingDTO.builder()
                .patientId(PATIENT_ID).bpm(70).timestamp(null).build();

        HeartbeatReading saved = HeartbeatReading.builder()
                .id(1L).patientId(PATIENT_ID).bpm(70).timestamp(LocalDateTime.now()).build();

        when(heartbeatRepo.save(argThat(reading -> reading.getTimestamp() != null)))
                .thenReturn(saved);

        HeartbeatReadingDTO result = iotService.recordHeartbeat(dto);

        assertThat(result).isNotNull();
        verify(heartbeatRepo).save(argThat(reading ->
                reading.getTimestamp() != null && reading.getPatientId().equals(PATIENT_ID)));
    }

    @Test
    void getLatestReading_returnsLatest() {
        HeartbeatReading reading = HeartbeatReading.builder()
                .id(5L).patientId(PATIENT_ID).bpm(68).timestamp(LocalDateTime.now()).build();

        when(heartbeatRepo.findFirstByPatientIdOrderByTimestampDesc(PATIENT_ID))
                .thenReturn(Optional.of(reading));

        HeartbeatReadingDTO result = iotService.getLatestReading(PATIENT_ID);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getBpm()).isEqualTo(68);
    }

    @Test
    void getLatestReading_returnsNullWhenNoData() {
        when(heartbeatRepo.findFirstByPatientIdOrderByTimestampDesc(PATIENT_ID))
                .thenReturn(Optional.empty());

        HeartbeatReadingDTO result = iotService.getLatestReading(PATIENT_ID);

        assertThat(result).isNull();
    }
}
