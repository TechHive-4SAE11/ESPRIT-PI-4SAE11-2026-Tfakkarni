package org.techhive.alertservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.techhive.alertservice.repository.AppointmentReminderRepository;
import org.techhive.alertservice.repository.GeofenceAlertRepository;
import org.techhive.alertservice.repository.IotAlertRepository;
import org.techhive.alertservice.repository.SafeZoneRepository;

@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "keycloak.enabled=false",
    "firebase.config-file=src/test/resources/firebase-test.json",
    "mailtrap.token=dummy-token",
    "mailtrap.inbox-id=dummy-inbox",
    "mailtrap.from=noreply@example.test",
    "telegram.bot-token=dummy-telegram-token",
    "telegram.default-chat-id=0",
    "tracking-service.url=http://localhost:8081",
    "notification.scheduler.medication-reminder-cron=0 0 0 * * *"
})
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    SecurityAutoConfiguration.class
})
class AlertServiceApplicationTests {
    @MockBean
    private AppointmentReminderRepository appointmentReminderRepository;

    @MockBean
    private GeofenceAlertRepository geofenceAlertRepository;

    @MockBean
    private IotAlertRepository iotAlertRepository;

    @MockBean
    private SafeZoneRepository safeZoneRepository;

    @Test
    void contextLoads() {
    }
}
