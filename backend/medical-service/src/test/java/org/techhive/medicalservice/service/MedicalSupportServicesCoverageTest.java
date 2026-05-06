package org.techhive.medicalservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.techhive.medicalservice.client.UserServiceSearchClient;
import org.techhive.medicalservice.config.GeminiSafetyAuditProperties;
import org.techhive.medicalservice.entity.Diagnostics;
import org.techhive.medicalservice.entity.MedicalFolder;
import org.techhive.medicalservice.entity.coaching.CoachingGoal;
import org.techhive.medicalservice.entity.coaching.CoachingGoalStatus;
import org.techhive.medicalservice.repository.CoachingGoalRepository;
import org.techhive.medicalservice.repository.CoachingProgressRepository;
import org.techhive.medicalservice.repository.MedicalFolderRepository;
import org.techhive.medicalservice.service.coaching.CoachingNotificationService;
import org.techhive.medicalservice.service.coaching.CoachingStaleScheduler;
import org.techhive.medicalservice.service.safety.GeminiSafetyAuditService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MedicalSupportServicesCoverageTest {

    @Test
    void fileStorageCreatesAttachmentsClassifiesTypesAndRejectsInvalidFiles() {
        FileStorageService service = new FileStorageService();
        MockMultipartFile file = new MockMultipartFile(
                "file", "brain-mri.pdf", "application/pdf", "pdf-data".getBytes());

        var attachment = service.createAttachment(file, "scan", service.determineFileType(file.getContentType(), file.getOriginalFilename()));

        assertThat(attachment.getOriginalFileName()).isEqualTo("brain-mri.pdf");
        assertThat(attachment.getFileName()).startsWith("file_").endsWith(".pdf");
        assertThat(attachment.getFileData()).containsExactly("pdf-data".getBytes());
        assertThat(service.determineFileType("image/png", "photo.png")).isEqualTo("PHOTO");
        assertThat(service.determineFileType("application/pdf", "report.pdf")).isEqualTo("DOCUMENT");
        assertThat(service.determineFileType("application/msword", "letter.doc")).isEqualTo("DOCUMENT");
        assertThat(service.determineFileType("application/vnd.ms-excel", "sheet.xls")).isEqualTo("DOCUMENT");
        assertThat(service.determineFileType("text/plain", "notes.txt")).isEqualTo("DOCUMENT");
        assertThat(service.determineFileType("application/octet-stream", "chest-xray.bin")).isEqualTo("XRAY");
        assertThat(service.determineFileType("application/octet-stream", "brain-mri.bin")).isEqualTo("MRI");
        assertThat(service.determineFileType("application/octet-stream", "ct-scan.bin")).isEqualTo("CT_SCAN");
        assertThat(service.determineFileType("application/octet-stream", "ultrasound.bin")).isEqualTo("ULTRASOUND");
        assertThat(service.determineFileType(null, "whatever")).isEqualTo("OTHER");
        assertThat(service.determineFileType("application/octet-stream", "misc.bin")).isEqualTo("OTHER");

        assertThatThrownBy(() -> service.createAttachment(new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]), "", "DOCUMENT"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
        assertThatThrownBy(() -> service.createAttachment(new MockMultipartFile("file", "bad.exe", "application/x-msdownload", new byte[]{1}), "", "OTHER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not allowed");
        assertThatThrownBy(() -> service.createAttachment(new MockMultipartFile("file", "huge.pdf", "application/pdf", new byte[10 * 1024 * 1024 + 1]), "", "DOCUMENT"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10MB");
        assertThat(service.createAttachment(new MockMultipartFile("file", "", "text/plain", "x".getBytes()), "", "DOCUMENT").getFileName())
                .startsWith("unknown_file_");
    }

    @Test
    void patientServiceMapsUserSearchAndDiagnosisFallbacks() {
        UserServiceSearchClient userClient = mock(UserServiceSearchClient.class);
        MedicalFolderRepository folderRepository = mock(MedicalFolderRepository.class);
        PatientService service = new PatientService(userClient, folderRepository);

        Diagnostics diagnostics = Diagnostics.builder().diseaseName("Alzheimer").build();
        MedicalFolder folder = MedicalFolder.builder().patientId("kc-1").diagnostics(List.of(diagnostics)).build();
        when(userClient.searchUsersByName("Nour")).thenReturn(List.of(Map.of(
                "id", 15,
                "keycloakId", "kc-1",
                "firstName", "Nour",
                "lastName", "Trabelsi",
                "email", "nour@example.test"
        )));
        when(folderRepository.findByPatientId("kc-1")).thenReturn(List.of(folder));

        assertThat(service.findByName("Nour").getDiagnosis()).isEqualTo("Alzheimer");

        when(userClient.searchUsersByName("Fallback")).thenReturn(List.of(Map.of("id", 22, "firstName", "Ali")));
        when(folderRepository.findByPatientId("22")).thenReturn(List.of(MedicalFolder.builder()
                .diagnostics(List.of(Diagnostics.builder().diseaseName(null).build()))
                .build()));
        assertThat(service.findByName("Fallback").getDiagnosis()).isEqualTo("Non spécifié");

        when(userClient.searchUsersByName("Missing")).thenReturn(List.of());
        assertThat(service.findByName("Missing")).isNull();
        when(userClient.searchUsersByName("Null")).thenReturn(null);
        assertThat(service.findByName("Null")).isNull();
    }

    @Test
    void coachingStaleSchedulerSendsOnlyForStaleGoalsAndHonorsDemoModeAndCooldown() {
        CoachingGoalRepository goalRepository = mock(CoachingGoalRepository.class);
        CoachingProgressRepository progressRepository = mock(CoachingProgressRepository.class);
        CoachingNotificationService notificationService = mock(CoachingNotificationService.class);
        CoachingStaleScheduler scheduler = new CoachingStaleScheduler(goalRepository, progressRepository, notificationService);
        ReflectionTestUtils.setField(scheduler, "staleDays", 7);
        ReflectionTestUtils.setField(scheduler, "staleNotificationCooldownHours", 168);

        MedicalFolder folder = MedicalFolder.builder().patientId("patient-1").build();
        CoachingGoal stale = CoachingGoal.builder().id(1L).medicalFolder(folder).goalTitle("Walk").targetDays(3).build();
        CoachingGoal fresh = CoachingGoal.builder().id(2L).medicalFolder(folder).goalTitle("Read").targetDays(5).build();
        CoachingGoal throttled = CoachingGoal.builder()
                .id(3L)
                .medicalFolder(folder)
                .goalTitle("Hydrate")
                .lastStaleNotificationAt(LocalDateTime.now())
                .build();

        when(goalRepository.findByStatus(CoachingGoalStatus.ACTIVE)).thenReturn(List.of(stale, fresh, throttled));
        when(progressRepository.findLatestProgressDate(1L)).thenReturn(Optional.of(LocalDate.now().minusDays(10)));
        when(progressRepository.findLatestProgressDate(2L)).thenReturn(Optional.of(LocalDate.now()));
        when(progressRepository.findLatestProgressDate(3L)).thenReturn(Optional.empty());

        assertThat(scheduler.runNowForTest()).isEqualTo(1);
        verify(notificationService).notifyUser(eq("patient-1"), eq(folder), eq(stale), eq("STALE_PROGRESS"), anyString(), contains("Walk"));
        verify(goalRepository).save(stale);

        scheduler.setDemoMode(true);
        assertThat(scheduler.isDemoMode()).isTrue();
        stale.setLastStaleNotificationAt(null);
        when(progressRepository.findLatestProgressDate(1L)).thenReturn(Optional.of(LocalDate.now().minusDays(1)));
        assertThat(scheduler.runNowForTest()).isGreaterThanOrEqualTo(1);
        scheduler.notifyStaleGoals();
    }

    @Test
    void geminiSafetyAuditHandlesDisabledBlankKeyErrorsAndJsonResponses() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        GeminiSafetyAuditProperties props = new GeminiSafetyAuditProperties();
        GeminiSafetyAuditService disabled = new GeminiSafetyAuditService(props, objectMapper);
        assertThat(disabled.isEnabled()).isFalse();
        assertThat(disabled.analyzePatientPool("{\"patients\":[]}")).isEmpty();

        props.setApiKey("key");
        GeminiSafetyAuditService service = new GeminiSafetyAuditService(props, objectMapper);
        RestTemplate restTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(service, "geminiRestTemplate", restTemplate);

        JsonNode ok = objectMapper.readTree("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"```json\\n{\\\"chronicAlerts\\\":[],\\\"conflicts\\\":[]}\\n```\"}]}}]}");
        JsonNode error = objectMapper.readTree("{\"error\":{\"message\":\"bad key\"}}");
        JsonNode empty = objectMapper.readTree("{\"candidates\":[]}");
        JsonNode blank = objectMapper.readTree("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"\"}]}}]}");

        when(restTemplate.postForObject(anyString(), any(), eq(JsonNode.class)))
                .thenReturn(ok)
                .thenReturn(error)
                .thenReturn(empty)
                .thenReturn(blank)
                .thenThrow(new IllegalStateException("network"));

        assertThat(service.isEnabled()).isTrue();
        assertThat(service.analyzePatientPool("{\"patients\":[]}")).isPresent();
        assertThat(service.analyzePatientPool("{\"patients\":[]}")).isEmpty();
        assertThat(service.analyzePatientPool("{\"patients\":[]}")).isEmpty();
        assertThat(service.analyzePatientPool("{\"patients\":[]}")).isEmpty();
        assertThat(service.analyzePatientPool("{\"patients\":[]}")).isEmpty();
    }
}
