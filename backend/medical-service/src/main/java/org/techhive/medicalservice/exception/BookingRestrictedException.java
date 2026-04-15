package org.techhive.medicalservice.exception;

public class BookingRestrictedException extends RuntimeException {

    public BookingRestrictedException(String message) {
        super(message);
    }
}
