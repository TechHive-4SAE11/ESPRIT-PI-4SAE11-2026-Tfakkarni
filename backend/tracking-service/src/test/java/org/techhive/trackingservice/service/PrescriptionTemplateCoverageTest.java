package org.techhive.trackingservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.techhive.trackingservice.controller.PrescriptionTemplateController;
import org.techhive.trackingservice.dto.MedicationRequestDTO;
import org.techhive.trackingservice.dto.PrescriptionTemplateRequestDTO;
import org.techhive.trackingservice.dto.PrescriptionTemplateResponseDTO;
import org.techhive.trackingservice.entity.Medication;
import org.techhive.trackingservice.entity.Prescription;
import org.techhive.trackingservice.entity.PrescriptionTemplate;
import org.techhive.trackingservice.entity.TemplateMedication;
import org.techhive.trackingservice.mapper.PrescriptionTemplateMapper;
import org.techhive.trackingservice.repository.PrescriptionRepository;
import org.techhive.trackingservice.repository.PrescriptionTemplateRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionTemplateCoverageTest {

    @Mock PrescriptionTemplateRepository templateRepository;
    @Mock PrescriptionRepository prescriptionRepository;

    private PrescriptionTemplateMapper mapper;
    private PrescriptionTemplateService service;

    @BeforeEach
    void setUp() {
        mapper = new PrescriptionTemplateMapper();
        service = new PrescriptionTemplateService(templateRepository, prescriptionRepository, mapper);
    }

    @Test
    void serviceCreatesTemplatesFromScratchAndPrescriptionAndMapsRelationships() {
        PrescriptionTemplate template = template("Plan mémoire", "doctor-kc");
        template.setMedications(new ArrayList<>(List.of(templateMedication("Donepezil"))));
        when(templateRepository.save(template)).thenReturn(template);

        PrescriptionTemplate created = service.createTemplate(template);

        assertThat(created).isSameAs(template);
        assertThat(created.getMedications()).allSatisfy(med -> assertThat(med.getPrescriptionTemplate()).isSameAs(template));

        Medication medication = new Medication();
        medication.setMedicationName("Rivastigmine");
        medication.setDosage("1 patch");
        medication.setFrequency("Quotidien");
        medication.setDuration("30 jours");
        medication.setInstructions("Le matin");
        Prescription prescription = new Prescription();
        prescription.setId(44L);
        prescription.setMedications(List.of(medication));
        when(prescriptionRepository.findById(44L)).thenReturn(Optional.of(prescription));
        when(templateRepository.save(any(PrescriptionTemplate.class))).thenAnswer(inv -> {
            PrescriptionTemplate saved = inv.getArgument(0);
            saved.setId(45L);
            return saved;
        });

        PrescriptionTemplate fromPrescription = service.createFromPrescription(44L, "Depuis ordonnance", "description", "doctor-kc");

        assertThat(fromPrescription.getId()).isEqualTo(45L);
        assertThat(fromPrescription.getMedications()).hasSize(1);
        assertThat(fromPrescription.getMedications().get(0).getMedicationName()).isEqualTo("Rivastigmine");
        assertThat(fromPrescription.getMedications().get(0).getPrescriptionTemplate()).isSameAs(fromPrescription);

        Prescription prescriptionWithoutMeds = new Prescription();
        prescriptionWithoutMeds.setMedications(null);
        when(prescriptionRepository.findById(46L)).thenReturn(Optional.of(prescriptionWithoutMeds));
        PrescriptionTemplate noMeds = service.createFromPrescription(46L, "Vide", null, "doctor-kc");
        assertThat(noMeds.getMedications()).isEmpty();

        when(prescriptionRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createFromPrescription(404L, "missing", null, "doctor-kc"))
                .hasMessageContaining("Prescription not found");
    }

    @Test
    void serviceReadsSearchesUpdatesDeletesAndMapperHandlesNullMedicationList() {
        PrescriptionTemplate template = template("Suivi Alzheimer", "doctor-kc");
        TemplateMedication med = templateMedication("Memantine");
        template.setMedications(new ArrayList<>(List.of(med)));
        when(templateRepository.findByDoctorIdOrderByCreatedAtDesc("doctor-kc")).thenReturn(List.of(template));
        when(templateRepository.findById(5L)).thenReturn(Optional.of(template));
        when(templateRepository.findByDoctorIdAndNameContainingIgnoreCase("doctor-kc", "alz")).thenReturn(List.of(template));

        assertThat(service.getTemplatesByDoctor("doctor-kc")).containsExactly(template);
        assertThat(service.getTemplateById(5L)).contains(template);
        assertThat(service.searchTemplates("doctor-kc", "alz")).containsExactly(template);

        PrescriptionTemplate updated = template("Plan actualisé", "doctor-kc");
        updated.setDescription("Nouvelle description");
        updated.setMedications(new ArrayList<>(List.of(templateMedication("Galantamine"))));
        when(templateRepository.save(template)).thenReturn(template);
        PrescriptionTemplate result = service.updateTemplate(5L, updated);
        assertThat(result.getName()).isEqualTo("Plan actualisé");
        assertThat(result.getDescription()).isEqualTo("Nouvelle description");
        assertThat(result.getMedications()).hasSize(1);
        assertThat(result.getMedications().get(0).getPrescriptionTemplate()).isSameAs(result);

        PrescriptionTemplate noMedicationTemplate = template("Sans meds", "doctor-kc");
        noMedicationTemplate.setMedications(null);
        PrescriptionTemplateResponseDTO dto = mapper.toResponseDTO(noMedicationTemplate);
        assertThat(dto.getMedications()).isEmpty();

        MedicationRequestDTO requestDTO = new MedicationRequestDTO("Vitamine D", "1000 UI", "Jour", "60 jours", "Avec repas");
        TemplateMedication mappedMed = mapper.toTemplateMedicationEntity(requestDTO);
        assertThat(mappedMed.getMedicationName()).isEqualTo("Vitamine D");
        assertThat(mapper.toTemplateMedicationDTO(mappedMed).getDosage()).isEqualTo("1000 UI");

        when(templateRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateTemplate(404L, updated)).hasMessageContaining("Template not found");
        when(templateRepository.existsById(6L)).thenReturn(true);
        service.deleteTemplate(6L);
        verify(templateRepository).deleteById(6L);
        when(templateRepository.existsById(404L)).thenReturn(false);
        assertThatThrownBy(() -> service.deleteTemplate(404L)).hasMessageContaining("Template not found");
    }

    @Test
    @SuppressWarnings("unchecked")
    void controllerMapsCrudSuccessAndErrorResponses() {
        PrescriptionTemplateService mockService = mock(PrescriptionTemplateService.class);
        PrescriptionTemplateMapper mapper = new PrescriptionTemplateMapper();
        PrescriptionTemplateController controller = new PrescriptionTemplateController(mockService, mapper);
        PrescriptionTemplate template = template("Urgence", "doctor-kc");
        template.setMedications(new ArrayList<>(List.of(templateMedication("Lorazepam"))));
        PrescriptionTemplateRequestDTO request = new PrescriptionTemplateRequestDTO(
                "Urgence", "Crise", "doctor-kc",
                List.of(new MedicationRequestDTO("Lorazepam", "1mg", "SOS", "1 jour", "Sur avis médical"))
        );

        when(mockService.createTemplate(any(PrescriptionTemplate.class))).thenReturn(template);
        ResponseEntity<?> created = controller.createTemplate(request);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(((PrescriptionTemplateResponseDTO) created.getBody()).getName()).isEqualTo("Urgence");

        when(mockService.createTemplate(any(PrescriptionTemplate.class))).thenThrow(new RuntimeException("db down"));
        ResponseEntity<?> createError = controller.createTemplate(request);
        assertThat(createError.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat((Map<String, String>) createError.getBody()).containsEntry("error", "Failed to create template: db down");

        when(mockService.createFromPrescription(9L, "Depuis Rx", "desc", "doctor-kc")).thenReturn(template);
        assertThat(controller.createFromPrescription(9L, "Depuis Rx", "desc", "doctor-kc").getStatusCode()).isEqualTo(HttpStatus.CREATED);
        when(mockService.createFromPrescription(404L, "Depuis Rx", "desc", "doctor-kc")).thenThrow(new RuntimeException("missing"));
        ResponseEntity<?> fromError = controller.createFromPrescription(404L, "Depuis Rx", "desc", "doctor-kc");
        assertThat(fromError.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((Map<String, String>) fromError.getBody()).containsEntry("error", "missing");

        when(mockService.getTemplatesByDoctor("doctor-kc")).thenReturn(List.of(template));
        assertThat(controller.getTemplatesByDoctor("doctor-kc").getBody()).extracting(PrescriptionTemplateResponseDTO::getName).containsExactly("Urgence");
        when(mockService.getTemplateById(7L)).thenReturn(Optional.of(template));
        assertThat(controller.getTemplateById(7L).getStatusCode()).isEqualTo(HttpStatus.OK);
        when(mockService.getTemplateById(404L)).thenReturn(Optional.empty());
        assertThat(controller.getTemplateById(404L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        when(mockService.searchTemplates("doctor-kc", "urg")).thenReturn(List.of(template));
        assertThat(controller.searchTemplates("doctor-kc", "urg").getBody()).hasSize(1);

        when(mockService.updateTemplate(any(Long.class), any(PrescriptionTemplate.class))).thenReturn(template);
        assertThat(controller.updateTemplate(7L, request).getStatusCode()).isEqualTo(HttpStatus.OK);
        when(mockService.updateTemplate(any(Long.class), any(PrescriptionTemplate.class))).thenThrow(new RuntimeException("missing"));
        assertThat(controller.updateTemplate(404L, request).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(controller.deleteTemplate(7L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        doThrow(new RuntimeException("missing")).when(mockService).deleteTemplate(404L);
        assertThat(controller.deleteTemplate(404L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private PrescriptionTemplate template(String name, String doctorId) {
        PrescriptionTemplate template = new PrescriptionTemplate();
        template.setId(5L);
        template.setName(name);
        template.setDescription("Description");
        template.setDoctorId(doctorId);
        template.setCreatedAt(LocalDateTime.of(2026, 5, 3, 15, 40));
        template.setUpdatedAt(LocalDateTime.of(2026, 5, 3, 15, 41));
        return template;
    }

    private TemplateMedication templateMedication(String name) {
        TemplateMedication medication = new TemplateMedication();
        medication.setId(8L);
        medication.setMedicationName(name);
        medication.setDosage("10mg");
        medication.setFrequency("Matin");
        medication.setDuration("30 jours");
        medication.setInstructions("Après repas");
        return medication;
    }
}
