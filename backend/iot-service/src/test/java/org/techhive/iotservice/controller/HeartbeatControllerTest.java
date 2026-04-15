package org.techhive.iotservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.techhive.iotservice.dto.HeartbeatReadingDTO;
import org.techhive.iotservice.dto.SleepAnalysisResponse;
import org.techhive.iotservice.dto.SleepSummary;
import org.techhive.iotservice.service.IotService;
import org.techhive.iotservice.service.SleepAnalysisService;
import org.techhive.iotservice.service.FeatureGateClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class HeartbeatControllerTest {

    @Mock
    private IotService iotService;

    @Mock
    private SleepAnalysisService sleepAnalysisService;

    @Mock
    private FeatureGateClient featureGateClient;

    @InjectMocks
    private HeartbeatController heartbeatController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final String PATIENT_ID = "patient-123";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(heartbeatController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        when(featureGateClient.isIotEnabled(anyString())).thenReturn(true);
    }

    @Test
    void getReadings_withDate_returnsReadings() throws Exception {
        LocalDate date = LocalDate.of(2026, 4, 10);
        List<HeartbeatReadingDTO> readings = List.of(
                HeartbeatReadingDTO.builder().id(1L).patientId(PATIENT_ID).bpm(65)
                        .timestamp(LocalDateTime.of(2026, 4, 10, 22, 30)).build()
        );

        when(iotService.getHeartbeatReadings(PATIENT_ID, date)).thenReturn(readings);

        mockMvc.perform(get("/api/iot/heartbeat/{patientId}", PATIENT_ID)
                        .param("date", "2026-04-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bpm").value(65))
                .andExpect(jsonPath("$[0].patientId").value(PATIENT_ID));
    }

    @Test
    void getReadings_withoutDate_defaultsToYesterday() throws Exception {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        when(iotService.getHeartbeatReadings(PATIENT_ID, yesterday))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/iot/heartbeat/{patientId}", PATIENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getSleepAnalysis_returnsAnalysis() throws Exception {
        SleepAnalysisResponse response = SleepAnalysisResponse.builder()
                .patientId(PATIENT_ID)
                .date(LocalDate.of(2026, 4, 10))
                .timeline(List.of())
                .summary(SleepSummary.builder()
                        .totalSleepMinutes(420)
                        .qualityScore(85)
                        .qualityLabel("Excellent")
                        .build())
                .insights(List.of("Good sleep"))
                .build();

        when(sleepAnalysisService.analyze(eq(PATIENT_ID), any(LocalDate.class)))
                .thenReturn(response);

        mockMvc.perform(get("/api/iot/heartbeat/{patientId}/sleep-analysis", PATIENT_ID)
                        .param("date", "2026-04-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID))
                .andExpect(jsonPath("$.summary.qualityScore").value(85))
                .andExpect(jsonPath("$.summary.qualityLabel").value("Excellent"));
    }

    @Test
    void getLatestReading_found_returns200() throws Exception {
        HeartbeatReadingDTO dto = HeartbeatReadingDTO.builder()
                .id(1L).patientId(PATIENT_ID).bpm(72)
                .timestamp(LocalDateTime.now()).build();

        when(iotService.getLatestReading(PATIENT_ID)).thenReturn(dto);

        mockMvc.perform(get("/api/iot/heartbeat/{patientId}/latest", PATIENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bpm").value(72));
    }

    @Test
    void getLatestReading_noData_returns204() throws Exception {
        when(iotService.getLatestReading(PATIENT_ID)).thenReturn(null);

        mockMvc.perform(get("/api/iot/heartbeat/{patientId}/latest", PATIENT_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void recordHeartbeat_returns200() throws Exception {
        HeartbeatReadingDTO input = HeartbeatReadingDTO.builder()
                .patientId(PATIENT_ID).bpm(75)
                .timestamp(LocalDateTime.of(2026, 4, 10, 22, 0)).build();

        HeartbeatReadingDTO saved = HeartbeatReadingDTO.builder()
                .id(1L).patientId(PATIENT_ID).bpm(75)
                .timestamp(LocalDateTime.of(2026, 4, 10, 22, 0)).build();

        when(iotService.recordHeartbeat(any(HeartbeatReadingDTO.class))).thenReturn(saved);

        mockMvc.perform(post("/api/iot/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.bpm").value(75));
    }
}
