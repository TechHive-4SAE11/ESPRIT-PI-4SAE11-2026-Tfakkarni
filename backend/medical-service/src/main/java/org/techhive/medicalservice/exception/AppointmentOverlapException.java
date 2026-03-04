package org.techhive.medicalservice.exception;

public class AppointmentOverlapException extends RuntimeException {
    public AppointmentOverlapException(String message) {
        super(message);
    }
}

