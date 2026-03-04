package org.techhive.mlservice.service.guardian;

import java.util.Base64;
import java.util.List;

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
public class MlGuardianMessagingService {

    private final MlGuardianTwilioProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String TWILIO_MESSAGING_URL = "https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json";

    public void sendWhatsAppFollowUp(String condition, String patientPhone) {
        if (isConfigMissing())
            return;

        String url = String.format(TWILIO_MESSAGING_URL, properties.getAccountSid());

        String message = "Hello from Tfakkarni Co-Pilot. Following your assessment for " + condition
                + ", we've scheduled a follow-up. Please ensure you are monitoring your symptoms. Reply HELP if you need assistance.";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("To", "whatsapp:" + patientPhone);
        body.add("From", "whatsapp:" + properties.getWhatsappNumber());
        body.add("Body", message);

        postMessage(url, body, "WhatsApp follow-up");
    }

    public void sendWhatsAppTemplateMessage(String toPhone, String contentSid, String contentVariables) {
        if (isConfigMissing())
            return;

        String url = String.format(TWILIO_MESSAGING_URL, properties.getAccountSid());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("To", "whatsapp:" + toPhone);
        body.add("From", "whatsapp:" + properties.getWhatsappNumber());
        body.add("ContentSid", contentSid);
        body.add("ContentVariables", contentVariables);

        postMessage(url, body, "WhatsApp Template (" + contentSid + ")");
    }

    private void postMessage(String url, MultiValueMap<String, String> body, String label) {
        try {
            HttpHeaders headers = createAuthHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            String response = restTemplate.postForObject(url, request, String.class);
            log.info("✅ {} sent successfully. Twilio response: {}", label, response);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("❌ Twilio API Error for {} — Status: {} — Body: {}", label, e.getStatusCode(),
                    e.getResponseBodyAsString());
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            log.error("❌ Twilio Server Error for {} — Status: {} — Body: {}", label, e.getStatusCode(),
                    e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("❌ Unexpected error sending {} — Message: {}", label, e.getMessage(), e);
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
