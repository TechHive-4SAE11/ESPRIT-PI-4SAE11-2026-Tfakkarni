package org.techhive.medicalservice.entity;

public enum AppointmentStatus {
    SCHEDULED,
    CONFIRMED,
    CANCELLED,
    COMPLETED,
    /** Patient did not attend (past slot); drives attendance monitoring on the medical folder. */
    NO_SHOW
}

