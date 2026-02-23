package org.techhive.trackingservice.enums;

/**
 * Enum representing the status of a prescribed medication based on treatment timeline
 */
public enum MedicationStatus {
    /** Medication is currently active and patient should be taking it */
    ACTIVE("Actif"),
    
    /** Medication treatment period has ended */
    EXPIRED("Expiré"),
    
    /** Medication has been discontinued by doctor before completion */
    DISCONTINUED("Arrêté"),
    
    /** Medication is ongoing/long-term treatment with no end date */
    ONGOING("En cours - longue durée");

    private final String displayName;

    MedicationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
