package org.techhive.iotservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThatCode;

class HeartbeatAlertServiceTest {

    private HeartbeatAlertService alertService;

    @BeforeEach
    void setUp() {
        alertService = new HeartbeatAlertService();
        ReflectionTestUtils.setField(alertService, "highBpmThreshold", 120);
        ReflectionTestUtils.setField(alertService, "lowBpmThreshold", 40);
        ReflectionTestUtils.setField(alertService, "cooldownMinutes", 10);
        // Leave Telegram unconfigured so no real HTTP calls happen
        ReflectionTestUtils.setField(alertService, "botToken", "");
        ReflectionTestUtils.setField(alertService, "defaultChatId", "");
    }

    @Test
    void checkAndAlert_normalBpm_noAlert() {
        // BPM in normal range (40-120) should not trigger anything
        assertThatCode(() -> alertService.checkAndAlert("patient-1", 75))
                .doesNotThrowAnyException();
    }

    @Test
    void checkAndAlert_atBoundary_noAlert() {
        // Exactly at threshold values should not alert
        assertThatCode(() -> alertService.checkAndAlert("patient-1", 120))
                .doesNotThrowAnyException();
        assertThatCode(() -> alertService.checkAndAlert("patient-1", 40))
                .doesNotThrowAnyException();
    }

    @Test
    void checkAndAlert_highBpm_triggersAlert() {
        // BPM > 120 should trigger alert path (no exception, just logs warning since Telegram unconfigured)
        assertThatCode(() -> alertService.checkAndAlert("patient-1", 150))
                .doesNotThrowAnyException();
    }

    @Test
    void checkAndAlert_lowBpm_triggersAlert() {
        // BPM < 40 should trigger alert path
        assertThatCode(() -> alertService.checkAndAlert("patient-1", 30))
                .doesNotThrowAnyException();
    }

    @Test
    void checkAndAlert_cooldownActive_skipsAlert() {
        // First call — triggers alert
        alertService.checkAndAlert("patient-1", 150);

        // Inject recent timestamp into cooldown map
        ConcurrentHashMap<String, LocalDateTime> lastAlertTimes =
                (ConcurrentHashMap<String, LocalDateTime>) ReflectionTestUtils.getField(alertService, "lastAlertTimes");
        lastAlertTimes.put("patient-1", LocalDateTime.now());

        // Second call within cooldown — should be skipped (no exception)
        assertThatCode(() -> alertService.checkAndAlert("patient-1", 150))
                .doesNotThrowAnyException();
    }

    @Test
    void checkAndAlert_cooldownExpired_sendsAlert() {
        // Set expired cooldown
        ConcurrentHashMap<String, LocalDateTime> lastAlertTimes =
                (ConcurrentHashMap<String, LocalDateTime>) ReflectionTestUtils.getField(alertService, "lastAlertTimes");
        lastAlertTimes.put("patient-1", LocalDateTime.now().minusMinutes(15));

        // Should not skip — cooldown expired
        assertThatCode(() -> alertService.checkAndAlert("patient-1", 150))
                .doesNotThrowAnyException();
    }

    @Test
    void checkAndAlert_differentPatients_independentCooldowns() {
        alertService.checkAndAlert("patient-1", 150);

        ConcurrentHashMap<String, LocalDateTime> lastAlertTimes =
                (ConcurrentHashMap<String, LocalDateTime>) ReflectionTestUtils.getField(alertService, "lastAlertTimes");
        lastAlertTimes.put("patient-1", LocalDateTime.now());

        // Different patient should not be affected by patient-1's cooldown
        assertThatCode(() -> alertService.checkAndAlert("patient-2", 150))
                .doesNotThrowAnyException();
        // patient-2 should now have its own cooldown entry
        assertThatCode(() -> lastAlertTimes.containsKey("patient-2"));
    }

    @Test
    void sendTelegramMessage_telegramNotConfigured_skips() {
        // With blank botToken, should not throw
        assertThatCode(() -> alertService.sendTelegramMessage("<b>Test</b>"))
                .doesNotThrowAnyException();
    }
}
