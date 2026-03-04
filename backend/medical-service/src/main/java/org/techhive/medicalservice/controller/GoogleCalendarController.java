package org.techhive.medicalservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.medicalservice.dto.CalendarStatusDTO;
import org.techhive.medicalservice.service.GoogleCalendarService;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/medical/calendar")
@CrossOrigin(origins = "http://localhost:4200")
public class GoogleCalendarController {

    private final GoogleCalendarService googleCalendarService;

    public GoogleCalendarController(GoogleCalendarService googleCalendarService) {
        this.googleCalendarService = googleCalendarService;
    }

    @GetMapping("/auth-url/{doctorId}")
    public ResponseEntity<Map<String, String>> getAuthUrl(@PathVariable String doctorId) {
        String url = googleCalendarService.generateAuthUrl(doctorId);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam String code,
            @RequestParam String state) throws IOException {
        String doctorId = state;
        googleCalendarService.handleCallback(code, doctorId);
        // Redirect to Angular dashboard calendar-sync page
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "http://localhost:4200/doctor/calendar-sync")
                .build();
    }

    @GetMapping("/status/{doctorId}")
    public ResponseEntity<CalendarStatusDTO> getStatus(@PathVariable String doctorId) {
        return ResponseEntity.ok(googleCalendarService.getStatus(doctorId));
    }

    @PostMapping("/disconnect/{doctorId}")
    public ResponseEntity<Void> disconnect(@PathVariable String doctorId) {
        googleCalendarService.disconnect(doctorId);
        return ResponseEntity.ok().build();
    }
}
