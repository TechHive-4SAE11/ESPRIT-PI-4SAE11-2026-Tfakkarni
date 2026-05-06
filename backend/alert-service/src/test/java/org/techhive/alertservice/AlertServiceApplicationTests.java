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
        "spring.datasource.url=jdbc:h2:mem:alert_service_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;NON_KEYWORDS=VALUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cache.type=simple",
        "spring.data.redis.repositories.enabled=false",
        "spring.quartz.job-store-type=memory",
        "firebase.config-file=classpath:missing-firebase-test.json",
        "tracking-service.url=http://localhost:0"
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
