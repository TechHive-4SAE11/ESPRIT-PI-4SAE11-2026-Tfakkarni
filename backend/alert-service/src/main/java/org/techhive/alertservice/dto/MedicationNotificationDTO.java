package org.techhive.alertservice.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents a medication reminder notification stored in Redis
 */
public class MedicationNotificationDTO implements Serializable {
    private String id;
    private String patientId;
    private Long medicationId;
    private String medicationName;
    private String dosage;
    private String frequency;
    private String instructions;
    private String status; // ACTIVE, ONGOING
    private boolean read;
    private boolean pushed; // whether Firebase push was sent
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private String type; // MEDICATION_REMINDER

    // Default constructor
    public MedicationNotificationDTO() {
    }

    // All-args constructor
    public MedicationNotificationDTO(String id, String patientId, Long medicationId, String medicationName,
                                     String dosage, String frequency, String instructions, String status,
                                     boolean read, boolean pushed, LocalDateTime createdAt, LocalDateTime readAt,
                                     String type) {
        this.id = id;
        this.patientId = patientId;
        this.medicationId = medicationId;
        this.medicationName = medicationName;
        this.dosage = dosage;
        this.frequency = frequency;
        this.instructions = instructions;
        this.status = status;
        this.read = read;
        this.pushed = pushed;
        this.createdAt = createdAt;
        this.readAt = readAt;
        this.type = type;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public Long getMedicationId() {
        return medicationId;
    }

    public void setMedicationId(Long medicationId) {
        this.medicationId = medicationId;
    }

    public String getMedicationName() {
        return medicationName;
    }

    public void setMedicationName(String medicationName) {
        this.medicationName = medicationName;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean isPushed() {
        return pushed;
    }

    public void setPushed(boolean pushed) {
        this.pushed = pushed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String patientId;
        private Long medicationId;
        private String medicationName;
        private String dosage;
        private String frequency;
        private String instructions;
        private String status;
        private boolean read;
        private boolean pushed;
        private LocalDateTime createdAt;
        private LocalDateTime readAt;
        private String type;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder patientId(String patientId) {
            this.patientId = patientId;
            return this;
        }

        public Builder medicationId(Long medicationId) {
            this.medicationId = medicationId;
            return this;
        }

        public Builder medicationName(String medicationName) {
            this.medicationName = medicationName;
            return this;
        }

        public Builder dosage(String dosage) {
            this.dosage = dosage;
            return this;
        }

        public Builder frequency(String frequency) {
            this.frequency = frequency;
            return this;
        }

        public Builder instructions(String instructions) {
            this.instructions = instructions;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder read(boolean read) {
            this.read = read;
            return this;
        }

        public Builder pushed(boolean pushed) {
            this.pushed = pushed;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder readAt(LocalDateTime readAt) {
            this.readAt = readAt;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public MedicationNotificationDTO build() {
            return new MedicationNotificationDTO(id, patientId, medicationId, medicationName, dosage,
                    frequency, instructions, status, read, pushed, createdAt, readAt, type);
        }
    }
}
