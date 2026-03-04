package org.techhive.mlservice.service.guardian;

import org.springframework.stereotype.Service;
import org.techhive.mlservice.dto.SymptomPilotResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MlGuardianDispatchManager {

    private final MlGuardianVoiceService voiceService;
    private final MlGuardianMessagingService messagingService;
    private final MlGuardianTwilioProperties properties;

    public void processAnalysisResults(SymptomPilotResponse response, String patientId) {
        if (response == null || response.getPredictions() == null || response.getPredictions().isEmpty()) {
            return;
        }

        SymptomPilotResponse.Prediction topPrediction = response.getPredictions().get(0);

        // CASE 1: Code Red - Critical Alert (Probability > 65%)
        if (response.isCriticalAlert() && topPrediction.getProbability() > 0.65) {
            log.info("🚨 AI Guardian: Triggering Emergency Voice Call for {}", topPrediction.getCondition());
            voiceService.makeEmergencyCall(topPrediction.getCondition(), patientId);
        }

        // CASE 2: Follow-up - Significant Prediction (Moderate or High risk)
        else if (topPrediction.getProbability() > 0.40) {
            log.info("📱 AI Guardian: Triggering WhatsApp Follow-up for {}", topPrediction.getCondition());
            // Using doctor phone from properties for trial purposes
            String trialPhone = properties.getDoctorPhone();
            messagingService.sendWhatsAppFollowUp(topPrediction.getCondition(), trialPhone);
        }
    }
}
