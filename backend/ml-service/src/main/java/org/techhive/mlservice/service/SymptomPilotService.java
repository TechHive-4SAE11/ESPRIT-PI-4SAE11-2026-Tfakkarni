package org.techhive.mlservice.service;

import org.techhive.mlservice.dto.SymptomPilotRequest;
import org.techhive.mlservice.dto.SymptomPilotResponse;

public interface SymptomPilotService {
    SymptomPilotResponse analyzeSymptoms(SymptomPilotRequest request);
}
