package org.techhive.trackingservice.mapper;

import org.junit.jupiter.api.Test;
import org.techhive.trackingservice.dto.CareActivityRequestDTO;
import org.techhive.trackingservice.dto.CareActivityResponseDTO;
import org.techhive.trackingservice.dto.CarePlanRequestDTO;
import org.techhive.trackingservice.dto.CarePlanResponseDTO;
import org.techhive.trackingservice.dto.MedicationRequestDTO;
import org.techhive.trackingservice.dto.MedicationResponseDTO;
import org.techhive.trackingservice.dto.PrescriptionResponseDTO;
import org.techhive.trackingservice.dto.PrescriptionTemplateResponseDTO;
import org.techhive.trackingservice.dto.TemplateMedicationDTO;
import org.techhive.trackingservice.entity.CareActivity;
import org.techhive.trackingservice.entity.CarePlan;
import org.techhive.trackingservice.entity.MedicalFolder;
import org.techhive.trackingservice.entity.Medication;
import org.techhive.trackingservice.entity.Prescription;
import org.techhive.trackingservice.entity.PrescriptionTemplate;
import org.techhive.trackingservice.entity.Session;
import org.techhive.trackingservice.entity.TemplateMedication;
import org.techhive.trackingservice.enums.CareActivityType;
import org.techhive.trackingservice.enums.MedicationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MapperCoverageTest {

    private final CarePlanMapper carePlanMapper = new CarePlanMapper();
    private final PrescriptionMapper prescriptionMapper = new PrescriptionMapper();
    private final PrescriptionTemplateMapper prescriptionTemplateMapper = new PrescriptionTemplateMapper();

    @Test
    void carePlanMapperHandlesNullAndRoundTripsActivities() {
        assertThat(carePlanMapper.toEntity(null)).isNull();
        assertThat(carePlanMapper.toResponseDTO(null)).isNull();
        assertThat(carePlanMapper.toActivityEntity(null)).isNull();
        assertThat(carePlanMapper.toActivityResponseDTO(null)).isNull();

        CareActivityRequestDTO activityRequest = CareActivityRequestDTO.builder()
                .activityName("Marche douce")
                .activityType(CareActivityType.PHYSICAL_ACTIVITY)
                .description("Marcher dix minutes")
                .frequency("daily")
                .duration("10 min")
                .build();
        CarePlanRequestDTO request = new CarePlanRequestDTO(9L, List.of(activityRequest));

        CarePlan entity = carePlanMapper.toEntity(request);

        assertThat(entity.getCareActivities()).hasSize(1);
        CareActivity activity = entity.getCareActivities().get(0);
        assertThat(activity.getActivityName()).isEqualTo("Marche douce");
        assertThat(activity.getActivityType()).isEqualTo(CareActivityType.PHYSICAL_ACTIVITY);
        assertThat(activity.getDescription()).isEqualTo("Marcher dix minutes");
        assertThat(activity.getCompletionStatus()).isEqualTo("PENDING");

        CarePlan emptyPlan = carePlanMapper.toEntity(new CarePlanRequestDTO(10L, null));
        assertThat(emptyPlan.getCareActivities()).isEmpty();
    }

    @Test
    void carePlanMapperBuildsResponseWithSessionDoctorAndDefaultActivityType() {
        MedicalFolder folder = new MedicalFolder();
        folder.setIdDoctor("doctor-77");
        Session session = new Session();
        session.setId(123L);
        session.setMedicalFolder(folder);

        CareActivity activity = new CareActivity();
        activity.setId(5L);
        activity.setActivityName("Hydratation")
        ;
        activity.setDescription("Boire de l'eau")
        ;
        activity.setFrequency("twice daily");
        activity.setDuration("5 min");
        activity.setCompletionStatus("DONE");
        activity.setCreatedAt(LocalDateTime.of(2026, 5, 3, 9, 0));

        CarePlan carePlan = new CarePlan();
        carePlan.setId(44L);
        carePlan.setSession(session);
        carePlan.setCareActivities(List.of(activity));
        carePlan.setCreatedAt(LocalDateTime.of(2026, 5, 3, 8, 0));
        carePlan.setUpdatedAt(LocalDateTime.of(2026, 5, 3, 10, 0));

        CarePlanResponseDTO response = carePlanMapper.toResponseDTO(carePlan);

        assertThat(response.getId()).isEqualTo(44L);
        assertThat(response.getSessionId()).isEqualTo(123L);
        assertThat(response.getDoctorId()).isEqualTo("doctor-77");
        assertThat(response.getActivities()).hasSize(1);
        CareActivityResponseDTO activityResponse = response.getActivities().get(0);
        assertThat(activityResponse.getActivityName()).isEqualTo("Hydratation");
        assertThat(activityResponse.getActivityType()).isEqualTo(CareActivityType.PHYSICAL_ACTIVITY);
        assertThat(activityResponse.getCompletionStatus()).isEqualTo("DONE");
    }

    @Test
    void prescriptionMapperMapsMedicationsAndSessionContext() {
        MedicationRequestDTO request = new MedicationRequestDTO("Paracétamol", "500mg", "2 fois/jour", "5 days", "Après repas");
        Medication medication = prescriptionMapper.toMedicationEntity(request);

        assertThat(medication.getMedicationName()).isEqualTo("Paracétamol");
        assertThat(medication.getDosage()).isEqualTo("500mg");
        assertThat(medication.getFrequency()).isEqualTo("2 fois/jour");
        assertThat(medication.getDuration()).isEqualTo("5 days");
        assertThat(medication.getInstructions()).isEqualTo("Après repas");

        MedicalFolder folder = new MedicalFolder();
        folder.setIdDoctor("doctor-42");
        Session session = new Session();
        session.setId(31L);
        session.setSessionDate(LocalDateTime.of(2026, 5, 3, 14, 0));
        session.setMedicalFolder(folder);
        Prescription prescription = new Prescription();
        prescription.setId(6L);
        prescription.setSession(session);
        prescription.setCreatedAt(LocalDateTime.of(2026, 5, 3, 11, 0));
        prescription.setUpdatedAt(LocalDateTime.of(2026, 5, 3, 11, 30));
        medication.setId(7L);
        medication.setStatus(MedicationStatus.ACTIVE);
        medication.setStartDate(LocalDate.of(2026, 5, 3));
        medication.setEndDate(LocalDate.of(2026, 5, 8));
        medication.setCreatedAt(LocalDateTime.of(2026, 5, 3, 11, 5));
        medication.setPrescription(prescription);
        prescription.setMedications(List.of(medication));

        MedicationResponseDTO medicationResponse = prescriptionMapper.toMedicationResponseDTO(medication);
        PrescriptionResponseDTO prescriptionResponse = prescriptionMapper.toResponseDTO(prescription);

        assertThat(medicationResponse.getId()).isEqualTo(7L);
        assertThat(medicationResponse.getSessionId()).isEqualTo(31L);
        assertThat(medicationResponse.getSessionDate()).isEqualTo(LocalDateTime.of(2026, 5, 3, 14, 0));
        assertThat(medicationResponse.getDoctorId()).isEqualTo("doctor-42");
        assertThat(prescriptionResponse.getId()).isEqualTo(6L);
        assertThat(prescriptionResponse.getSessionId()).isEqualTo(31L);
        assertThat(prescriptionResponse.getDoctorId()).isEqualTo("doctor-42");
        assertThat(prescriptionResponse.getMedications()).hasSize(1);

        Medication detached = new Medication();
        detached.setMedicationName("Vitamine D");
        MedicationResponseDTO detachedResponse = prescriptionMapper.toMedicationResponseDTO(detached);
        assertThat(detachedResponse.getSessionId()).isNull();
        assertThat(detachedResponse.getDoctorId()).isNull();
    }

    @Test
    void prescriptionMapperHandlesEmptyMedicationList() {
        Session session = new Session();
        session.setId(3L);
        Prescription prescription = new Prescription();
        prescription.setId(2L);
        prescription.setSession(session);
        prescription.setMedications(null);

        PrescriptionResponseDTO response = prescriptionMapper.toResponseDTO(prescription);

        assertThat(response.getMedications()).isEmpty();
        assertThat(response.getDoctorId()).isNull();
    }

    @Test
    void prescriptionTemplateMapperMapsTemplateAndMedicationDtos() {
        MedicationRequestDTO request = new MedicationRequestDTO("Amoxicilline", "1g", "3 fois/jour", "7 days", "Avec nourriture");
        TemplateMedication medication = prescriptionTemplateMapper.toTemplateMedicationEntity(request);
        medication.setId(12L);

        assertThat(medication.getMedicationName()).isEqualTo("Amoxicilline");
        assertThat(medication.getDosage()).isEqualTo("1g");
        assertThat(medication.getFrequency()).isEqualTo("3 fois/jour");
        assertThat(medication.getDuration()).isEqualTo("7 days");
        assertThat(medication.getInstructions()).isEqualTo("Avec nourriture");

        TemplateMedicationDTO medicationDTO = prescriptionTemplateMapper.toTemplateMedicationDTO(medication);
        assertThat(medicationDTO.getId()).isEqualTo(12L);
        assertThat(medicationDTO.getMedicationName()).isEqualTo("Amoxicilline");

        PrescriptionTemplate template = new PrescriptionTemplate();
        template.setId(77L);
        template.setName("Traitement sinusite")
        ;
        template.setDescription("Protocole court");
        template.setDoctorId("doctor-55");
        template.setMedications(List.of(medication));
        template.setCreatedAt(LocalDateTime.of(2026, 5, 3, 12, 0));
        template.setUpdatedAt(LocalDateTime.of(2026, 5, 3, 13, 0));

        PrescriptionTemplateResponseDTO response = prescriptionTemplateMapper.toResponseDTO(template);

        assertThat(response.getId()).isEqualTo(77L);
        assertThat(response.getName()).isEqualTo("Traitement sinusite");
        assertThat(response.getDoctorId()).isEqualTo("doctor-55");
        assertThat(response.getMedications()).hasSize(1);

        template.setMedications(null);
        assertThat(prescriptionTemplateMapper.toResponseDTO(template).getMedications()).isEmpty();
    }
}
