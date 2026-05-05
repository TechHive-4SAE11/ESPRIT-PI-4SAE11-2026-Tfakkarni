package org.techhive.trackingservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.techhive.trackingservice.dto.FollowUpReminderResponse;
import org.techhive.trackingservice.dto.NotificationResponse;
import org.techhive.trackingservice.entity.ActivityEntry;
import org.techhive.trackingservice.entity.DailyLog;
import org.techhive.trackingservice.entity.DoctorNotification;
import org.techhive.trackingservice.entity.FollowUpReminder;
import org.techhive.trackingservice.entity.MedicalFolder;
import org.techhive.trackingservice.entity.MedicationIntakeLog;
import org.techhive.trackingservice.entity.NutritionEntry;
import org.techhive.trackingservice.repository.DailyLogRepository;
import org.techhive.trackingservice.repository.DoctorNotificationRepository;
import org.techhive.trackingservice.repository.FollowUpReminderRepository;
import org.techhive.trackingservice.repository.MedicalFolderRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertReminderServiceCoverageTest {

    @Mock RestTemplate lbRestTemplate;
    @Mock RestTemplate plainRestTemplate;
    @Mock DoctorNotificationRepository notificationRepository;
    @Mock MedicalFolderRepository medicalFolderRepository;
    @Mock FollowUpReminderRepository reminderRepository;
    @Mock DailyLogRepository dailyLogRepository;

    private IncidentAlertService incidentAlertService;
    private FollowUpReminderService followUpReminderService;

    @BeforeEach
    void setUp() {
        incidentAlertService = new IncidentAlertService(lbRestTemplate, plainRestTemplate, notificationRepository, medicalFolderRepository);
        ReflectionTestUtils.setField(incidentAlertService, "mailtrapToken", "mail-token");
        ReflectionTestUtils.setField(incidentAlertService, "mailtrapInboxId", "inbox-1");
        ReflectionTestUtils.setField(incidentAlertService, "fromEmail", "alerts@tfakkarni.test");
        ReflectionTestUtils.setField(incidentAlertService, "telegramBotToken", "telegram-token");
        ReflectionTestUtils.setField(incidentAlertService, "telegramChatId", "chat-1");
        ReflectionTestUtils.setField(incidentAlertService, "fallbackEmail", "fallback@tfakkarni.test");

        followUpReminderService = new FollowUpReminderService(reminderRepository, dailyLogRepository, lbRestTemplate, plainRestTemplate);
        ReflectionTestUtils.setField(followUpReminderService, "telegramBotToken", "telegram-token");
        ReflectionTestUtils.setField(followUpReminderService, "telegramChatId", "chat-1");
    }

    @Test
    void incidentAlertHandlesDoctorResolutionNotificationEmailAndTelegram() {
        MedicalFolder folder = new MedicalFolder();
        folder.setIdPatient("patient-kc");
        folder.setIdDoctor("doctor-kc");
        when(medicalFolderRepository.findByIdPatient("patient-kc")).thenReturn(List.of(folder));
        when(lbRestTemplate.getForObject("http://user-service/api/users/keycloak/doctor-kc", Map.class))
                .thenReturn(Map.of("firstName", "Sarra", "lastName", "Mansouri", "email", "doctor@tfakkarni.test", "keycloakId", "doctor-kc"));
        when(lbRestTemplate.getForObject("http://user-service/api/users/keycloak/patient-kc", Map.class))
                .thenReturn(Map.of("firstName", "Nour", "lastName", "Trabelsi", "email", "patient@tfakkarni.test", "keycloakId", "patient-kc"));
        when(notificationRepository.save(any(DoctorNotification.class))).thenAnswer(inv -> inv.getArgument(0));
        when(plainRestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(Map.of("ok", true), HttpStatus.OK));

        incidentAlertService.handleIncidentAlert("patient-kc", "GRAVE", "CHUTE", "Chute dans le salon", "Salon", "Repos", "Aucune", "10:00", "2026-05-03");

        ArgumentCaptor<DoctorNotification> notificationCaptor = ArgumentCaptor.forClass(DoctorNotification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        DoctorNotification saved = notificationCaptor.getValue();
        assertThat(saved.getDoctorKeycloakId()).isEqualTo("doctor-kc");
        assertThat(saved.getPatientName()).isEqualTo("Nour Trabelsi");
        assertThat(saved.getIncidentType()).isEqualTo("CHUTE");
        verify(plainRestTemplate).exchange(eq("https://sandbox.api.mailtrap.io/api/send/inbox-1"), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class));
        verify(plainRestTemplate).exchange(eq("https://api.telegram.org/bottelegram-token/sendMessage"), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void incidentAlertFallsBackThroughAlternateDoctorStrategiesAndCrudHelpers() {
        when(medicalFolderRepository.findByIdPatient("patient-missing")).thenThrow(new RuntimeException("folder db down"));
        MedicalFolder fallbackFolder = new MedicalFolder();
        fallbackFolder.setIdDoctor("folder-doctor");
        when(medicalFolderRepository.findAll()).thenReturn(List.of(fallbackFolder));
        when(lbRestTemplate.getForObject("http://user-service/api/users/keycloak/patient-missing", Map.class)).thenThrow(new RuntimeException("user unavailable"));
        when(lbRestTemplate.getForObject("http://user-service/api/users/keycloak/folder-doctor", Map.class)).thenThrow(new RuntimeException("doctor unavailable"));
        when(notificationRepository.save(any(DoctorNotification.class))).thenAnswer(inv -> inv.getArgument(0));
        when(plainRestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class))).thenThrow(new RuntimeException("network disabled in test"));

        incidentAlertService.handleIncidentAlert("patient-missing", "MODERE", "ERRANCE", "Sortie", "Rue", "Appel", "", "11:00", "bad-date");

        ArgumentCaptor<DoctorNotification> notificationCaptor = ArgumentCaptor.forClass(DoctorNotification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getDoctorKeycloakId()).isEqualTo("folder-doctor");
        assertThat(notificationCaptor.getValue().getPatientName()).isEqualTo("patient-missing");

        DoctorNotification notification = notification(22L, "doctor-a", false);
        when(notificationRepository.findByDoctorKeycloakIdOrderByCreatedAtDesc("doctor-a")).thenReturn(List.of(notification));
        when(notificationRepository.findByDoctorKeycloakIdInOrderByCreatedAtDesc(List.of("doctor-a", "doctor-b"))).thenReturn(List.of(notification));
        when(notificationRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(notification));
        when(notificationRepository.findDistinctDoctorKeycloakIds()).thenReturn(List.of("doctor-a", "doctor-b"));
        when(notificationRepository.countByDoctorKeycloakIdAndReadFalse("doctor-a")).thenReturn(2L);
        when(notificationRepository.findById(22L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        assertThat(incidentAlertService.getNotificationsForDoctor("doctor-a")).extracting(NotificationResponse::getId).containsExactly(22L);
        assertThat(incidentAlertService.getNotificationsForDoctorIds(List.of("doctor-a", "doctor-b"))).hasSize(1);
        assertThat(incidentAlertService.getNotificationsForDoctorIds(List.of())).isEmpty();
        assertThat(incidentAlertService.getAllNotifications()).hasSize(1);
        assertThat(incidentAlertService.getDistinctDoctorIds()).containsExactly("doctor-a", "doctor-b");
        assertThat(incidentAlertService.getUnreadCount("doctor-a")).isEqualTo(2L);
        NotificationResponse read = incidentAlertService.markAsRead(22L);
        assertThat(read.isRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();

        DoctorNotification unread = notification(23L, "doctor-a", false);
        when(notificationRepository.findByDoctorKeycloakIdOrderByCreatedAtDesc("doctor-a")).thenReturn(List.of(unread));
        incidentAlertService.markAllAsRead("doctor-a");
        verify(notificationRepository).saveAll(List.of(unread));
        when(notificationRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> incidentAlertService.markAsRead(404L)).hasMessageContaining("Notification not found");
    }

    @Test
    void incidentAlertDirectTestReportsEmailTelegramAndDbOutcomes() {
        when(plainRestTemplate.exchange(eq("https://sandbox.api.mailtrap.io/api/send/inbox-1"), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(Map.of("id", "mail-1"), HttpStatus.ACCEPTED));
        when(plainRestTemplate.exchange(eq("https://api.telegram.org/bottelegram-token/sendMessage"), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(Map.of("ok", true), HttpStatus.OK));
        when(notificationRepository.count()).thenReturn(7L);
        when(notificationRepository.findDistinctDoctorKeycloakIds()).thenReturn(List.of("doctor-a", "doctor-b"));

        Map<String, String> result = incidentAlertService.testAlertDirect("doctor@tfakkarni.test");

        assertThat(result.get("email")).contains("202 ACCEPTED");
        assertThat(result.get("telegram")).isEqualTo("✅ OK");
        assertThat(result.get("db_total")).isEqualTo("7");
        assertThat(result.get("db_doctor_ids")).contains("doctor-a | doctor-b");
    }

    @Test
    void followUpCheckCreatesOnlyMissingRemindersAndSendsGroupedTelegram() {
        LocalDate today = LocalDate.now();
        DailyLog complete = new DailyLog();
        complete.setNutritionEntries(List.of(new NutritionEntry()));
        complete.setMedicationIntakes(List.of(new MedicationIntakeLog()));
        complete.setActivityEntries(List.of(new ActivityEntry()));
        DailyLog partial = new DailyLog();
        partial.setNutritionEntries(List.of(new NutritionEntry()));
        partial.setMedicationIntakes(List.of());
        partial.setActivityEntries(List.of());

        when(reminderRepository.findAllRegisteredPatientIds()).thenReturn(List.of("already", "complete", "missing", "partial"));
        when(reminderRepository.existsByPatientKeycloakIdAndReminderDate("already", today)).thenReturn(true);
        when(dailyLogRepository.findByPatientKeycloakIdAndLogDate("complete", today)).thenReturn(Optional.of(complete));
        when(dailyLogRepository.findByPatientKeycloakIdAndLogDate("missing", today)).thenReturn(Optional.empty());
        when(dailyLogRepository.findByPatientKeycloakIdAndLogDate("partial", today)).thenReturn(Optional.of(partial));
        when(lbRestTemplate.getForObject("http://user-service/api/users/keycloak/missing", Map.class))
                .thenReturn(Map.of("firstName", "Meriem", "lastName", "Saidi"));
        when(lbRestTemplate.getForObject("http://user-service/api/users/keycloak/partial", Map.class))
                .thenThrow(new RuntimeException("fallback name"));
        when(reminderRepository.save(any(FollowUpReminder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(plainRestTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class))).thenReturn("ok");

        int created = followUpReminderService.checkAndCreateReminders();

        assertThat(created).isEqualTo(2);
        ArgumentCaptor<FollowUpReminder> reminderCaptor = ArgumentCaptor.forClass(FollowUpReminder.class);
        verify(reminderRepository, org.mockito.Mockito.times(2)).save(reminderCaptor.capture());
        assertThat(reminderCaptor.getAllValues()).extracting(FollowUpReminder::getPatientKeycloakId).containsExactly("missing", "partial");
        assertThat(reminderCaptor.getAllValues().get(0).getMissingCategories()).isEqualTo("NUTRITION,MEDICATION,ACTIVITY");
        assertThat(reminderCaptor.getAllValues().get(1).getMissingCategories()).isEqualTo("MEDICATION,ACTIVITY");
        verify(plainRestTemplate).postForObject(eq("https://api.telegram.org/bottelegram-token/sendMessage"), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void followUpReadApisMapResponsesAndHandleNoTelegramConfiguration() {
        ReflectionTestUtils.setField(followUpReminderService, "telegramBotToken", "");
        when(reminderRepository.findAllRegisteredPatientIds()).thenReturn(List.of("missing"));
        when(reminderRepository.existsByPatientKeycloakIdAndReminderDate(eq("missing"), any(LocalDate.class))).thenReturn(false);
        when(dailyLogRepository.findByPatientKeycloakIdAndLogDate(eq("missing"), any(LocalDate.class))).thenReturn(Optional.empty());
        when(lbRestTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(Map.of("firstName", "Youssef", "lastName", "Karoui"));
        when(reminderRepository.save(any(FollowUpReminder.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(followUpReminderService.checkAndCreateReminders()).isEqualTo(1);
        verify(plainRestTemplate, never()).postForObject(anyString(), any(), eq(String.class));

        FollowUpReminder reminder = FollowUpReminder.builder()
                .id(31L)
                .patientKeycloakId("patient-a")
                .patientName("Youssef Karoui")
                .reminderDate(LocalDate.now())
                .message("missing")
                .missingCategories("NUTRITION")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
        when(reminderRepository.findByPatientKeycloakIdOrderByCreatedAtDesc("patient-a")).thenReturn(List.of(reminder));
        when(reminderRepository.findByPatientKeycloakIdAndReadFalseOrderByCreatedAtDesc("patient-a")).thenReturn(List.of(reminder));
        when(reminderRepository.countByPatientKeycloakIdAndReadFalse("patient-a")).thenReturn(1L);
        when(reminderRepository.findById(31L)).thenReturn(Optional.of(reminder));
        when(reminderRepository.save(reminder)).thenReturn(reminder);

        assertThat(followUpReminderService.getReminders("patient-a")).extracting(FollowUpReminderResponse::getId).containsExactly(31L);
        assertThat(followUpReminderService.getUnreadReminders("patient-a")).hasSize(1);
        assertThat(followUpReminderService.countUnread("patient-a")).isEqualTo(1L);
        assertThat(followUpReminderService.markAsRead(31L).isRead()).isTrue();
        assertThat(reminder.getReadAt()).isNotNull();
        followUpReminderService.markAllAsRead("patient-a");
        verify(reminderRepository).saveAll(List.of(reminder));
        when(reminderRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> followUpReminderService.markAsRead(404L)).hasMessageContaining("Reminder not found");
    }

    private DoctorNotification notification(Long id, String doctorId, boolean read) {
        DoctorNotification notification = new DoctorNotification();
        notification.setId(id);
        notification.setDoctorKeycloakId(doctorId);
        notification.setPatientKeycloakId("patient-a");
        notification.setPatientName("Nour Trabelsi");
        notification.setIncidentType("CHUTE");
        notification.setSeverity("GRAVE");
        notification.setDescription("description");
        notification.setLocation("Salon");
        notification.setActionTaken("Appel");
        notification.setOccurredAt("10:00");
        notification.setLogDate("2026-05-03");
        notification.setRead(read);
        notification.setCreatedAt(LocalDateTime.now());
        return notification;
    }
}
