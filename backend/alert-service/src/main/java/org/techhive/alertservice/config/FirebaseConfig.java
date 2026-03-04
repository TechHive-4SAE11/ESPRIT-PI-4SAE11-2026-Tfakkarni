package org.techhive.alertservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.config-file}")
    private Resource firebaseConfigFile;

    @PostConstruct
    public void initializeFirebase() {
        try {
            if (firebaseConfigFile.exists()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(firebaseConfigFile.getInputStream()))
                        .build();

                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options);
                    log.info("✅ Firebase initialized successfully");
                }
            } else {
                log.warn("⚠️ Firebase config file not found at: {}. Push notifications will be disabled.", 
                        firebaseConfigFile.getDescription());
                log.warn("⚠️ To enable push notifications, place your Firebase service account JSON at: src/main/resources/firebase-service-account.json");
            }
        } catch (IOException e) {
            log.error("❌ Failed to initialize Firebase: {}", e.getMessage());
            log.warn("⚠️ Push notifications will be disabled. REST API for notifications will still work.");
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                return FirebaseMessaging.getInstance();
            }
        } catch (Exception e) {
            log.warn("Firebase Messaging not available: {}", e.getMessage());
        }
        return null;
    }
}
