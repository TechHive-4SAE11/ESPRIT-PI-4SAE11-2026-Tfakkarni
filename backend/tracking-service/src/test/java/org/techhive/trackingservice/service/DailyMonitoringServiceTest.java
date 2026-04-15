package org.techhive.trackingservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.trackingservice.dto.*;
import org.techhive.trackingservice.entity.*;
import org.techhive.trackingservice.repository.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyMonitoringServiceTest {

    @Mock private DailyLogRepository logRepo;
    @Mock private NutritionEntryRepository nutritionRepo;
    @Mock private MedicationIntakeLogRepository medIntakeRepo;
    @Mock private MedicationRepository medicationRepo;
    @Mock private ActivityEntryRepository activityRepo;
    @Mock private IncidentEntryRepository incidentRepo;
    @Mock private IncidentAlertService incidentAlertService;

    @InjectMocks
    private DailyMonitoringService service;

    private static final String PATIENT_ID = "patient-abc-123";
    private static final LocalDate TODAY = LocalDate.of(2026, 4, 15);

    private DailyLog sampleLog;

    @BeforeEach
    void setUp() {
        sampleLog = new DailyLog();
        sampleLog.setId(1L);
        sampleLog.setPatientKeycloakId(PATIENT_ID);
        sampleLog.setLogDate(TODAY);
        sampleLog.setNutritionEntries(new ArrayList<>());
        sampleLog.setMedicationIntakes(new ArrayList<>());
        sampleLog.setActivityEntries(new ArrayList<>());
        sampleLog.setIncidentEntries(new ArrayList<>());
    }

    // ════════════════════════════════════════════════════════════════════════
    // getOrCreateLog
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getOrCreateLog")
    class GetOrCreateLog {

        @Test
        @DisplayName("devrait retourner le log existant quand il existe déjà")
        void shouldReturnExistingLog() {
            when(logRepo.findByPatientKeycloakIdAndLogDate(PATIENT_ID, TODAY))
                    .thenReturn(Optional.of(sampleLog));
            // Existing log already has medication intakes (non-empty)
            MedicationIntakeLog intake = new MedicationIntakeLog();
            intake.setId(10L);
            sampleLog.getMedicationIntakes().add(intake);

            DailyLog result = service.getOrCreateLog(PATIENT_ID, TODAY);

            assertThat(result).isEqualTo(sampleLog);
            assertThat(result.getId()).isEqualTo(1L);
            verify(logRepo).findByPatientKeycloakIdAndLogDate(PATIENT_ID, TODAY);
            verify(logRepo, never()).save(any());
        }

        @Test
        @DisplayName("devrait créer un nouveau log quand aucun n'existe")
        void shouldCreateNewLogWhenNoneExists() {
            when(logRepo.findByPatientKeycloakIdAndLogDate(PATIENT_ID, TODAY))
                    .thenReturn(Optional.empty());
            when(logRepo.save(any(DailyLog.class))).thenReturn(sampleLog);
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID))
                    .thenReturn(Collections.emptyList());

            DailyLog result = service.getOrCreateLog(PATIENT_ID, TODAY);

            assertThat(result).isNotNull();
            assertThat(result.getPatientKeycloakId()).isEqualTo(PATIENT_ID);
            verify(logRepo).save(any(DailyLog.class));
        }

        @Test
        @DisplayName("devrait auto-peupler les médicaments depuis les prescriptions actives")
        void shouldAutoPopulateMedicationsFromActivePrescriptions() {
            // New log with empty medication intakes
            when(logRepo.findByPatientKeycloakIdAndLogDate(PATIENT_ID, TODAY))
                    .thenReturn(Optional.empty());
            when(logRepo.save(any(DailyLog.class))).thenReturn(sampleLog);

            Medication med1 = new Medication();
            med1.setId(100L);
            med1.setMedicationName("Doliprane");
            Medication med2 = new Medication();
            med2.setId(101L);
            med2.setMedicationName("Aspirin");

            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID))
                    .thenReturn(List.of(med1, med2));
            when(logRepo.findById(1L)).thenReturn(Optional.of(sampleLog));

            service.getOrCreateLog(PATIENT_ID, TODAY);

            // Should save 2 medication intake entries with status OUBLIE
            verify(medIntakeRepo, times(2)).save(argThat(intake ->
                    "OUBLIE".equals(intake.getStatus())
            ));
        }

        @Test
        @DisplayName("ne devrait pas auto-peupler si le log a déjà des médicaments")
        void shouldNotAutoPopulateIfMedsAlreadyExist() {
            MedicationIntakeLog existingIntake = new MedicationIntakeLog();
            existingIntake.setId(50L);
            sampleLog.getMedicationIntakes().add(existingIntake);

            when(logRepo.findByPatientKeycloakIdAndLogDate(PATIENT_ID, TODAY))
                    .thenReturn(Optional.of(sampleLog));

            service.getOrCreateLog(PATIENT_ID, TODAY);

            verify(medicationRepo, never()).findByPrescriptionSessionMedicalFolderIdPatient(any());
            verify(medIntakeRepo, never()).save(any());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // getLogsForPatient / getLogById
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Lecture des logs")
    class ReadLogs {

        @Test
        @DisplayName("devrait retourner tous les logs d'un patient triés par date desc")
        void shouldReturnAllLogsForPatient() {
            DailyLog log2 = new DailyLog();
            log2.setId(2L);
            log2.setPatientKeycloakId(PATIENT_ID);
            log2.setLogDate(TODAY.minusDays(1));

            when(logRepo.findByPatientKeycloakIdOrderByLogDateDesc(PATIENT_ID))
                    .thenReturn(List.of(sampleLog, log2));

            List<DailyLog> result = service.getLogsForPatient(PATIENT_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getLogDate()).isEqualTo(TODAY);
            verify(logRepo).findByPatientKeycloakIdOrderByLogDateDesc(PATIENT_ID);
        }

        @Test
        @DisplayName("devrait retourner un log par ID")
        void shouldReturnLogById() {
            when(logRepo.findById(1L)).thenReturn(Optional.of(sampleLog));

            DailyLog result = service.getLogById(1L);

            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("devrait lever une exception si le log n'existe pas")
        void shouldThrowWhenLogNotFound() {
            when(logRepo.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getLogById(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Log not found");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Nutrition CRUD
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Nutrition CRUD")
    class NutritionCrud {

        private NutritionEntryRequest nutritionDto;

        @BeforeEach
        void init() {
            nutritionDto = new NutritionEntryRequest();
            nutritionDto.setMealType("BREAKFAST");
            nutritionDto.setDescription("Tartine beurre");
            nutritionDto.setQuantity("COMPLET");
            nutritionDto.setAppetite("BON");
            nutritionDto.setHydrationMl(250);
            nutritionDto.setNotes("Bien mangé");
            nutritionDto.setEntryTime("08:00");
        }

        @Test
        @DisplayName("devrait ajouter une entrée nutrition au log")
        void shouldAddNutritionEntry() {
            when(logRepo.findById(1L)).thenReturn(Optional.of(sampleLog));

            NutritionEntry saved = new NutritionEntry();
            saved.setId(10L);
            saved.setMealType("BREAKFAST");
            saved.setDescription("Tartine beurre");
            saved.setQuantity("COMPLET");
            saved.setAppetite("BON");
            saved.setHydrationMl(250);
            when(nutritionRepo.save(any(NutritionEntry.class))).thenReturn(saved);

            NutritionEntry result = service.addNutrition(1L, nutritionDto);

            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getMealType()).isEqualTo("BREAKFAST");
            assertThat(result.getQuantity()).isEqualTo("COMPLET");
            assertThat(result.getHydrationMl()).isEqualTo(250);
            verify(nutritionRepo).save(any(NutritionEntry.class));
        }

        @Test
        @DisplayName("devrait mettre à jour une entrée nutrition")
        void shouldUpdateNutritionEntry() {
            NutritionEntry existing = new NutritionEntry();
            existing.setId(10L);
            existing.setMealType("BREAKFAST");
            when(nutritionRepo.findById(10L)).thenReturn(Optional.of(existing));

            nutritionDto.setMealType("LUNCH");
            nutritionDto.setQuantity("DEMI");

            NutritionEntry updated = new NutritionEntry();
            updated.setId(10L);
            updated.setMealType("LUNCH");
            updated.setQuantity("DEMI");
            when(nutritionRepo.save(any())).thenReturn(updated);

            NutritionEntry result = service.updateNutrition(1L, 10L, nutritionDto);

            assertThat(result.getMealType()).isEqualTo("LUNCH");
            assertThat(result.getQuantity()).isEqualTo("DEMI");
        }

        @Test
        @DisplayName("devrait supprimer une entrée nutrition")
        void shouldDeleteNutritionEntry() {
            service.deleteNutrition(1L, 10L);
            verify(nutritionRepo).deleteById(10L);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Medication Intake CRUD
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Medication Intake CRUD")
    class MedicationIntakeCrud {

        private Medication medication;
        private MedicationIntakeLogRequest intakeDto;

        @BeforeEach
        void init() {
            medication = new Medication();
            medication.setId(100L);
            medication.setMedicationName("Doliprane");
            medication.setDosage("1000mg");
            medication.setFrequency("2x/jour");

            intakeDto = new MedicationIntakeLogRequest();
            intakeDto.setMedicationId(100L);
            intakeDto.setStatus("PRIS");
            intakeDto.setTakenAt("08:30");
            intakeDto.setNotes("Pris avec eau");
        }

        @Test
        @DisplayName("devrait ajouter une prise de médicament")
        void shouldAddMedicationIntake() {
            when(logRepo.findById(1L)).thenReturn(Optional.of(sampleLog));
            when(medicationRepo.findById(100L)).thenReturn(Optional.of(medication));

            MedicationIntakeLog saved = new MedicationIntakeLog();
            saved.setId(20L);
            saved.setMedication(medication);
            saved.setStatus("PRIS");
            saved.setTakenAt("08:30");
            when(medIntakeRepo.save(any(MedicationIntakeLog.class))).thenReturn(saved);

            MedicationIntakeLog result = service.addMedicationIntake(1L, intakeDto);

            assertThat(result.getId()).isEqualTo(20L);
            assertThat(result.getStatus()).isEqualTo("PRIS");
            assertThat(result.getMedication().getMedicationName()).isEqualTo("Doliprane");
        }

        @Test
        @DisplayName("devrait lever une exception si le médicament n'existe pas")
        void shouldThrowWhenMedicationNotFound() {
            when(logRepo.findById(1L)).thenReturn(Optional.of(sampleLog));
            when(medicationRepo.findById(999L)).thenReturn(Optional.empty());

            intakeDto.setMedicationId(999L);

            assertThatThrownBy(() -> service.addMedicationIntake(1L, intakeDto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Medication not found");
        }

        @Test
        @DisplayName("devrait mettre à jour le statut d'une prise")
        void shouldUpdateMedicationIntakeStatus() {
            MedicationIntakeLog existing = new MedicationIntakeLog();
            existing.setId(20L);
            existing.setMedication(medication);
            existing.setStatus("OUBLIE");
            when(medIntakeRepo.findById(20L)).thenReturn(Optional.of(existing));
            when(medicationRepo.findById(100L)).thenReturn(Optional.of(medication));

            intakeDto.setStatus("PRIS");
            intakeDto.setTakenAt("09:00");

            MedicationIntakeLog updated = new MedicationIntakeLog();
            updated.setId(20L);
            updated.setMedication(medication);
            updated.setStatus("PRIS");
            updated.setTakenAt("09:00");
            when(medIntakeRepo.save(any())).thenReturn(updated);

            MedicationIntakeLog result = service.updateMedicationIntake(1L, 20L, intakeDto);

            assertThat(result.getStatus()).isEqualTo("PRIS");
            assertThat(result.getTakenAt()).isEqualTo("09:00");
        }

        @Test
        @DisplayName("devrait supprimer une prise de médicament")
        void shouldDeleteMedicationIntake() {
            service.deleteMedicationIntake(1L, 20L);
            verify(medIntakeRepo).deleteById(20L);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Activity CRUD
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Activity CRUD")
    class ActivityCrud {

        private ActivityEntryRequest activityDto;

        @BeforeEach
        void init() {
            activityDto = new ActivityEntryRequest();
            activityDto.setActivityType("PHYSIQUE");
            activityDto.setDescription("Marche dans le jardin");
            activityDto.setDurationMinutes(30);
            activityDto.setIntensity("MODERE");
            activityDto.setStartTime("10:00");
        }

        @Test
        @DisplayName("devrait ajouter une activité")
        void shouldAddActivity() {
            when(logRepo.findById(1L)).thenReturn(Optional.of(sampleLog));

            ActivityEntry saved = new ActivityEntry();
            saved.setId(30L);
            saved.setActivityType("PHYSIQUE");
            saved.setDescription("Marche dans le jardin");
            saved.setDurationMinutes(30);
            saved.setIntensity("MODERE");
            when(activityRepo.save(any(ActivityEntry.class))).thenReturn(saved);

            ActivityEntry result = service.addActivity(1L, activityDto);

            assertThat(result.getId()).isEqualTo(30L);
            assertThat(result.getActivityType()).isEqualTo("PHYSIQUE");
            assertThat(result.getDurationMinutes()).isEqualTo(30);
        }

        @Test
        @DisplayName("devrait mettre à jour une activité")
        void shouldUpdateActivity() {
            ActivityEntry existing = new ActivityEntry();
            existing.setId(30L);
            when(activityRepo.findById(30L)).thenReturn(Optional.of(existing));

            activityDto.setDurationMinutes(45);
            activityDto.setIntensity("ELEVE");

            ActivityEntry updated = new ActivityEntry();
            updated.setId(30L);
            updated.setDurationMinutes(45);
            updated.setIntensity("ELEVE");
            when(activityRepo.save(any())).thenReturn(updated);

            ActivityEntry result = service.updateActivity(1L, 30L, activityDto);

            assertThat(result.getDurationMinutes()).isEqualTo(45);
            assertThat(result.getIntensity()).isEqualTo("ELEVE");
        }

        @Test
        @DisplayName("devrait supprimer une activité")
        void shouldDeleteActivity() {
            service.deleteActivity(1L, 30L);
            verify(activityRepo).deleteById(30L);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Incident CRUD + Alert Trigger
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Incident CRUD")
    class IncidentCrud {

        private IncidentEntryRequest incidentDto;

        @BeforeEach
        void init() {
            incidentDto = new IncidentEntryRequest();
            incidentDto.setIncidentType("CHUTE");
            incidentDto.setDescription("Chute dans le salon");
            incidentDto.setSeverity("GRAVE");
            incidentDto.setLocation("Salon");
            incidentDto.setActionTaken("Glace appliquée");
            incidentDto.setInjuryDetails("Hématome genou");
            incidentDto.setOccurredAt("14:30");
        }

        @Test
        @DisplayName("devrait ajouter un incident et déclencher une alerte pour sévérité GRAVE")
        void shouldAddIncidentAndTriggerAlertForGrave() {
            when(logRepo.findById(1L)).thenReturn(Optional.of(sampleLog));

            IncidentEntry saved = new IncidentEntry();
            saved.setId(40L);
            saved.setDailyLog(sampleLog);
            saved.setIncidentType("CHUTE");
            saved.setDescription("Chute dans le salon");
            saved.setSeverity("GRAVE");
            saved.setLocation("Salon");
            saved.setActionTaken("Glace appliquée");
            saved.setInjuryDetails("Hématome genou");
            saved.setOccurredAt("14:30");
            when(incidentRepo.save(any(IncidentEntry.class))).thenReturn(saved);

            IncidentEntry result = service.addIncident(1L, incidentDto);

            assertThat(result.getSeverity()).isEqualTo("GRAVE");
            verify(incidentAlertService).handleIncidentAlert(
                    eq(PATIENT_ID), eq("GRAVE"), eq("CHUTE"),
                    eq("Chute dans le salon"), eq("Salon"),
                    eq("Glace appliquée"), eq("Hématome genou"),
                    eq("14:30"), eq(TODAY.toString())
            );
        }

        @Test
        @DisplayName("devrait ajouter un incident MODERE et déclencher une alerte")
        void shouldTriggerAlertForModere() {
            incidentDto.setSeverity("MODERE");
            when(logRepo.findById(1L)).thenReturn(Optional.of(sampleLog));

            IncidentEntry saved = new IncidentEntry();
            saved.setId(41L);
            saved.setDailyLog(sampleLog);
            saved.setIncidentType("CHUTE");
            saved.setSeverity("MODERE");
            saved.setDescription("Chute dans le salon");
            saved.setLocation("Salon");
            saved.setActionTaken("Glace appliquée");
            saved.setInjuryDetails("Hématome genou");
            saved.setOccurredAt("14:30");
            when(incidentRepo.save(any())).thenReturn(saved);

            service.addIncident(1L, incidentDto);

            verify(incidentAlertService).handleIncidentAlert(
                    eq(PATIENT_ID), eq("MODERE"), any(), any(), any(), any(), any(), any(), any()
            );
        }

        @Test
        @DisplayName("ne devrait PAS déclencher d'alerte pour sévérité LEGER")
        void shouldNotTriggerAlertForLeger() {
            incidentDto.setSeverity("LEGER");
            when(logRepo.findById(1L)).thenReturn(Optional.of(sampleLog));

            IncidentEntry saved = new IncidentEntry();
            saved.setId(42L);
            saved.setDailyLog(sampleLog);
            saved.setIncidentType("CONFUSION");
            saved.setSeverity("LEGER");
            saved.setDescription("Confusion passagère");
            saved.setOccurredAt("15:00");
            when(incidentRepo.save(any())).thenReturn(saved);

            service.addIncident(1L, incidentDto);

            verifyNoInteractions(incidentAlertService);
        }

        @Test
        @DisplayName("devrait mettre à jour un incident")
        void shouldUpdateIncident() {
            IncidentEntry existing = new IncidentEntry();
            existing.setId(40L);
            existing.setSeverity("LEGER");
            when(incidentRepo.findById(40L)).thenReturn(Optional.of(existing));

            incidentDto.setSeverity("MODERE");
            incidentDto.setActionTaken("Appelé le médecin");

            IncidentEntry updated = new IncidentEntry();
            updated.setId(40L);
            updated.setSeverity("MODERE");
            updated.setActionTaken("Appelé le médecin");
            when(incidentRepo.save(any())).thenReturn(updated);

            IncidentEntry result = service.updateIncident(1L, 40L, incidentDto);

            assertThat(result.getSeverity()).isEqualTo("MODERE");
            assertThat(result.getActionTaken()).isEqualTo("Appelé le médecin");
        }

        @Test
        @DisplayName("devrait supprimer un incident")
        void shouldDeleteIncident() {
            service.deleteIncident(1L, 40L);
            verify(incidentRepo).deleteById(40L);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Voice Note
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Voice Note")
    class VoiceNote {

        @Test
        @DisplayName("devrait sauvegarder le texte de la note vocale")
        void shouldSaveVoiceNoteText() {
            when(logRepo.findById(1L)).thenReturn(Optional.of(sampleLog));
            when(logRepo.save(any())).thenReturn(sampleLog);

            String text = "Le patient a bien dormi cette nuit";
            String result = service.updateVoiceNote(1L, text);

            assertThat(result).isEqualTo(text);
            verify(logRepo).save(argThat(log -> text.equals(log.getVoiceNoteText())));
        }

        @Test
        @DisplayName("devrait effacer la note vocale avec null")
        void shouldDeleteVoiceNote() {
            when(logRepo.findById(1L)).thenReturn(Optional.of(sampleLog));
            when(logRepo.save(any())).thenReturn(sampleLog);

            service.updateVoiceNote(1L, null);

            verify(logRepo).save(argThat(log -> log.getVoiceNoteText() == null));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // toResponse mapper
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("toResponse mapper")
    class ToResponseMapper {

        @Test
        @DisplayName("devrait mapper un log vide correctement")
        void shouldMapEmptyLogCorrectly() {
            DailyLogResponse response = service.toResponse(sampleLog);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getPatientKeycloakId()).isEqualTo(PATIENT_ID);
            assertThat(response.getLogDate()).isEqualTo(TODAY);
            assertThat(response.getNutritionEntries()).isEmpty();
            assertThat(response.getMedicationIntakes()).isEmpty();
            assertThat(response.getActivityEntries()).isEmpty();
            assertThat(response.getIncidentEntries()).isEmpty();
        }

        @Test
        @DisplayName("devrait mapper les entrées nutrition dans la réponse")
        void shouldMapNutritionEntries() {
            NutritionEntry n = new NutritionEntry();
            n.setId(10L);
            n.setMealType("LUNCH");
            n.setDescription("Couscous");
            n.setQuantity("COMPLET");
            n.setAppetite("BON");
            n.setHydrationMl(500);
            n.setEntryTime("12:30");
            sampleLog.getNutritionEntries().add(n);

            DailyLogResponse response = service.toResponse(sampleLog);

            assertThat(response.getNutritionEntries()).hasSize(1);
            NutritionEntryResponse nr = response.getNutritionEntries().get(0);
            assertThat(nr.getMealType()).isEqualTo("LUNCH");
            assertThat(nr.getDescription()).isEqualTo("Couscous");
            assertThat(nr.getHydrationMl()).isEqualTo(500);
        }

        @Test
        @DisplayName("devrait mapper les prises de médicaments dans la réponse")
        void shouldMapMedicationIntakes() {
            Medication med = new Medication();
            med.setId(100L);
            med.setMedicationName("Doliprane");
            med.setDosage("1000mg");
            med.setFrequency("2x/jour");

            MedicationIntakeLog intake = new MedicationIntakeLog();
            intake.setId(20L);
            intake.setMedication(med);
            intake.setStatus("PRIS");
            intake.setTakenAt("08:30");
            sampleLog.getMedicationIntakes().add(intake);

            DailyLogResponse response = service.toResponse(sampleLog);

            assertThat(response.getMedicationIntakes()).hasSize(1);
            MedicationIntakeLogResponse mr = response.getMedicationIntakes().get(0);
            assertThat(mr.getMedicationName()).isEqualTo("Doliprane");
            assertThat(mr.getDosage()).isEqualTo("1000mg");
            assertThat(mr.getStatus()).isEqualTo("PRIS");
        }

        @Test
        @DisplayName("devrait mapper les activités dans la réponse")
        void shouldMapActivityEntries() {
            ActivityEntry a = new ActivityEntry();
            a.setId(30L);
            a.setActivityType("COGNITIVE");
            a.setDescription("Puzzle");
            a.setDurationMinutes(20);
            a.setIntensity("FAIBLE");
            sampleLog.getActivityEntries().add(a);

            DailyLogResponse response = service.toResponse(sampleLog);

            assertThat(response.getActivityEntries()).hasSize(1);
            assertThat(response.getActivityEntries().get(0).getActivityType()).isEqualTo("COGNITIVE");
        }

        @Test
        @DisplayName("devrait mapper les incidents dans la réponse")
        void shouldMapIncidentEntries() {
            IncidentEntry i = new IncidentEntry();
            i.setId(40L);
            i.setIncidentType("AGITATION");
            i.setDescription("Agitation nocturne");
            i.setSeverity("MODERE");
            i.setLocation("Chambre");
            sampleLog.getIncidentEntries().add(i);

            DailyLogResponse response = service.toResponse(sampleLog);

            assertThat(response.getIncidentEntries()).hasSize(1);
            IncidentEntryResponse ir = response.getIncidentEntries().get(0);
            assertThat(ir.getIncidentType()).isEqualTo("AGITATION");
            assertThat(ir.getSeverity()).isEqualTo("MODERE");
        }

        @Test
        @DisplayName("devrait mapper la note vocale et les notes globales")
        void shouldMapVoiceNoteAndGlobalNotes() {
            sampleLog.setGlobalNotes("Journée calme");
            sampleLog.setVoiceNoteText("Le patient va bien");

            DailyLogResponse response = service.toResponse(sampleLog);

            assertThat(response.getGlobalNotes()).isEqualTo("Journée calme");
            assertThat(response.getVoiceNoteText()).isEqualTo("Le patient va bien");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Available Medications
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Available Medications")
    class AvailableMedications {

        @Test
        @DisplayName("devrait retourner les médicaments disponibles pour un patient")
        void shouldReturnAvailableMedications() {
            Medication m1 = new Medication();
            m1.setId(100L);
            m1.setMedicationName("Doliprane");
            m1.setDosage("1000mg");
            m1.setFrequency("2x/jour");
            m1.setInstructions("Après repas");

            Medication m2 = new Medication();
            m2.setId(101L);
            m2.setMedicationName("Aspirin");
            m2.setDosage("500mg");

            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID))
                    .thenReturn(List.of(m1, m2));

            List<AvailableMedicationDTO> result = service.getAvailableMedications(PATIENT_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getMedicationName()).isEqualTo("Doliprane");
            assertThat(result.get(0).getDosage()).isEqualTo("1000mg");
            assertThat(result.get(1).getMedicationName()).isEqualTo("Aspirin");
        }

        @Test
        @DisplayName("devrait retourner une liste vide si aucun médicament")
        void shouldReturnEmptyWhenNoMedications() {
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID))
                    .thenReturn(Collections.emptyList());

            List<AvailableMedicationDTO> result = service.getAvailableMedications(PATIENT_ID);

            assertThat(result).isEmpty();
        }
    }
}
