package org.techhive.medicalservice.service.ai;

import org.techhive.medicalservice.dto.SlotSuggestionDTO;

import java.util.List;

public interface SuggestionService {

    List<SlotSuggestionDTO> suggestSlots(Long appointmentId, int numberOfSuggestions);
}

