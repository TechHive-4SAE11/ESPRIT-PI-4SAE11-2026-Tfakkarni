package org.techhive.medicalservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class RecurringAppointmentRequestDTO {

    @Valid
    private AppointmentRequestDTO appointmentRequest;

    @NotBlank
    private String frequency; // DAILY, WEEKLY, MONTHLY

    @Min(1)
    @Max(52)
    private int numberOfOccurrences;

    public AppointmentRequestDTO getAppointmentRequest() {
        return appointmentRequest;
    }

    public void setAppointmentRequest(AppointmentRequestDTO appointmentRequest) {
        this.appointmentRequest = appointmentRequest;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public int getNumberOfOccurrences() {
        return numberOfOccurrences;
    }

    public void setNumberOfOccurrences(int numberOfOccurrences) {
        this.numberOfOccurrences = numberOfOccurrences;
    }
}

