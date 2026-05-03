package org.techhive.trackingservice.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.techhive.trackingservice.client.UserServiceClient;
import org.techhive.trackingservice.dto.CareActivityRequestDTO;
import org.techhive.trackingservice.dto.CareActivityResponseDTO;
import org.techhive.trackingservice.dto.CarePlanRequestDTO;
import org.techhive.trackingservice.dto.CarePlanResponseDTO;
import org.techhive.trackingservice.dto.MedicalFolderRequestDTO;
import org.techhive.trackingservice.dto.MedicationRequestDTO;
import org.techhive.trackingservice.dto.MedicationResponseDTO;
import org.techhive.trackingservice.dto.PagedResponse;
import org.techhive.trackingservice.dto.PrescriptionRequestDTO;
import org.techhive.trackingservice.dto.PrescriptionResponseDTO;
import org.techhive.trackingservice.dto.SessionRequestDTO;
import org.techhive.trackingservice.entity.CareActivity;
import org.techhive.trackingservice.entity.CarePlan;
import org.techhive.trackingservice.entity.MedicalFolder;
import org.techhive.trackingservice.entity.Medication;
import org.techhive.trackingservice.entity.Prescription;
import org.techhive.trackingservice.entity.Session;
import org.techhive.trackingservice.enums.CareActivityType;
import org.techhive.trackingservice.enums.MedicationStatus;
import org.techhive.trackingservice.mapper.CarePlanMapper;
import org.techhive.trackingservice.mapper.PrescriptionMapper;
import org.techhive.trackingservice.repository.MedicationRepository;
import org.techhive.trackingservice.service.CarePlanService;
import org.techhive.trackingservice.service.MedicalFolderService;
import org.techhive.trackingservice.service.MedicationService;
import org.techhive.trackingservice.service.MedicationStatusScheduler;
import org.techhive.trackingservice.service.PrescriptionPdfService;
import org.techhive.trackingservice.service.PrescriptionService;
import org.techhive.trackingservice.service.SessionService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControllerCoverageTest {

    @Mock MedicationService medicationService;
    @Mock MedicationRepository medicationRepository;
    @Mock CarePlanService carePlanService;
    @Mock CarePlanMapper carePlanMapper;
    @Mock PrescriptionService prescriptionService;
    @Mock PrescriptionMapper prescriptionMapper;
    @Mock PrescriptionPdfService prescriptionPdfService;
    @Mock UserServiceClient userServiceClient;
    @Mock MedicalFolderService medicalFolderService;
    @Mock SessionService sessionService;
    @Mock MedicationStatusScheduler medicationStatusScheduler;

    @Test
    void medicationControllerCoversPaginationStatusDetailsAndErrors() {
        MedicationController controller = new MedicationController(medicationService, medicationRepository);
        MedicationResponseDTO dto = new MedicationResponseDTO();
        dto.setId(5L);
        when(medicationService.getMedicationsByPatient(eq("patient-1"), eq(MedicationStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto)));
        when(medicationService.getMedicationsByDoctor(eq("doctor-1"), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto)));

        ResponseEntity<PagedResponse<MedicationResponseDTO>> patientResponse = controller.getMedicationsByPatientPaginated(
                "patient-1", 0, 10, "createdAt", "ASC", MedicationStatus.ACTIVE);
        ResponseEntity<PagedResponse<MedicationResponseDTO>> doctorResponse = controller.getMedicationsByDoctorPaginated(
                "doctor-1", 0, 10, "createdAt", "DESC", null);

        assertThat(patientResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patientResponse.getBody().getContent()).hasSize(1);
        assertThat(doctorResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        when(medicationService.getMedicationsByPatient(eq("boom"), eq(null), any(Pageable.class)))
                .thenThrow(new RuntimeException("downstream"));
        assertThat(controller.getMedicationsByPatientPaginated("boom", 0, 10, "createdAt", "DESC", null).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        Medication medication = new Medication();
        medication.setId(9L);
        medication.setStatus(MedicationStatus.ACTIVE);
        medication.setInstructions("Initial");
        when(medicationRepository.findById(9L)).thenReturn(Optional.of(medication));
        when(medicationRepository.save(any(Medication.class))).thenAnswer(inv -> inv.getArgument(0));
        MedicationController.UpdateStatusRequest statusRequest = new MedicationController.UpdateStatusRequest();
        statusRequest.setStatus(MedicationStatus.DISCONTINUED);
        statusRequest.setReason("allergie");

        ResponseEntity<?> statusResponse = controller.updateMedicationStatus(9L, statusRequest);

        assertThat(statusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(medication.getEndDate()).isEqualTo(LocalDate.now());
        assertThat(medication.getInstructions()).contains("[DISCONTINUED] allergie");

        when(medicationRepository.findById(404L)).thenReturn(Optional.empty());
        assertThat(controller.updateMedicationStatus(404L, statusRequest).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        when(medicationRepository.findById(10L)).thenReturn(Optional.of(medication));
        when(medicationService.convertToDTO(medication)).thenReturn(dto);
        assertThat(controller.getMedication(10L).getStatusCode()).isEqualTo(HttpStatus.OK);
        when(medicationRepository.findById(11L)).thenReturn(Optional.empty());
        assertThat(controller.getMedication(11L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        when(medicationService.updateMedication(eq(12L), any(Medication.class))).thenReturn(dto);
        MedicationController.UpdateMedicationRequest updateRequest = new MedicationController.UpdateMedicationRequest();
        updateRequest.setMedicationName("Vitamine D");
        updateRequest.setDosage("1000 UI");
        updateRequest.setFrequency("daily");
        updateRequest.setDuration("ongoing");
        updateRequest.setInstructions("matin");
        updateRequest.setStartDate(LocalDate.now());
        updateRequest.setEndDate(LocalDate.now().plusDays(5));
        assertThat(controller.updateMedication(12L, updateRequest).getStatusCode()).isEqualTo(HttpStatus.OK);
        when(medicationService.updateMedication(eq(13L), any(Medication.class))).thenThrow(new IllegalArgumentException("missing"));
        assertThat(controller.updateMedication(13L, updateRequest).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void carePlanControllerCoversCrudPaginationAndErrorBranches() {
        CarePlanController controller = new CarePlanController(carePlanService, carePlanMapper);
        CareActivityRequestDTO activityRequest = CareActivityRequestDTO.builder()
                .activityName("Marche").activityType(CareActivityType.PHYSICAL_ACTIVITY).description("Douce").build();
        CarePlanRequestDTO request = new CarePlanRequestDTO(3L, List.of(activityRequest));
        CareActivity activity = new CareActivity();
        CarePlan plan = new CarePlan();
        plan.setId(4L);
        CarePlanResponseDTO responseDTO = new CarePlanResponseDTO();
        responseDTO.setId(4L);
        CareActivityResponseDTO activityDTO = new CareActivityResponseDTO();
        activityDTO.setId(8L);

        when(carePlanMapper.toActivityEntity(activityRequest)).thenReturn(activity);
        when(carePlanService.createCarePlanForSession(eq(3L), any(CarePlan.class))).thenReturn(plan);
        when(carePlanMapper.toResponseDTO(plan)).thenReturn(responseDTO);
        assertThat(controller.createCarePlan(request).getStatusCode()).isEqualTo(HttpStatus.CREATED);

        when(carePlanService.createCarePlanForSession(eq(99L), any(CarePlan.class))).thenThrow(new IllegalArgumentException("bad session"));
        assertThat(controller.createCarePlan(new CarePlanRequestDTO(99L, List.of(activityRequest))).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        when(carePlanService.createCarePlanForSession(eq(100L), any(CarePlan.class))).thenThrow(new RuntimeException("boom"));
        assertThat(controller.createCarePlan(new CarePlanRequestDTO(100L, List.of(activityRequest))).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        when(carePlanService.getAllCarePlans()).thenReturn(List.of(plan));
        assertThat(controller.getAllCarePlans().getBody()).hasSize(1);
        when(carePlanService.getCarePlanById(4L)).thenReturn(Optional.of(plan));
        assertThat(controller.getCarePlanById(4L).getStatusCode()).isEqualTo(HttpStatus.OK);
        when(carePlanService.getCarePlanById(404L)).thenReturn(Optional.empty());
        assertThat(controller.getCarePlanById(404L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        when(carePlanService.updateCarePlan(eq(4L), any(CarePlan.class))).thenReturn(plan);
        assertThat(controller.updateCarePlan(4L, request).getStatusCode()).isEqualTo(HttpStatus.OK);
        when(carePlanService.updateCarePlan(eq(404L), any(CarePlan.class))).thenThrow(new RuntimeException("missing"));
        assertThat(controller.updateCarePlan(404L, request).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        when(carePlanService.getCarePlansBySession(3L)).thenReturn(List.of(plan));
        when(carePlanService.getCarePlansByPatient("patient-1")).thenReturn(List.of(plan));
        when(carePlanService.getCarePlansByPatientPaginated(eq("patient-1"), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(plan)));
        assertThat(controller.getCarePlansBySession(3L).getBody()).hasSize(1);
        assertThat(controller.getCarePlansByPatient("patient-1").getBody()).hasSize(1);
        assertThat(controller.getCarePlansByPatientPaginated("patient-1", 0, 10, "createdAt", "ASC").getBody().getContent()).hasSize(1);

        when(carePlanService.updateActivityStatus(8L, "DONE")).thenReturn(activity);
        when(carePlanMapper.toActivityResponseDTO(activity)).thenReturn(activityDTO);
        assertThat(controller.updateActivityStatus(8L, Map.of("status", "DONE")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.updateActivityStatus(8L, Map.of()).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        when(carePlanService.updateActivityStatus(9L, "DONE")).thenThrow(new RuntimeException("missing"));
        assertThat(controller.updateActivityStatus(9L, Map.of("status", "DONE")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(controller.deleteCarePlan(4L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        doThrow(new RuntimeException("missing")).when(carePlanService).deleteCarePlan(404L);
        assertThat(controller.deleteCarePlan(404L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void prescriptionControllerCoversCrudPaginationAndPdfBranches() throws Exception {
        PrescriptionController controller = new PrescriptionController(prescriptionService, prescriptionMapper, prescriptionPdfService, userServiceClient);
        MedicationRequestDTO medicationRequest = new MedicationRequestDTO("Paracétamol", "500mg", "daily", "5 days", "after meal");
        PrescriptionRequestDTO request = new PrescriptionRequestDTO();
        request.setSessionId(7L);
        request.setMedications(List.of(medicationRequest));
        Medication medication = new Medication();
        Prescription prescription = new Prescription();
        prescription.setId(6L);
        PrescriptionResponseDTO responseDTO = new PrescriptionResponseDTO();
        responseDTO.setId(6L);

        when(prescriptionMapper.toMedicationEntity(medicationRequest)).thenReturn(medication);
        when(prescriptionService.createPrescriptionForSession(eq(7L), any(Prescription.class))).thenReturn(prescription);
        when(prescriptionMapper.toResponseDTO(prescription)).thenReturn(responseDTO);
        assertThat(controller.createPrescription(request).getStatusCode()).isEqualTo(HttpStatus.CREATED);

        when(prescriptionService.createPrescriptionForSession(eq(8L), any(Prescription.class))).thenThrow(new IllegalArgumentException("bad session"));
        PrescriptionRequestDTO badRequest = new PrescriptionRequestDTO();
        badRequest.setSessionId(8L);
        badRequest.setMedications(List.of(medicationRequest));
        assertThat(controller.createPrescription(badRequest).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        when(prescriptionService.createPrescriptionForSession(eq(9L), any(Prescription.class))).thenThrow(new RuntimeException("boom"));
        PrescriptionRequestDTO boomRequest = new PrescriptionRequestDTO();
        boomRequest.setSessionId(9L);
        boomRequest.setMedications(List.of(medicationRequest));
        assertThat(controller.createPrescription(boomRequest).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        when(prescriptionService.getAllPrescriptions()).thenReturn(List.of(prescription));
        assertThat(controller.getAllPrescriptions().getBody()).hasSize(1);
        when(prescriptionService.getPrescriptionById(6L)).thenReturn(Optional.of(prescription));
        assertThat(controller.getPrescriptionById(6L).getStatusCode()).isEqualTo(HttpStatus.OK);
        when(prescriptionService.getPrescriptionById(404L)).thenReturn(Optional.empty());
        assertThat(controller.getPrescriptionById(404L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        when(prescriptionService.getPrescriptionsBySession(7L)).thenReturn(List.of(prescription));
        when(prescriptionService.getPrescriptionsByPatient("patient-1")).thenReturn(List.of(prescription));
        when(prescriptionService.getPrescriptionsByPatientPaginated(eq("patient-1"), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(prescription)));
        assertThat(controller.getPrescriptionsBySession(7L).getBody()).hasSize(1);
        assertThat(controller.getPrescriptionsByPatient("patient-1").getBody()).hasSize(1);
        assertThat(controller.getPrescriptionsByPatientPaginated("patient-1", 0, 10, "createdAt", "DESC").getBody().getContent()).hasSize(1);

        when(prescriptionService.updatePrescription(eq(6L), any(Prescription.class))).thenReturn(prescription);
        assertThat(controller.updatePrescription(6L, request).getStatusCode()).isEqualTo(HttpStatus.OK);
        PrescriptionRequestDTO nullMedsRequest = new PrescriptionRequestDTO();
        nullMedsRequest.setMedications(null);
        when(prescriptionService.updatePrescription(eq(10L), any(Prescription.class))).thenThrow(new IllegalArgumentException("invalid"));
        assertThat(controller.updatePrescription(10L, nullMedsRequest).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        when(prescriptionService.updatePrescription(eq(11L), any(Prescription.class))).thenThrow(new RuntimeException("missing"));
        assertThat(controller.updatePrescription(11L, request).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(controller.deletePrescription(6L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(prescriptionService).deletePrescription(6L);

        when(prescriptionService.getPrescriptionById(12L)).thenReturn(Optional.empty());
        assertThat(controller.getPrescriptionPdf(12L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        when(prescriptionService.getPrescriptionById(13L)).thenReturn(Optional.of(prescription));
        when(prescriptionService.getDoctorKeycloakIdForPrescription(13L)).thenReturn("doctor-kc");
        when(userServiceClient.getUserByKeycloakId("doctor-kc")).thenReturn(Map.of("id", 55));
        when(userServiceClient.getDoctorSignature(55L)).thenReturn(new byte[]{1, 2});
        when(prescriptionPdfService.generatePrescriptionPdf(eq(prescription), any())).thenReturn(new byte[]{3, 4, 5});
        assertThat(controller.getPrescriptionPdf(13L).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void medicalFolderSessionAndMedicationStatusControllersCoverRemainingCrudPaths() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 3, 15, 45);

        MedicalFolder folder = folder(21L, "patient-kc-21", "doctor-kc-21", now);
        MedicalFolderRequestDTO folderRequest = new MedicalFolderRequestDTO("patient-kc-21", "doctor-kc-21", "O+", 1.70, 70.0);
        MedicalFolderController folderController = new MedicalFolderController(medicalFolderService);
        when(medicalFolderService.createMedicalFolder(any(MedicalFolder.class))).thenReturn(folder);
        assertThat(folderController.createMedicalFolder(folderRequest).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        when(medicalFolderService.getAllMedicalFolders()).thenReturn(List.of(folder));
        assertThat(folderController.getAllMedicalFolders().getBody()).hasSize(1);
        when(medicalFolderService.getMedicalFolderById(21L)).thenReturn(Optional.of(folder));
        assertThat(folderController.getMedicalFolderById(21L).getStatusCode()).isEqualTo(HttpStatus.OK);
        when(medicalFolderService.getMedicalFolderById(404L)).thenReturn(Optional.empty());
        assertThat(folderController.getMedicalFolderById(404L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        when(medicalFolderService.getMedicalFoldersByPatient("patient-kc-21")).thenReturn(List.of(folder));
        when(medicalFolderService.getMedicalFoldersByDoctor("doctor-kc-21")).thenReturn(List.of(folder));
        when(medicalFolderService.getMedicalFoldersByPatientAndDoctor("patient-kc-21", "doctor-kc-21")).thenReturn(List.of(folder));
        assertThat(folderController.getMedicalFoldersByPatient("patient-kc-21").getBody()).hasSize(1);
        assertThat(folderController.getMedicalFoldersByDoctor("doctor-kc-21").getBody()).hasSize(1);
        assertThat(folderController.getMedicalFoldersByPatientAndDoctor("patient-kc-21", "doctor-kc-21").getBody()).hasSize(1);
        when(medicalFolderService.updateMedicalFolder(eq(21L), any(MedicalFolder.class))).thenReturn(folder);
        assertThat(folderController.updateMedicalFolder(21L, folderRequest).getStatusCode()).isEqualTo(HttpStatus.OK);
        when(medicalFolderService.updateMedicalFolder(eq(404L), any(MedicalFolder.class))).thenThrow(new RuntimeException("missing"));
        assertThat(folderController.updateMedicalFolder(404L, folderRequest).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(folderController.deleteMedicalFolder(21L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(medicalFolderService).deleteMedicalFolder(21L);

        Session session = session(31L, folder, now);
        SessionRequestDTO sessionRequest = new SessionRequestDTO(21L, now, "Consultation mémoire");
        SessionController sessionController = new SessionController(sessionService);
        when(sessionService.createSessionForMedicalFolder(eq(21L), any(Session.class))).thenReturn(session);
        assertThat(sessionController.createSession(sessionRequest).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        when(sessionService.createSessionForMedicalFolder(eq(404L), any(Session.class))).thenThrow(new RuntimeException("missing"));
        assertThat(sessionController.createSession(new SessionRequestDTO(404L, now, "missing")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        when(sessionService.getAllSessions()).thenReturn(List.of(session));
        assertThat(sessionController.getAllSessions().getBody()).hasSize(1);
        assertThat(sessionController.getSessionsByPatient("patient-kc-21").getBody()).hasSize(1);
        when(sessionService.getSessionById(31L)).thenReturn(Optional.of(session));
        assertThat(sessionController.getSessionById(31L).getStatusCode()).isEqualTo(HttpStatus.OK);
        when(sessionService.getSessionById(404L)).thenReturn(Optional.empty());
        assertThat(sessionController.getSessionById(404L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        when(sessionService.getSessionsByMedicalFolder(21L)).thenReturn(List.of(session));
        when(sessionService.getSessionsWithoutPrescriptions(21L)).thenReturn(List.of(session));
        when(sessionService.getSessionsWithoutCarePlans(21L)).thenReturn(List.of(session));
        assertThat(sessionController.getSessionsByMedicalFolder(21L).getBody()).hasSize(1);
        assertThat(sessionController.getSessionsWithoutPrescriptions(21L).getBody()).hasSize(1);
        assertThat(sessionController.getSessionsWithoutCarePlans(21L).getBody()).hasSize(1);
        when(sessionService.updateSession(eq(31L), any(Session.class))).thenReturn(session);
        assertThat(sessionController.updateSession(31L, sessionRequest).getStatusCode()).isEqualTo(HttpStatus.OK);
        when(sessionService.updateSession(eq(404L), any(Session.class))).thenThrow(new RuntimeException("missing"));
        assertThat(sessionController.updateSession(404L, sessionRequest).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(sessionController.deleteSession(31L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(sessionService).deleteSession(31L);

        MedicationStatusController statusController = new MedicationStatusController(medicationStatusScheduler);
        assertThat(statusController.triggerStatusUpdate().getBody()).containsEntry("status", "success");
        verify(medicationStatusScheduler).updateAllMedicationStatuses();
        assertThat(statusController.initializeDates().getBody()).containsEntry("message", "Medication dates initialized");
        verify(medicationStatusScheduler).initializeMedicationDates();
        assertThat(statusController.discontinueMedication(77L, "effet indésirable").getBody())
                .containsEntry("medicationId", 77L)
                .containsEntry("reason", "effet indésirable");
        verify(medicationStatusScheduler).discontinueMedication(77L, "effet indésirable");
        MedicationStatusScheduler.MedicationStatusStats stats = new MedicationStatusScheduler.MedicationStatusStats(4, 1, 1, 1, 1);
        when(medicationStatusScheduler.getStatusStatistics()).thenReturn(stats);
        assertThat(statusController.getStatistics().getBody().active()).isEqualTo(1);
        assertThat(statusController.healthCheck().getBody()).containsEntry("service", "MedicationStatusScheduler");
    }

    private static MedicalFolder folder(Long id, String patientId, String doctorId, LocalDateTime now) {
        MedicalFolder folder = new MedicalFolder();
        folder.setId(id);
        folder.setIdPatient(patientId);
        folder.setIdDoctor(doctorId);
        folder.setCreatedAt(now.minusDays(2));
        folder.setUpdatedAt(now.minusDays(1));
        return folder;
    }

    private static Session session(Long id, MedicalFolder folder, LocalDateTime now) {
        Session session = new Session();
        session.setId(id);
        session.setMedicalFolder(folder);
        session.setSessionDate(now);
        session.setNotes("Consultation mémoire");
        session.setCreatedAt(now.minusHours(2));
        session.setUpdatedAt(now.minusHours(1));
        return session;
    }
}
