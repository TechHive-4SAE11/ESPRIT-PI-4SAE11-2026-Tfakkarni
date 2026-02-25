package org.techhive.trackingservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.trackingservice.dto.*;
import org.techhive.trackingservice.entity.*;
import org.techhive.trackingservice.repository.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyMonitoringService {

    private final DailyLogRepository             logRepo;
    private final NutritionEntryRepository       nutritionRepo;
    private final MedicationIntakeLogRepository  medIntakeRepo;
    private final MedicationRepository           medicationRepo;
    private final ActivityEntryRepository        activityRepo;
    private final IncidentEntryRepository        incidentRepo;

    // ── Daily log ──────────────────────────────────────────────────────────

    /**
     * Retourne le log du jour, ou le crée s'il n'existe pas.
     *
     * Si le log n'a aucun médicament enregistré, on le pré-peuple
     * automatiquement à partir des prescriptions actives du patient,
     * avec le statut « OUBLIE » (= à prendre). Cela garantit que le
     * patient voit toujours ses médicaments du jour dès l'ouverture.
     */
    @Transactional
    public DailyLog getOrCreateLog(String keycloakId, LocalDate date) {
        DailyLog log = logRepo.findByPatientKeycloakIdAndLogDate(keycloakId, date)
                .orElseGet(() -> {
                    DailyLog newLog = new DailyLog();
                    newLog.setPatientKeycloakId(keycloakId);
                    newLog.setLogDate(date);
                    return logRepo.save(newLog);
                });

        // Auto-peuplement des médicaments si le log n'en a aucun
        if (log.getMedicationIntakes() == null || log.getMedicationIntakes().isEmpty()) {
            List<Medication> meds = medicationRepo
                    .findByPrescriptionSessionMedicalFolderIdPatient(keycloakId);
            for (Medication med : meds) {
                MedicationIntakeLog intake = new MedicationIntakeLog();
                intake.setDailyLog(log);
                intake.setMedication(med);
                intake.setStatus("OUBLIE");   // statut par défaut = « à prendre »
                medIntakeRepo.save(intake);
            }
            // Recharger le log pour avoir les intakes frais en mémoire
            if (!meds.isEmpty()) {
                log = logRepo.findById(log.getId()).orElse(log);
            }
        }

        return log;
    }

    public List<DailyLog> getLogsForPatient(String keycloakId) {
        return logRepo.findByPatientKeycloakIdOrderByLogDateDesc(keycloakId);
    }

    public DailyLog getLogById(Long id) {
        return logRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Log not found: " + id));
    }

    // ── Available medications for patient ─────────────────────────────────

    public List<AvailableMedicationDTO> getAvailableMedications(String keycloakId) {
        return medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(keycloakId)
                .stream()
                .map(m -> new AvailableMedicationDTO(
                        m.getId(),
                        m.getMedicationName(),
                        m.getDosage(),
                        m.getFrequency(),
                        m.getInstructions()))
                .collect(Collectors.toList());
    }

    // ── Nutrition ──────────────────────────────────────────────────────────

    @Transactional
    public NutritionEntry addNutrition(Long logId, NutritionEntryRequest dto) {
        DailyLog log = getLogById(logId);
        NutritionEntry e = new NutritionEntry();
        e.setDailyLog(log);
        copyNutrition(dto, e);
        return nutritionRepo.save(e);
    }

    @Transactional
    public NutritionEntry updateNutrition(Long logId, Long id, NutritionEntryRequest dto) {
        NutritionEntry e = nutritionRepo.findById(id).orElseThrow();
        copyNutrition(dto, e);
        return nutritionRepo.save(e);
    }

    @Transactional
    public void deleteNutrition(Long logId, Long id) { nutritionRepo.deleteById(id); }

    private void copyNutrition(NutritionEntryRequest dto, NutritionEntry e) {
        e.setMealType(dto.getMealType());
        e.setDescription(dto.getDescription());
        e.setQuantity(dto.getQuantity());
        e.setAppetite(dto.getAppetite());
        e.setHydrationMl(dto.getHydrationMl());
        e.setNotes(dto.getNotes());
        e.setEntryTime(dto.getEntryTime());
    }

    // ── Medication intakes ────────────────────────────────────────────────

    @Transactional
    public MedicationIntakeLog addMedicationIntake(Long logId, MedicationIntakeLogRequest dto) {
        DailyLog log = getLogById(logId);
        Medication med = medicationRepo.findById(dto.getMedicationId())
                .orElseThrow(() -> new RuntimeException("Medication not found: " + dto.getMedicationId()));
        MedicationIntakeLog e = new MedicationIntakeLog();
        e.setDailyLog(log);
        e.setMedication(med);
        copyMedIntake(dto, e);
        return medIntakeRepo.save(e);
    }

    @Transactional
    public MedicationIntakeLog updateMedicationIntake(Long logId, Long id, MedicationIntakeLogRequest dto) {
        MedicationIntakeLog e = medIntakeRepo.findById(id).orElseThrow();
        if (dto.getMedicationId() != null) {
            Medication med = medicationRepo.findById(dto.getMedicationId())
                    .orElseThrow(() -> new RuntimeException("Medication not found: " + dto.getMedicationId()));
            e.setMedication(med);
        }
        copyMedIntake(dto, e);
        return medIntakeRepo.save(e);
    }

    @Transactional
    public void deleteMedicationIntake(Long logId, Long id) { medIntakeRepo.deleteById(id); }

    private void copyMedIntake(MedicationIntakeLogRequest dto, MedicationIntakeLog e) {
        e.setTakenAt(dto.getTakenAt());
        e.setStatus(dto.getStatus());
        e.setNotes(dto.getNotes());
    }

    // ── Activities ────────────────────────────────────────────────────────

    @Transactional
    public ActivityEntry addActivity(Long logId, ActivityEntryRequest dto) {
        DailyLog log = getLogById(logId);
        ActivityEntry e = new ActivityEntry();
        e.setDailyLog(log);
        copyActivity(dto, e);
        return activityRepo.save(e);
    }

    @Transactional
    public ActivityEntry updateActivity(Long logId, Long id, ActivityEntryRequest dto) {
        ActivityEntry e = activityRepo.findById(id).orElseThrow();
        copyActivity(dto, e);
        return activityRepo.save(e);
    }

    @Transactional
    public void deleteActivity(Long logId, Long id) { activityRepo.deleteById(id); }

    private void copyActivity(ActivityEntryRequest dto, ActivityEntry e) {
        e.setActivityType(dto.getActivityType());
        e.setDescription(dto.getDescription());
        e.setDurationMinutes(dto.getDurationMinutes());
        e.setIntensity(dto.getIntensity());
        e.setNotes(dto.getNotes());
        e.setStartTime(dto.getStartTime());
    }

    // ── Incidents ─────────────────────────────────────────────────────────

    @Transactional
    public IncidentEntry addIncident(Long logId, IncidentEntryRequest dto) {
        DailyLog log = getLogById(logId);
        IncidentEntry e = new IncidentEntry();
        e.setDailyLog(log);
        copyIncident(dto, e);
        return incidentRepo.save(e);
    }

    @Transactional
    public IncidentEntry updateIncident(Long logId, Long id, IncidentEntryRequest dto) {
        IncidentEntry e = incidentRepo.findById(id).orElseThrow();
        copyIncident(dto, e);
        return incidentRepo.save(e);
    }

    @Transactional
    public void deleteIncident(Long logId, Long id) { incidentRepo.deleteById(id); }

    private void copyIncident(IncidentEntryRequest dto, IncidentEntry e) {
        e.setIncidentType(dto.getIncidentType());
        e.setDescription(dto.getDescription());
        e.setSeverity(dto.getSeverity());
        e.setLocation(dto.getLocation());
        e.setActionTaken(dto.getActionTaken());
        e.setInjuryDetails(dto.getInjuryDetails());
        e.setOccurredAt(dto.getOccurredAt());
    }

    // ── Voice note ─────────────────────────────────────────────────────────

    @Transactional
    public String updateVoiceNote(Long logId, String voiceNoteText) {
        DailyLog log = getLogById(logId);
        log.setVoiceNoteText(voiceNoteText);
        logRepo.save(log);
        return voiceNoteText;
    }

    // ── Mapper ────────────────────────────────────────────────────────────

    public DailyLogResponse toResponse(DailyLog log) {
        DailyLogResponse r = new DailyLogResponse();
        r.setId(log.getId());
        r.setPatientKeycloakId(log.getPatientKeycloakId());
        r.setLogDate(log.getLogDate());
        r.setGlobalNotes(log.getGlobalNotes());
        r.setVoiceNoteText(log.getVoiceNoteText());
        r.setCreatedAt(log.getCreatedAt());
        r.setUpdatedAt(log.getUpdatedAt());

        r.setNutritionEntries(log.getNutritionEntries().stream().map(e -> {
            NutritionEntryResponse d = new NutritionEntryResponse();
            d.setId(e.getId()); d.setMealType(e.getMealType()); d.setDescription(e.getDescription());
            d.setQuantity(e.getQuantity()); d.setAppetite(e.getAppetite());
            d.setHydrationMl(e.getHydrationMl()); d.setNotes(e.getNotes()); d.setEntryTime(e.getEntryTime());
            return d;
        }).collect(Collectors.toList()));

        r.setMedicationIntakes(log.getMedicationIntakes().stream().map(e -> {
            MedicationIntakeLogResponse d = new MedicationIntakeLogResponse();
            d.setId(e.getId());
            d.setMedicationId(e.getMedication().getId());
            d.setMedicationName(e.getMedication().getMedicationName());
            d.setDosage(e.getMedication().getDosage());
            d.setFrequency(e.getMedication().getFrequency());
            d.setTakenAt(e.getTakenAt());
            d.setStatus(e.getStatus());
            d.setNotes(e.getNotes());
            return d;
        }).collect(Collectors.toList()));

        r.setActivityEntries(log.getActivityEntries().stream().map(e -> {
            ActivityEntryResponse d = new ActivityEntryResponse();
            d.setId(e.getId()); d.setActivityType(e.getActivityType()); d.setDescription(e.getDescription());
            d.setDurationMinutes(e.getDurationMinutes()); d.setIntensity(e.getIntensity());
            d.setNotes(e.getNotes()); d.setStartTime(e.getStartTime());
            return d;
        }).collect(Collectors.toList()));

        r.setIncidentEntries(log.getIncidentEntries().stream().map(e -> {
            IncidentEntryResponse d = new IncidentEntryResponse();
            d.setId(e.getId()); d.setIncidentType(e.getIncidentType()); d.setDescription(e.getDescription());
            d.setSeverity(e.getSeverity()); d.setLocation(e.getLocation());
            d.setActionTaken(e.getActionTaken()); d.setInjuryDetails(e.getInjuryDetails());
            d.setOccurredAt(e.getOccurredAt());
            return d;
        }).collect(Collectors.toList()));

        return r;
    }
}
