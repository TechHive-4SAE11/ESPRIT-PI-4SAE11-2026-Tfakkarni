package org.techhive.trackingservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.techhive.trackingservice.dto.*;
import org.techhive.trackingservice.entity.*;
import org.techhive.trackingservice.service.DailyMonitoringService;
import org.techhive.trackingservice.service.ElevenLabsService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DailyMonitoringControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock private DailyMonitoringService svc;
    @Mock private ElevenLabsService elevenLabsService;

    @InjectMocks private DailyMonitoringController controller;

    private static final String PATIENT_ID = "patient-abc-123";
    private static final LocalDate TODAY = LocalDate.of(2026, 4, 15);

    private DailyLog sampleLog;
    private DailyLogResponse sampleResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(converter)
                .build();

        sampleLog = new DailyLog();
        sampleLog.setId(1L);
        sampleLog.setPatientKeycloakId(PATIENT_ID);
        sampleLog.setLogDate(TODAY);
        sampleLog.setNutritionEntries(new ArrayList<>());
        sampleLog.setMedicationIntakes(new ArrayList<>());
        sampleLog.setActivityEntries(new ArrayList<>());
        sampleLog.setIncidentEntries(new ArrayList<>());

        sampleResponse = new DailyLogResponse();
        sampleResponse.setId(1L);
        sampleResponse.setPatientKeycloakId(PATIENT_ID);
        sampleResponse.setLogDate(TODAY);
        sampleResponse.setNutritionEntries(Collections.emptyList());
        sampleResponse.setMedicationIntakes(Collections.emptyList());
        sampleResponse.setActivityEntries(Collections.emptyList());
        sampleResponse.setIncidentEntries(Collections.emptyList());
    }

    // ════════════════════════════════════════════════════════════════════════
    // Daily Log endpoints
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Daily Log endpoints")
    class DailyLogEndpoints {

        @Test
        @DisplayName("POST /patient/{id}/date/{date} — devrait créer ou retourner le log du jour")
        void getOrCreate() throws Exception {
            when(svc.getOrCreateLog(PATIENT_ID, TODAY)).thenReturn(sampleLog);
            when(svc.toResponse(sampleLog)).thenReturn(sampleResponse);

            mockMvc.perform(post("/api/daily-monitoring/patient/{keycloakId}/date/{date}",
                            PATIENT_ID, TODAY.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.patientKeycloakId").value(PATIENT_ID))
                    .andExpect(jsonPath("$.logDate").value(TODAY.toString()));
        }

        @Test
        @DisplayName("GET /patient/{id} — devrait retourner tous les logs du patient")
        void getAllLogs() throws Exception {
            when(svc.getLogsForPatient(PATIENT_ID)).thenReturn(List.of(sampleLog));
            when(svc.toResponse(any(DailyLog.class))).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/daily-monitoring/patient/{keycloakId}", PATIENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(1));
        }

        @Test
        @DisplayName("GET /{id} — devrait retourner un log par ID")
        void getById() throws Exception {
            when(svc.getLogById(1L)).thenReturn(sampleLog);
            when(svc.toResponse(sampleLog)).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/daily-monitoring/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.patientKeycloakId").value(PATIENT_ID));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Available Medications
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /patient/{id}/available-medications — devrait retourner les médicaments")
    void getAvailableMedications() throws Exception {
        AvailableMedicationDTO med = new AvailableMedicationDTO(100L, "Doliprane", "1000mg", "2x/jour", "Après repas");
        when(svc.getAvailableMedications(PATIENT_ID)).thenReturn(List.of(med));

        mockMvc.perform(get("/api/daily-monitoring/patient/{keycloakId}/available-medications", PATIENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].medicationName").value("Doliprane"))
                .andExpect(jsonPath("$[0].dosage").value("1000mg"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // Nutrition endpoints
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Nutrition endpoints")
    class NutritionEndpoints {

        @Test
        @DisplayName("POST /{logId}/nutrition — devrait ajouter une entrée nutrition")
        void addNutrition() throws Exception {
            NutritionEntryRequest dto = new NutritionEntryRequest(
                    "BREAKFAST", "Tartine", "COMPLET", "BON", 250, "RAS", "08:00");

            NutritionEntry saved = new NutritionEntry();
            saved.setId(10L);
            saved.setMealType("BREAKFAST");
            saved.setDescription("Tartine");
            saved.setQuantity("COMPLET");
            saved.setAppetite("BON");
            saved.setHydrationMl(250);
            when(svc.addNutrition(eq(1L), any(NutritionEntryRequest.class))).thenReturn(saved);

            mockMvc.perform(post("/api/daily-monitoring/{logId}/nutrition", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.mealType").value("BREAKFAST"))
                    .andExpect(jsonPath("$.hydrationMl").value(250));
        }

        @Test
        @DisplayName("PUT /{logId}/nutrition/{id} — devrait mettre à jour une nutrition")
        void updateNutrition() throws Exception {
            NutritionEntryRequest dto = new NutritionEntryRequest(
                    "LUNCH", "Couscous", "DEMI", "MOYEN", 300, null, "12:30");

            NutritionEntry updated = new NutritionEntry();
            updated.setId(10L);
            updated.setMealType("LUNCH");
            updated.setDescription("Couscous");
            updated.setQuantity("DEMI");
            when(svc.updateNutrition(eq(1L), eq(10L), any())).thenReturn(updated);

            mockMvc.perform(put("/api/daily-monitoring/{logId}/nutrition/{id}", 1L, 10L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mealType").value("LUNCH"));
        }

        @Test
        @DisplayName("DELETE /{logId}/nutrition/{id} — devrait supprimer")
        void deleteNutrition() throws Exception {
            mockMvc.perform(delete("/api/daily-monitoring/{logId}/nutrition/{id}", 1L, 10L))
                    .andExpect(status().isNoContent());

            verify(svc).deleteNutrition(1L, 10L);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Medication Intake endpoints
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Medication Intake endpoints")
    class MedicationIntakeEndpoints {

        @Test
        @DisplayName("POST /{logId}/medication-intakes — devrait ajouter une prise")
        void addMedicationIntake() throws Exception {
            MedicationIntakeLogRequest dto = new MedicationIntakeLogRequest(100L, "08:30", "PRIS", "Avec eau");

            Medication med = new Medication();
            med.setId(100L);
            med.setMedicationName("Doliprane");
            med.setDosage("1000mg");
            med.setFrequency("2x/jour");

            MedicationIntakeLog saved = new MedicationIntakeLog();
            saved.setId(20L);
            saved.setMedication(med);
            saved.setStatus("PRIS");
            saved.setTakenAt("08:30");
            when(svc.addMedicationIntake(eq(1L), any())).thenReturn(saved);

            mockMvc.perform(post("/api/daily-monitoring/{logId}/medication-intakes", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(20))
                    .andExpect(jsonPath("$.medicationName").value("Doliprane"))
                    .andExpect(jsonPath("$.status").value("PRIS"));
        }

        @Test
        @DisplayName("DELETE /{logId}/medication-intakes/{id} — devrait supprimer")
        void deleteMedicationIntake() throws Exception {
            mockMvc.perform(delete("/api/daily-monitoring/{logId}/medication-intakes/{id}", 1L, 20L))
                    .andExpect(status().isNoContent());

            verify(svc).deleteMedicationIntake(1L, 20L);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Activity endpoints
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Activity endpoints")
    class ActivityEndpoints {

        @Test
        @DisplayName("POST /{logId}/activities — devrait ajouter une activité")
        void addActivity() throws Exception {
            ActivityEntryRequest dto = new ActivityEntryRequest(
                    "PHYSIQUE", "Marche", 30, "MODERE", null, "10:00");

            ActivityEntry saved = new ActivityEntry();
            saved.setId(30L);
            saved.setActivityType("PHYSIQUE");
            saved.setDescription("Marche");
            saved.setDurationMinutes(30);
            saved.setIntensity("MODERE");
            when(svc.addActivity(eq(1L), any())).thenReturn(saved);

            mockMvc.perform(post("/api/daily-monitoring/{logId}/activities", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(30))
                    .andExpect(jsonPath("$.activityType").value("PHYSIQUE"))
                    .andExpect(jsonPath("$.durationMinutes").value(30));
        }

        @Test
        @DisplayName("DELETE /{logId}/activities/{id} — devrait supprimer")
        void deleteActivity() throws Exception {
            mockMvc.perform(delete("/api/daily-monitoring/{logId}/activities/{id}", 1L, 30L))
                    .andExpect(status().isNoContent());

            verify(svc).deleteActivity(1L, 30L);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Incident endpoints
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Incident endpoints")
    class IncidentEndpoints {

        @Test
        @DisplayName("POST /{logId}/incidents — devrait ajouter un incident")
        void addIncident() throws Exception {
            IncidentEntryRequest dto = new IncidentEntryRequest(
                    "CHUTE", "Chute salon", "GRAVE", "Salon", "Glace", "Hématome", "14:30");

            IncidentEntry saved = new IncidentEntry();
            saved.setId(40L);
            saved.setIncidentType("CHUTE");
            saved.setDescription("Chute salon");
            saved.setSeverity("GRAVE");
            saved.setLocation("Salon");
            when(svc.addIncident(eq(1L), any())).thenReturn(saved);

            mockMvc.perform(post("/api/daily-monitoring/{logId}/incidents", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(40))
                    .andExpect(jsonPath("$.incidentType").value("CHUTE"))
                    .andExpect(jsonPath("$.severity").value("GRAVE"));
        }

        @Test
        @DisplayName("DELETE /{logId}/incidents/{id} — devrait supprimer")
        void deleteIncident() throws Exception {
            mockMvc.perform(delete("/api/daily-monitoring/{logId}/incidents/{id}", 1L, 40L))
                    .andExpect(status().isNoContent());

            verify(svc).deleteIncident(1L, 40L);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Voice Note endpoints
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Voice Note endpoints")
    class VoiceNoteEndpoints {

        @Test
        @DisplayName("DELETE /{logId}/voice-note — devrait supprimer la note vocale")
        void deleteVoiceNote() throws Exception {
            mockMvc.perform(delete("/api/daily-monitoring/{logId}/voice-note", 1L))
                    .andExpect(status().isNoContent());

            verify(svc).updateVoiceNote(1L, null);
        }
    }
}
