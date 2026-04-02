package org.techhive.mlservice.service.guardian;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "ml.twilio")
public class MlGuardianTwilioProperties {
    private String accountSid;
    private String authToken;
    private String phoneNumber;
    private String whatsappNumber;
    private String doctorPhone;
}
