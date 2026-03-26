package org.techhive.medicalservice.controller;

import org.techhive.medicalservice.dto.SlotSuggestionDTO;
import org.techhive.medicalservice.service.ai.SuggestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/medical/appointments")
@CrossOrigin(origins = "http://localhost:4200")
public class SuggestionController {

    private final SuggestionService suggestionService;

    public SuggestionController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @GetMapping("/{appointmentId}/suggestions")
    public ResponseEntity<List<SlotSuggestionDTO>> getSuggestions(
            @PathVariable Long appointmentId,
            @RequestParam(name = "count", defaultValue = "3") int count) {
        List<SlotSuggestionDTO> suggestions = suggestionService.suggestSlots(appointmentId, count);
        return ResponseEntity.ok(suggestions);
    }
}

