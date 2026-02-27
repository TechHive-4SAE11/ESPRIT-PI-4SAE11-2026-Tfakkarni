package org.techhive.medicalservice.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.techhive.medicalservice.dto.ReminderRequestDTO;
import org.techhive.medicalservice.dto.ReminderResponseDTO;

import java.util.List;

@Component
public class AlertServiceClient {

    private static final String REMINDERS_PATH = "/api/alert/appointment-reminders";

    private final RestClient alertServiceRestClient;

    public AlertServiceClient(RestClient alertServiceRestClient) {
        this.alertServiceRestClient = alertServiceRestClient;
    }

    public ReminderResponseDTO createReminder(ReminderRequestDTO request) {
        return alertServiceRestClient
                .post()
                .uri(REMINDERS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ReminderResponseDTO.class);
    }

    public List<ReminderResponseDTO> getRemindersByAppointment(Long appointmentId) {
        return alertServiceRestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(REMINDERS_PATH)
                        .queryParam("appointmentId", appointmentId)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<ReminderResponseDTO>>() {});
    }

    public ReminderResponseDTO getReminderById(Long reminderId) {
        return alertServiceRestClient
                .get()
                .uri(REMINDERS_PATH + "/{id}", reminderId)
                .retrieve()
                .body(ReminderResponseDTO.class);
    }

    public ReminderResponseDTO updateReminder(Long reminderId, ReminderRequestDTO request) {
        return alertServiceRestClient
                .put()
                .uri(REMINDERS_PATH + "/{id}", reminderId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ReminderResponseDTO.class);
    }

    public void deleteReminder(Long reminderId) {
        alertServiceRestClient
                .delete()
                .uri(REMINDERS_PATH + "/{id}", reminderId)
                .retrieve()
                .toBodilessEntity();
    }
}
