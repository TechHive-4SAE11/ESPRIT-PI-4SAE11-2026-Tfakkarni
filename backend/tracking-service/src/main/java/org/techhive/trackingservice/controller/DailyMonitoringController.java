package org.techhive.trackingservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.techhive.trackingservice.dto.*;
import org.techhive.trackingservice.entity.*;
import org.techhive.trackingservice.service.DailyMonitoringService;
import org.techhive.trackingservice.service.ElevenLabsService;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/daily-monitoring")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DailyMonitoringController {

    private final DailyMonitoringService svc;
    private final ElevenLabsService elevenLabsService;

    // ── Daily Log ──────────────────────────────────────────────────────────

    @PostMapping("/patient/{keycloakId}/date/{date}")
    public ResponseEntity<DailyLogResponse> getOrCreate(
            @PathVariable String keycloakId, @PathVariable String date) {
        DailyLog log = svc.getOrCreateLog(keycloakId, LocalDate.parse(date));
        return ResponseEntity.ok(svc.toResponse(log));
    }

    @GetMapping("/patient/{keycloakId}")
    public ResponseEntity<List<DailyLogResponse>> getAll(@PathVariable String keycloakId) {
        return ResponseEntity.ok(svc.getLogsForPatient(keycloakId)
                .stream().map(svc::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DailyLogResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(svc.toResponse(svc.getLogById(id)));
    }

    // ── Available Medications ──────────────────────────────────────────────

    @GetMapping("/patient/{keycloakId}/available-medications")
    public ResponseEntity<List<AvailableMedicationDTO>> getAvailableMedications(
            @PathVariable String keycloakId) {
        return ResponseEntity.ok(svc.getAvailableMedications(keycloakId));
    }

    // ── Nutrition ──────────────────────────────────────────────────────────

    @PostMapping("/{logId}/nutrition")
    public ResponseEntity<NutritionEntryResponse> addNutrition(
            @PathVariable Long logId, @RequestBody NutritionEntryRequest dto) {
        NutritionEntry e = svc.addNutrition(logId, dto);
        return ResponseEntity.ok(toNutritionResponse(e));
    }

    @PutMapping("/{logId}/nutrition/{id}")
    public ResponseEntity<NutritionEntryResponse> updateNutrition(
            @PathVariable Long logId, @PathVariable Long id, @RequestBody NutritionEntryRequest dto) {
        return ResponseEntity.ok(toNutritionResponse(svc.updateNutrition(logId, id, dto)));
    }

    @DeleteMapping("/{logId}/nutrition/{id}")
    public ResponseEntity<Void> deleteNutrition(@PathVariable Long logId, @PathVariable Long id) {
        svc.deleteNutrition(logId, id);
        return ResponseEntity.noContent().build();
    }

    // ── Medication Intakes ─────────────────────────────────────────────────

    @PostMapping("/{logId}/medication-intakes")
    public ResponseEntity<MedicationIntakeLogResponse> addMed(
            @PathVariable Long logId, @RequestBody MedicationIntakeLogRequest dto) {
        return ResponseEntity.ok(toMedIntakeResponse(svc.addMedicationIntake(logId, dto)));
    }

    @PutMapping("/{logId}/medication-intakes/{id}")
    public ResponseEntity<MedicationIntakeLogResponse> updateMed(
            @PathVariable Long logId, @PathVariable Long id, @RequestBody MedicationIntakeLogRequest dto) {
        return ResponseEntity.ok(toMedIntakeResponse(svc.updateMedicationIntake(logId, id, dto)));
    }

    @DeleteMapping("/{logId}/medication-intakes/{id}")
    public ResponseEntity<Void> deleteMed(@PathVariable Long logId, @PathVariable Long id) {
        svc.deleteMedicationIntake(logId, id);
        return ResponseEntity.noContent().build();
    }

    // ── Activities ─────────────────────────────────────────────────────────

    @PostMapping("/{logId}/activities")
    public ResponseEntity<ActivityEntryResponse> addActivity(
            @PathVariable Long logId, @RequestBody ActivityEntryRequest dto) {
        return ResponseEntity.ok(toActivityResponse(svc.addActivity(logId, dto)));
    }

    @PutMapping("/{logId}/activities/{id}")
    public ResponseEntity<ActivityEntryResponse> updateActivity(
            @PathVariable Long logId, @PathVariable Long id, @RequestBody ActivityEntryRequest dto) {
        return ResponseEntity.ok(toActivityResponse(svc.updateActivity(logId, id, dto)));
    }

    @DeleteMapping("/{logId}/activities/{id}")
    public ResponseEntity<Void> deleteActivity(@PathVariable Long logId, @PathVariable Long id) {
        svc.deleteActivity(logId, id);
        return ResponseEntity.noContent().build();
    }

    // ── Incidents ──────────────────────────────────────────────────────────

    @PostMapping("/{logId}/incidents")
    public ResponseEntity<IncidentEntryResponse> addIncident(
            @PathVariable Long logId, @RequestBody IncidentEntryRequest dto) {
        return ResponseEntity.ok(toIncidentResponse(svc.addIncident(logId, dto)));
    }

    @PutMapping("/{logId}/incidents/{id}")
    public ResponseEntity<IncidentEntryResponse> updateIncident(
            @PathVariable Long logId, @PathVariable Long id, @RequestBody IncidentEntryRequest dto) {
        return ResponseEntity.ok(toIncidentResponse(svc.updateIncident(logId, id, dto)));
    }

    @DeleteMapping("/{logId}/incidents/{id}")
    public ResponseEntity<Void> deleteIncident(@PathVariable Long logId, @PathVariable Long id) {
        svc.deleteIncident(logId, id);
        return ResponseEntity.noContent().build();
    }

    // ── Voice Note ──────────────────────────────────────────────────────

    /**
     * Receives an audio recording, transcribes it via ElevenLabs STT,
     * and persists the resulting text on the daily log.
     */
    @PostMapping(value = "/{logId}/voice-note", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadVoiceNote(
            @PathVariable Long logId,
            @RequestParam("audio") MultipartFile audioFile,
            @RequestParam(value = "language", required = false) String language) {
        try {
            if (audioFile.isEmpty()) {
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "Le fichier audio est vide"));
            }
            // Map frontend language codes to ElevenLabs ISO 639-3 codes
            String languageCode = null;
            if ("ar".equals(language)) {
                languageCode = "ara"; // Arabic (ISO 639-3)
            } else if ("fr".equals(language)) {
                languageCode = "fra"; // French
            }
            String transcribedText = elevenLabsService.transcribeAudio(audioFile, languageCode);
            svc.updateVoiceNote(logId, transcribedText);
            return ResponseEntity.ok(java.util.Map.of("text", transcribedText));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(java.util.Map.of("error", "Erreur interne lors de la transcription"));
        }
    }

    /**
     * Deletes the voice note text from a daily log.
     */
    @DeleteMapping("/{logId}/voice-note")
    public ResponseEntity<Void> deleteVoiceNote(@PathVariable Long logId) {
        svc.updateVoiceNote(logId, null);
        return ResponseEntity.noContent().build();
    }

    // ── Mini-mappers ───────────────────────────────────────────────────────

    private NutritionEntryResponse toNutritionResponse(NutritionEntry e) {
        NutritionEntryResponse r = new NutritionEntryResponse();
        r.setId(e.getId()); r.setMealType(e.getMealType()); r.setDescription(e.getDescription());
        r.setQuantity(e.getQuantity()); r.setAppetite(e.getAppetite());
        r.setHydrationMl(e.getHydrationMl()); r.setNotes(e.getNotes()); r.setEntryTime(e.getEntryTime());
        return r;
    }

    private MedicationIntakeLogResponse toMedIntakeResponse(MedicationIntakeLog e) {
        MedicationIntakeLogResponse r = new MedicationIntakeLogResponse();
        r.setId(e.getId());
        r.setMedicationId(e.getMedication().getId());
        r.setMedicationName(e.getMedication().getMedicationName());
        r.setDosage(e.getMedication().getDosage());
        r.setFrequency(e.getMedication().getFrequency());
        r.setTakenAt(e.getTakenAt());
        r.setStatus(e.getStatus());
        r.setNotes(e.getNotes());
        return r;
    }

    private ActivityEntryResponse toActivityResponse(ActivityEntry e) {
        ActivityEntryResponse r = new ActivityEntryResponse();
        r.setId(e.getId()); r.setActivityType(e.getActivityType()); r.setDescription(e.getDescription());
        r.setDurationMinutes(e.getDurationMinutes()); r.setIntensity(e.getIntensity());
        r.setNotes(e.getNotes()); r.setStartTime(e.getStartTime());
        return r;
    }

    private IncidentEntryResponse toIncidentResponse(IncidentEntry e) {
        IncidentEntryResponse r = new IncidentEntryResponse();
        r.setId(e.getId()); r.setIncidentType(e.getIncidentType()); r.setDescription(e.getDescription());
        r.setSeverity(e.getSeverity()); r.setLocation(e.getLocation());
        r.setActionTaken(e.getActionTaken()); r.setInjuryDetails(e.getInjuryDetails()); r.setOccurredAt(e.getOccurredAt());
        return r;
    }
}
