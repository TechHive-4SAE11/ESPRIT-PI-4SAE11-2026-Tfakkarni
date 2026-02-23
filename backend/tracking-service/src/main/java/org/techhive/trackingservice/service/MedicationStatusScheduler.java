package org.techhive.trackingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.trackingservice.entity.Medication;
import org.techhive.trackingservice.enums.MedicationStatus;
import org.techhive.trackingservice.repository.MedicationRepository;
import org.techhive.trackingservice.util.DurationParser;

import java.time.LocalDate;
import java.util.List;

/**
 * Service that runs scheduled tasks to automatically update medication statuses
 * based on prescription dates and durations.
 * 
 * Cron Jobs:
 * - Calculate and set end dates for medications without them
 * - Update medication status (ACTIVE/EXPIRED/ONGOING) based on current date
 * - Daily check at midnight
 * - Hourly check for real-time updates
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MedicationStatusScheduler {

    private final MedicationRepository medicationRepository;

    /**
     * Runs once at startup to initialize end dates for all medications
     */
    @Transactional
    public void initializeMedicationDates() {
        log.info("Initializing medication end dates...");
        
        List<Medication> medications = medicationRepository.findAll();
        int updated = 0;
        
        for (Medication medication : medications) {
            if (medication.getEndDate() == null && medication.getDuration() != null) {
                LocalDate startDate = medication.getStartDate();
                if (startDate == null && medication.getPrescription() != null 
                        && medication.getPrescription().getSession() != null) {
                    startDate = medication.getPrescription().getSession().getSessionDate().toLocalDate();
                    medication.setStartDate(startDate);
                }
                
                if (startDate != null) {
                    LocalDate endDate = DurationParser.calculateEndDate(startDate, medication.getDuration());
                    medication.setEndDate(endDate);
                    updated++;
                }
            }
        }
        
        if (updated > 0) {
            medicationRepository.saveAll(medications);
            log.info("Initialized end dates for {} medications", updated);
        }
    }

    /**
     * Runs every minute for testing purposes
     * 
     * Cron: 0 * * * * * = At second 0 of every minute
     * TODO: Change back to "0 0 0 * * *" (midnight) for production
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void dailyMedicationStatusUpdate() {
        log.info("[TEST MODE] Starting medication status update at {}", LocalDate.now());
        updateAllMedicationStatuses();
    }

    /**
     * DISABLED FOR TESTING - Runs every 6 hours for more frequent updates
     * 
     * Cron: 0 0 (star/6) * * * = At minute 0 of every 6th hour
     * TODO: Re-enable for production by uncommenting @Scheduled
     */
    // @Scheduled(cron = "0 0 */6 * * *")
    @Transactional
    public void periodicMedicationStatusUpdate() {
        log.info("Starting periodic medication status update");
        updateAllMedicationStatuses();
    }

    /**
     * Core method to update all medication statuses
     */
    @Transactional
    public void updateAllMedicationStatuses() {
        List<Medication> medications = medicationRepository.findAll();
        LocalDate today = LocalDate.now();
        
        int activeCount = 0;
        int expiredCount = 0;
        int ongoingCount = 0;
        int statusChanged = 0;
        
        for (Medication medication : medications) {
            // Skip discontinued medications
            if (medication.getStatus() == MedicationStatus.DISCONTINUED) {
                continue;
            }

            // Ensure start date is set
            if (medication.getStartDate() == null && medication.getPrescription() != null 
                    && medication.getPrescription().getSession() != null) {
                medication.setStartDate(medication.getPrescription().getSession().getSessionDate().toLocalDate());
            }

            // Calculate end date if not set
            if (medication.getEndDate() == null && medication.getDuration() != null 
                    && medication.getStartDate() != null) {
                LocalDate endDate = DurationParser.calculateEndDate(
                    medication.getStartDate(), 
                    medication.getDuration()
                );
                medication.setEndDate(endDate);
            }

            // Determine new status
            MedicationStatus oldStatus = medication.getStatus();
            MedicationStatus newStatus = DurationParser.determineStatus(
                medication.getStartDate(),
                medication.getEndDate(),
                today
            );

            // Update status if changed
            if (oldStatus != newStatus) {
                medication.setStatus(newStatus);
                statusChanged++;
                
                log.info("Medication {} status changed: {} → {} (Patient: {}, Duration: {})", 
                    medication.getMedicationName(),
                    oldStatus,
                    newStatus,
                    medication.getPrescription() != null 
                        && medication.getPrescription().getSession() != null
                        && medication.getPrescription().getSession().getMedicalFolder() != null
                        ? medication.getPrescription().getSession().getMedicalFolder().getIdPatient()
                        : "Unknown",
                    medication.getDuration()
                );
            }

            // Count statuses
            switch (medication.getStatus()) {
                case ACTIVE:
                    activeCount++;
                    break;
                case EXPIRED:
                    expiredCount++;
                    break;
                case ONGOING:
                    ongoingCount++;
                    break;
            }
        }

        if (statusChanged > 0) {
            medicationRepository.saveAll(medications);
        }

        log.info("Medication status update completed:");
        log.info("  Active: {}", activeCount);
        log.info("  Expired: {}", expiredCount);
        log.info("  Ongoing: {}", ongoingCount);
        log.info("  Status changes: {}", statusChanged);
    }

    /**
     * Manually discontinue a medication (can be called from service layer)
     */
    @Transactional
    public void discontinueMedication(Long medicationId, String reason) {
        Medication medication = medicationRepository.findById(medicationId)
            .orElseThrow(() -> new RuntimeException("Medication not found: " + medicationId));
        
        medication.setStatus(MedicationStatus.DISCONTINUED);
        medication.setEndDate(LocalDate.now());
        medication.setInstructions(
            (medication.getInstructions() != null ? medication.getInstructions() + "\n\n" : "") +
            "Discontinued on " + LocalDate.now() + ". Reason: " + reason
        );
        
        medicationRepository.save(medication);
        log.info("Medication {} discontinued: {}", medication.getMedicationName(), reason);
    }

    /**
     * Get statistics about medication statuses
     */
    public MedicationStatusStats getStatusStatistics() {
        List<Medication> medications = medicationRepository.findAll();
        
        long activeCount = medications.stream()
            .filter(m -> m.getStatus() == MedicationStatus.ACTIVE)
            .count();
        long expiredCount = medications.stream()
            .filter(m -> m.getStatus() == MedicationStatus.EXPIRED)
            .count();
        long ongoingCount = medications.stream()
            .filter(m -> m.getStatus() == MedicationStatus.ONGOING)
            .count();
        long discontinuedCount = medications.stream()
            .filter(m -> m.getStatus() == MedicationStatus.DISCONTINUED)
            .count();
        
        return new MedicationStatusStats(
            medications.size(),
            activeCount,
            expiredCount,
            ongoingCount,
            discontinuedCount
        );
    }

    /**
     * DTO for medication status statistics
     */
    public record MedicationStatusStats(
        long total,
        long active,
        long expired,
        long ongoing,
        long discontinued
    ) {}
}
