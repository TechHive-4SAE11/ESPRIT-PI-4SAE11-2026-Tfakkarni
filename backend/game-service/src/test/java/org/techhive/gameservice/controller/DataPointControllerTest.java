package org.techhive.gameservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.techhive.gameservice.dto.*;
import org.techhive.gameservice.entity.DataPointPerformance;
import org.techhive.gameservice.entity.DataPointType;
import org.techhive.gameservice.repository.DataPointPerformanceRepository;
import org.techhive.gameservice.service.DataPointService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DataPointController.class)
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "google.translate.api-key=test",
    "elevenlabs.api-key=test",
    "elevenlabs.voice-id-en=test",
    "elevenlabs.voice-id-tn=test",
    "elevenlabs.model-id=test"
})
class DataPointControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockBean private DataPointService dataPointService;
  @MockBean private DataPointPerformanceRepository performanceRepository;

  @Test
  void photoEndpointsDelegateToService() throws Exception {
    DataPointSummary summary = summary(DataPointType.PHOTO, "Home");
    when(dataPointService.createPhoto(eq("patient-1"), any(CreatePhotoRequest.class))).thenReturn(summary);
    when(dataPointService.updatePhoto(eq(7L), any(UpdateDataPointRequest.class))).thenReturn(summary);

    mockMvc.perform(post("/api/games/data/photos/patient-1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new CreatePhotoRequest("Home", "aW1n", "image/png", List.of()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("PHOTO"));

    mockMvc.perform(put("/api/games/data/photos/7")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new UpdateDataPointRequest("New", null, null, null, null, null, List.of()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.label").value("Home"));

    mockMvc.perform(delete("/api/games/data/photos/7"))
        .andExpect(status().isNoContent());
  }

  @Test
  void placeMovieAndQuestionCreateEndpointsReturnSummaries() throws Exception {
    when(dataPointService.createPlace(eq("patient-1"), any(CreateMemoryPlaceRequest.class)))
        .thenReturn(summary(DataPointType.PLACE, "Park"));
    when(dataPointService.createMovie(eq("patient-1"), any(CreateMovieMemoryRequest.class)))
        .thenReturn(summary(DataPointType.MOVIE, "Movie"));
    when(dataPointService.createQuestion(eq("patient-1"), any(CreateQuestionMemoryRequest.class)))
        .thenReturn(summary(DataPointType.QUESTION, "Question"));

    mockMvc.perform(post("/api/games/data/places/patient-1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new CreateMemoryPlaceRequest("Park", 36.8, 10.18, "hint", List.of()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("PLACE"));

    mockMvc.perform(post("/api/games/data/movies/patient-1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new CreateMovieMemoryRequest(1, "Movie", "/p.jpg", "2020", "Hero", List.of()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("MOVIE"));

    mockMvc.perform(post("/api/games/data/questions/patient-1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new CreateQuestionMemoryRequest("Who?", "Nour", List.of()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("QUESTION"));
  }

  @Test
  void updateAndDeleteEndpointsForNonPhotoTypesWork() throws Exception {
    when(dataPointService.updatePlace(eq(1L), any(UpdateDataPointRequest.class))).thenReturn(summary(DataPointType.PLACE, "Park"));
    when(dataPointService.updateMovie(eq(2L), any(UpdateDataPointRequest.class))).thenReturn(summary(DataPointType.MOVIE, "Movie"));
    when(dataPointService.updateQuestion(eq(3L), any(UpdateDataPointRequest.class))).thenReturn(summary(DataPointType.QUESTION, "Question"));
    String body = objectMapper.writeValueAsString(new UpdateDataPointRequest());

    mockMvc.perform(put("/api/games/data/places/1").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk()).andExpect(jsonPath("$.type").value("PLACE"));
    mockMvc.perform(delete("/api/games/data/places/1")).andExpect(status().isNoContent());
    mockMvc.perform(put("/api/games/data/movies/2").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk()).andExpect(jsonPath("$.type").value("MOVIE"));
    mockMvc.perform(delete("/api/games/data/movies/2")).andExpect(status().isNoContent());
    mockMvc.perform(put("/api/games/data/questions/3").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk()).andExpect(jsonPath("$.type").value("QUESTION"));
    mockMvc.perform(delete("/api/games/data/questions/3")).andExpect(status().isNoContent());
  }

  @Test
  void listCountsAndPerformanceEndpointsReturnRepositoryData() throws Exception {
    when(dataPointService.getAllDataPoints(eq("patient-1"), anyList(), anyList()))
        .thenReturn(List.of(summary(DataPointType.PHOTO, "Home")));
    when(dataPointService.getCounts("patient-1")).thenReturn(Map.of("PHOTO", 1L, "PLACE", 2L));
    DataPointPerformance performance = new DataPointPerformance();
    performance.setId(5L);
    performance.setPatientKeycloakId("patient-1");
    when(performanceRepository.findByPatientKeycloakId("patient-1")).thenReturn(List.of(performance));

    mockMvc.perform(get("/api/games/data/patient-1")
        .param("types", "PHOTO")
        .param("tagIds", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].label").value("Home"));

    mockMvc.perform(get("/api/games/data/patient-1/counts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.PHOTO").value(1));

    mockMvc.perform(get("/api/games/data/performance/patient-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].patientKeycloakId").value("patient-1"));
  }

  private DataPointSummary summary(DataPointType type, String label) {
    return DataPointSummary.builder()
        .id(1L)
        .type(type)
        .label(label)
        .subtitle("subtitle")
        .correctAnswer(label)
        .createdAt(LocalDateTime.now())
        .tags(List.of(new TagResponse(1L, "Family", "#3b82f6")))
        .build();
  }
}
