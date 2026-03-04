package org.techhive.mlservice.service.guardian;

import java.util.Base64;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MlGuardianVoiceService {

    private final MlGuardianTwilioProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String TWILIO_CALL_URL = "https://api.twilio.com/2010-04-01/Accounts/%s/Calls.json";

    public void makeEmergencyCall(String condition, String patientId) {
        if (isConfigMissing())
            return;

        String url = String.format(TWILIO_CALL_URL, properties.getAccountSid());

        // Prepare TwiML logic - For a quick demo, we use a public TwiML Echo service or
        // TwiML Bin
        // In a real app, this would point to our own MLGuardianController endpoint
        String message = "Emergency Alert. Potential " + condition + " detected for Patient " + patientId
                + ". Please intervene immediately.";
        String twiml = "<Response><Say voice='alice'>" + message + "</Say></Response>";

        // Using Twimlets to host the TwiML dynamically without needing a public tunnel
        // for this demo
        String echoUrl = "https://twimlets.com/echo?Twiml="
                + twiml.replace(" ", "%20").replace("<", "%3C").replace(">", "%3E");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("To", properties.getDoctorPhone());
        body.add("From", properties.getPhoneNumber());
        body.add("Url", echoUrl);

        try {
            HttpHeaders headers = createAuthHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            String response = restTemplate.postForObject(url, request, String.class);
            log.info("✅ Emergency call triggered for condition: {} — Twilio: {}", condition, response);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("❌ Twilio Voice API Error — Status: {} — Body: {}", e.getStatusCode(),
                    e.getResponseBodyAsString());
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            log.error("❌ Twilio Voice Server Error — Status: {} — Body: {}", e.getStatusCode(),
                    e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("❌ Unexpected error triggering voice call — Message: {}", e.getMessage(), e);
        }
    }

    private HttpHeaders createAuthHeaders() {
        String auth = properties.getAccountSid() + ":" + properties.getAuthToken();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encodedAuth);
        return headers;
    }

    private boolean isConfigMissing() {
        return properties.getAccountSid() == null || properties.getAccountSid().contains("xxxx");
    }
}
