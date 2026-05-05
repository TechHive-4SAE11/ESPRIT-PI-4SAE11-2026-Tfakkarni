package org.techhive.trackingservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.techhive.trackingservice.repository.ActivityEntryRepository;
import org.techhive.trackingservice.repository.CareActivityRepository;
import org.techhive.trackingservice.repository.CarePlanRepository;
import org.techhive.trackingservice.repository.DailyLogRepository;
import org.techhive.trackingservice.repository.DoctorNotificationRepository;
import org.techhive.trackingservice.repository.DoctorRatingRepository;
import org.techhive.trackingservice.repository.FollowUpReminderRepository;
import org.techhive.trackingservice.repository.IncidentEntryRepository;
import org.techhive.trackingservice.repository.MedicalFolderRepository;
import org.techhive.trackingservice.repository.MedicalMeetingRepository;
import org.techhive.trackingservice.repository.MedicationIntakeEntryRepository;
import org.techhive.trackingservice.repository.MedicationIntakeLogRepository;
import org.techhive.trackingservice.repository.MedicationRepository;
import org.techhive.trackingservice.repository.NutritionEntryRepository;
import org.techhive.trackingservice.repository.PatientAnswerRepository;
import org.techhive.trackingservice.repository.PrescriptionRepository;
import org.techhive.trackingservice.repository.PrescriptionTemplateRepository;
import org.techhive.trackingservice.repository.QuestionRepository;
import org.techhive.trackingservice.repository.QuestionnaireRepository;
import org.techhive.trackingservice.repository.SessionRepository;

@ActiveProfiles("test")
@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "keycloak.enabled=false",
    "elevenlabs.api.key=dummy-elevenlabs-key",
    "claude.api-key=dummy-claude-key",
    "claude.api-url=http://localhost/claude-test",
    "claude.model=test-model",
    "daily.api-key=dummy-daily-key",
    "daily.api-url=http://localhost/daily-test",
    "gemini.api.key=dummy-gemini-key",
    "mailtrap.token=dummy-token",
    "mailtrap.inbox-id=dummy-inbox",
    "mailtrap.from=noreply@example.test",
    "telegram.bot-token=dummy-telegram-token",
    "telegram.default-chat-id=0",
    "alert.fallback-email=alerts@example.test",
    "meeting.room-expiry-minutes=60",
    "followup.scheduler.cron=0 0 0 * * *"
})
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    SecurityAutoConfiguration.class
})
class TrackingServiceApplicationTests {

    @MockBean
    private ActivityEntryRepository activityEntryRepository;

    @MockBean
    private CareActivityRepository careActivityRepository;

    @MockBean
    private CarePlanRepository carePlanRepository;

    @MockBean
    private DailyLogRepository dailyLogRepository;

    @MockBean
    private DoctorNotificationRepository doctorNotificationRepository;

    @MockBean
    private DoctorRatingRepository doctorRatingRepository;

    @MockBean
    private FollowUpReminderRepository followUpReminderRepository;

    @MockBean
    private IncidentEntryRepository incidentEntryRepository;

    @MockBean
    private MedicalFolderRepository medicalFolderRepository;

    @MockBean
    private MedicalMeetingRepository medicalMeetingRepository;

    @MockBean
    private MedicationIntakeEntryRepository medicationIntakeEntryRepository;

    @MockBean
    private MedicationIntakeLogRepository medicationIntakeLogRepository;

    @MockBean
    private MedicationRepository medicationRepository;

    @MockBean
    private NutritionEntryRepository nutritionEntryRepository;

    @MockBean
    private PatientAnswerRepository patientAnswerRepository;

    @MockBean
    private PrescriptionRepository prescriptionRepository;

    @MockBean
    private PrescriptionTemplateRepository prescriptionTemplateRepository;

    @MockBean
    private QuestionRepository questionRepository;

    @MockBean
    private QuestionnaireRepository questionnaireRepository;

    @MockBean
    private SessionRepository sessionRepository;

    @Test
    void contextLoads() {
    }
}
