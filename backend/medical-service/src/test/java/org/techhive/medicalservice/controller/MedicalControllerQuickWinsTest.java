package org.techhive.medicalservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.techhive.medicalservice.dto.*;
import org.techhive.medicalservice.entity.DiagnosticAttachment;
import org.techhive.medicalservice.entity.Diagnostics;
import org.techhive.medicalservice.entity.MedicalFolder;
import org.techhive.medicalservice.repository.DiagnosticsRepository;
import org.techhive.medicalservice.repository.MedicalFolderRepository;
import org.techhive.medicalservice.service.FileStorageService;
import org.techhive.medicalservice.service.MedicalFolderService;
import org.techhive.medicalservice.service.PatientService;
import org.techhive.medicalservice.service.PredictionService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalControllerQuickWinsTest {

    @Mock
    private PredictionService predictionService;
    @Mock
    private PatientService patientService;
    @Mock
    private MedicalFolderRepository medicalFolderRepository;
    @Mock
    private DiagnosticsRepository diagnosticsRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private MedicalFolderService medicalFolderService;
    @Mock
    private Authentication authentication;
    @Mock
    private HttpServletRequest request;

    @Test
    void healthController_returnsStaticUpStatus() {
        ResponseEntity<HealthController.HealthStatus> response = new HealthController().health();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().status);
        assertEquals("medical-service", response.getBody().service);
        assertEquals("Medical service is running", response.getBody().message);
    }

    @Test
    void predictionController_delegatesDashboardAndAppointmentPrediction() {
        PredictionController controller = new PredictionController(predictionService);
        DashboardStatsDTO dashboard = DashboardStatsDTO.builder().totalAppointments(12L).globalNoShowRate(25.0).build();
        PredictionDTO prediction = PredictionDTO.builder().riskScore(77).riskLevel("HIGH").recommendation("call patient").build();
        when(predictionService.getDashboardStats()).thenReturn(dashboard);
        when(predictionService.predictForAppointment(42L)).thenReturn(prediction);

        assertSame(dashboard, controller.getDashboardStats().getBody());
        assertSame(prediction, controller.getPredictionForAppointment(42L).getBody());
    }

    @Test
    void patientController_returnsOkForFoundPatientAndNotFoundForMissingPatient() {
        PatientController controller = new PatientController(patientService);
        PatientDTO patient = PatientDTO.builder()
                .id(5L)
                .keycloakId("patient-keycloak")
                .firstName("Amira")
                .lastName("Bouzid")
                .email("amira.bouzid@example.tn")
                .diagnosis("Asthma")
                .build();
        when(patientService.findByName("Amira")).thenReturn(patient);
        when(patientService.findByName("Unknown")).thenReturn(null);

        ResponseEntity<PatientDTO> found = controller.searchPatientByName("Amira");
        ResponseEntity<PatientDTO> missing = controller.searchPatientByName("Unknown");

        assertEquals(HttpStatus.OK, found.getStatusCode());
        assertEquals("Amira", found.getBody().getFirstName());
        assertEquals(HttpStatus.NOT_FOUND, missing.getStatusCode());
        assertNull(missing.getBody());
    }

    @Test
    void debugAnalyticsController_returnsCountsPatientsAndDiagnosticSample() {
        DebugAnalyticsController controller = new DebugAnalyticsController(medicalFolderRepository, diagnosticsRepository);
        MedicalFolder folderA = MedicalFolder.builder().patientId("patient-a").build();
        MedicalFolder folderB = MedicalFolder.builder().patientId("patient-b").build();
        Diagnostics diagA = Diagnostics.builder().diseaseName("Diabetes").build();
        Diagnostics diagB = Diagnostics.builder().diseaseName("Asthma").build();
        when(medicalFolderRepository.count()).thenReturn(2L);
        when(diagnosticsRepository.count()).thenReturn(2L);
        when(medicalFolderRepository.findAll()).thenReturn(List.of(folderA, folderB));
        when(diagnosticsRepository.findAll()).thenReturn(List.of(diagA, diagB));

        Map<String, Object> result = controller.debugData();

        assertEquals(2L, result.get("medical_service_folders_count"));
        assertEquals(2L, result.get("medical_service_diagnostics_count"));
        assertTrue(result.get("patient_ids_in_folders").toString().contains("patient-a"));
        assertEquals(List.of("Diabetes", "Asthma"), result.get("diagnostics_sample"));
    }

    @Test
    void diagnosticAttachmentController_uploadsSingleAndMultipleFilesAndMapsErrors() {
        DiagnosticAttachmentController controller = new DiagnosticAttachmentController(fileStorageService);
        MockMultipartFile file = new MockMultipartFile("file", "scan.png", "image/png", "data".getBytes(StandardCharsets.UTF_8));
        DiagnosticAttachment attachment = DiagnosticAttachment.builder()
                .id(10L)
                .fileName("file_10.png")
                .originalFileName("scan.png")
                .contentType("image/png")
                .fileSize(4L)
                .description("scan")
                .fileType("PHOTO")
                .createdAt(LocalDateTime.of(2026, 5, 3, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 5, 3, 9, 1))
                .build();
        when(fileStorageService.determineFileType("image/png", "scan.png")).thenReturn("PHOTO");
        when(fileStorageService.createAttachment(any(MultipartFile.class), any(), eq("PHOTO"))).thenAnswer(invocation -> {
            DiagnosticAttachment copy = DiagnosticAttachment.builder()
                    .id(10L)
                    .fileName("file_10.png")
                    .originalFileName("scan.png")
                    .contentType("image/png")
                    .fileSize(4L)
                    .description(invocation.getArgument(1))
                    .fileType("PHOTO")
                    .createdAt(LocalDateTime.of(2026, 5, 3, 9, 0))
                    .updatedAt(LocalDateTime.of(2026, 5, 3, 9, 1))
                    .build();
            return copy;
        });

        ResponseEntity<DiagnosticAttachmentResponse> single = controller.uploadFile(file, "scan", 99L);
        ResponseEntity<List<DiagnosticAttachmentResponse>> multiple = controller.uploadMultipleFiles(
                new MultipartFile[] { file, file }, new String[] { "first" }, 99L);

        assertEquals(HttpStatus.OK, single.getStatusCode());
        assertEquals(99L, single.getBody().getDiagnosticId());
        assertEquals("file_10.png", single.getBody().getFileName());
        assertEquals(2, multiple.getBody().size());
        assertNull(multiple.getBody().get(1).getDescription());

        doThrow(new IllegalArgumentException("invalid file"))
                .when(fileStorageService).determineFileType("image/png", "scan.png");
        ResponseStatusException badRequest = assertThrows(ResponseStatusException.class,
                () -> controller.uploadFile(file, "scan", 99L));
        assertEquals(HttpStatus.BAD_REQUEST, badRequest.getStatusCode());

        doThrow(new RuntimeException("disk full"))
                .when(fileStorageService).determineFileType("image/png", "scan.png");
        ResponseStatusException serverError = assertThrows(ResponseStatusException.class,
                () -> controller.uploadMultipleFiles(new MultipartFile[] { file }, null, 99L));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, serverError.getStatusCode());
    }

    @Test
    void diagnosticAttachmentController_placeholderEndpointsReturnInternalServerError() {
        DiagnosticAttachmentController controller = new DiagnosticAttachmentController(fileStorageService);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                assertThrows(ResponseStatusException.class, () -> controller.downloadFile(1L)).getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                assertThrows(ResponseStatusException.class, () -> controller.viewFile(1L)).getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                assertThrows(ResponseStatusException.class, () -> controller.deleteFile(1L)).getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                assertThrows(ResponseStatusException.class, () -> controller.getAttachmentsByDiagnostic(1L)).getStatusCode());
    }

    @Test
    void medicalFolderController_delegatesReadUpdateDeleteAndRestrictionEndpoints() {
        MedicalFolderController controller = new MedicalFolderController(medicalFolderService, new ObjectMapper());
        MedicalFolderResponse response = MedicalFolderResponse.builder().id(7L).patientId("patient-7").doctorId("doctor-7").build();
        MedicalFolderStatsResponse stats = MedicalFolderStatsResponse.builder().total(3L).thisMonth(1L).thisWeek(1L).patientCount(2L).build();
        when(medicalFolderService.getMedicalFolders(any(), eq("pat"))).thenReturn(new PageImpl<>(List.of(response)));
        when(medicalFolderService.getMedicalFolderStats()).thenReturn(stats);
        when(medicalFolderService.getMedicalFoldersByDoctorId("doctor-7")).thenReturn(List.of(response));
        when(medicalFolderService.getMedicalFoldersByPatientId("patient-7")).thenReturn(List.of(response));
        when(medicalFolderService.getMedicalFoldersByPatientIdAndDoctorId("patient-7", "doctor-7")).thenReturn(List.of(response));
        when(medicalFolderService.getMedicalFolderById(7L)).thenReturn(response);
        when(medicalFolderService.clearBookingRestrictionAfterReview(7L)).thenReturn(response);
        when(medicalFolderService.manualRestrictPatientBooking(7L, "manual review")).thenReturn(response);
        when(medicalFolderService.updateMedicalFolder(eq(7L), any())).thenReturn(response);
        when(medicalFolderService.partialUpdateMedicalFolder(eq(7L), any())).thenReturn(response);

        assertEquals(1, controller.getMedicalFolders(PageRequest.of(0, 10), "pat").getBody().getContent().size());
        assertSame(stats, controller.getMedicalFolderStats().getBody());
        assertEquals(1, controller.getMedicalFoldersByDoctorId("doctor-7").getBody().size());
        assertEquals(1, controller.getMedicalFoldersByPatientId("patient-7").getBody().size());
        assertEquals(1, controller.getMedicalFoldersByPatientAndDoctor("patient-7", "doctor-7").getBody().size());
        assertSame(response, controller.getMedicalFolderById(7L).getBody());
        assertSame(response, controller.clearBookingRestriction(7L).getBody());
        assertSame(response, controller.restrictPatientBooking(7L, "manual review").getBody());
        assertSame(response, controller.updateMedicalFolder(7L, new UpdateMedicalFolderRequest()).getBody());
        assertSame(response, controller.partialUpdateMedicalFolder(7L, new UpdateMedicalFolderRequest()).getBody());
        assertEquals(HttpStatus.NO_CONTENT, controller.deleteMedicalFolder(7L).getStatusCode());
        verify(medicalFolderService).deleteMedicalFolder(7L);
    }

    @Test
    void medicalFolderController_createExtractsDoctorFromAuthenticationPrincipal() {
        MedicalFolderController controller = new MedicalFolderController(medicalFolderService, new ObjectMapper());
        CreateMedicalFolderRequest create = CreateMedicalFolderRequest.builder().patientId("patient-auth").build();
        MedicalFolderResponse response = MedicalFolderResponse.builder().id(8L).patientId("patient-auth").doctorId("doctor-auth").build();
        when(authentication.getPrincipal()).thenReturn("doctor-auth");
        when(medicalFolderService.createMedicalFolder(create)).thenReturn(response);

        ResponseEntity<MedicalFolderResponse> result = controller.createMedicalFolder(create, authentication, request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals("doctor-auth", create.getDoctorId());
        assertSame(response, result.getBody());
    }

    @Test
    void medicalFolderController_createExtractsDoctorFromBearerSubjectAndRejectsBadTokens() {
        MedicalFolderController controller = new MedicalFolderController(medicalFolderService, new ObjectMapper());
        CreateMedicalFolderRequest create = CreateMedicalFolderRequest.builder().patientId("patient-token").build();
        MedicalFolderResponse response = MedicalFolderResponse.builder().id(9L).patientId("patient-token").doctorId("doctor-token").build();
        when(authentication.getPrincipal()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwtWithPayload("{\"sub\":\"doctor-token\"}"));
        when(medicalFolderService.createMedicalFolder(create)).thenReturn(response);

        ResponseEntity<MedicalFolderResponse> result = controller.createMedicalFolder(create, authentication, request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals("doctor-token", create.getDoctorId());

        CreateMedicalFolderRequest badCreate = CreateMedicalFolderRequest.builder().patientId("patient-bad").build();
        when(request.getHeader("Authorization")).thenReturn("Bearer malformed-token");
        ResponseStatusException badToken = assertThrows(ResponseStatusException.class,
                () -> controller.createMedicalFolder(badCreate, authentication, request));
        assertEquals(HttpStatus.UNAUTHORIZED, badToken.getStatusCode());

        when(request.getHeader("Authorization")).thenReturn(null);
        ResponseStatusException missingHeader = assertThrows(ResponseStatusException.class,
                () -> controller.createMedicalFolder(badCreate, authentication, request));
        assertEquals(HttpStatus.UNAUTHORIZED, missingHeader.getStatusCode());
    }

    private static String jwtWithPayload(String payloadJson) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".";
    }
}
