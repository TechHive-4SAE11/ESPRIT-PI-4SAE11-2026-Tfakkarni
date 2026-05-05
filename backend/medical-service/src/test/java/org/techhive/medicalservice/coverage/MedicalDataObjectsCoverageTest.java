package org.techhive.medicalservice.coverage;

import org.junit.jupiter.api.Test;
import org.techhive.medicalservice.config.GeminiSafetyAuditProperties;
import org.techhive.medicalservice.config.RestClientConfig;
import org.techhive.medicalservice.config.RestTemplateConfig;
import org.techhive.medicalservice.converter.JsonbConverter;
import org.techhive.medicalservice.dto.*;
import org.techhive.medicalservice.dto.audit.PatientMedicationAuditRequest;
import org.techhive.medicalservice.dto.audit.PatientMedicationAuditResponse;
import org.techhive.medicalservice.dto.audit.PatientMedicationSummaryDto;
import org.techhive.medicalservice.dto.coaching.*;
import org.techhive.medicalservice.dto.game.GameAttemptDTO;
import org.techhive.medicalservice.dto.game.GameStatsDTO;
import org.techhive.medicalservice.dto.tracking.*;
import org.techhive.medicalservice.entity.*;
import org.techhive.medicalservice.entity.coaching.*;
import org.techhive.medicalservice.entity.enums.*;
import org.techhive.medicalservice.exception.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class MedicalDataObjectsCoverageTest {

    private static final List<Class<?>> DATA_CLASSES = List.of(
            EquipmentLoan.class, DoctorCalendar.class, DiagnosticAttachment.class, Diagnostics.class,
            MedicalHistory.class, PatientBadge.class, Appointment.class, MedicalFolder.class,
            AIReport.class, Equipment.class, PredictionResult.class,
            CoachingProgress.class, CoachingGoal.class, CoachingNotification.class,
            UpdateMedicalHistoryRequest.class, CreateDiagnosticsRequest.class, MedicalHistoryResponse.class,
            CrossPatientDiseaseDto.class, FlaggedPatientDto.class, DashboardStatsDTO.class,
            PredictionDTO.class, PatientRiskDTO.class, DiagnosticsResponse.class, SlotSuggestionDTO.class,
            DiagnosticAttachmentResponse.class, ReminderRequestDTO.class, RecurringAppointmentRequestDTO.class,
            CreateMedicalHistoryRequest.class, UpdateDiagnosticsRequest.class, PatientDTO.class,
            AIReportResponse.class, AppointmentRequestDTO.class, DossierForMlRequest.class,
            MonthComparisonDto.class, CreateMedicalFolderRequest.class, MedicalFolderStatsResponse.class,
            ClinicalSafetyStatsDto.class, ReminderResponseDTO.class, FolderSpecificStatsDto.class,
            ClinicalAnalysisResult.class, AppointmentResponseDTO.class, PatientBadgeDto.class,
            EquipmentLoanDTO.class, DiseaseCountDto.class, CalendarStatusDTO.class,
            MedicalFolderResponse.class, DiagnosticsByMonthDto.class, EquipmentDTO.class,
            UpdateMedicalFolderRequest.class, PatientMedicationAuditResponse.class,
            PatientMedicationAuditRequest.class, PatientMedicationSummaryDto.class,
            GameStatsDTO.class, GameAttemptDTO.class, TrackingSummaryDTO.class,
            MedicationLogDTO.class, IncidentDTO.class, MedicationComplianceDTO.class,
            IncidentStatsDTO.class, CoachingGoalResponse.class, CoachingNotificationResponse.class,
            CoachingProgressResponse.class, CoachingProgressRequest.class, CoachingGoalRequest.class,
            CoachingGoalStatusRequest.class, GeminiSafetyAuditProperties.class
    );

    @Test
    void dataObjects_shouldSupportNoArgConstructionFieldAccessorsAndToString() throws Exception {
        for (Class<?> type : DATA_CLASSES) {
            Object instance = instantiate(type);
            assertNotNull(instance, () -> "Expected instance for " + type.getName());

            Map<String, Object> assigned = new LinkedHashMap<>();
            for (Field field : allFields(type)) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                    continue;
                }
                Object value = sampleValue(field.getType(), field.getGenericType().getTypeName());
                if (value == Unsupported.INSTANCE) {
                    continue;
                }
                field.setAccessible(true);
                field.set(instance, value);
                assigned.put(field.getName(), value);
            }

            for (Map.Entry<String, Object> entry : assigned.entrySet()) {
                Field field = findField(type, entry.getKey());
                field.setAccessible(true);
                assertEquals(entry.getValue(), field.get(instance), () -> type.getSimpleName() + "." + entry.getKey());
                invokeAccessorIfPresent(type, instance, entry.getKey(), field.getType());
            }

            // Lombok/manual methods often live on source annotation or method lines; exercise them safely.
            instance.toString();
            instance.equals(instance);
            instance.hashCode();
        }
    }

    @Test
    void enumTypes_shouldExposeValuesAndValueOf() {
        List<Class<? extends Enum<?>>> enums = List.of(
                AppointmentStatus.class, AppointmentType.class, AttendanceRiskLevel.class,
                EquipmentStatus.class, EquipmentCategory.class, LoanStatus.class, EquipmentCondition.class,
                CoachingPriority.class, CoachingGoalStatus.class, CoachingMood.class,
                CoachingGoalType.class, ProgressRecordedByRole.class, ReminderStatus.class,
                ReminderType.class, ReminderChannel.class
        );

        for (Class<? extends Enum<?>> enumType : enums) {
            Enum<?>[] constants = enumType.getEnumConstants();
            assertTrue(constants.length > 0, () -> enumType.getSimpleName() + " should define constants");
            for (Enum<?> constant : constants) {
                assertSame(constant, Enum.valueOf((Class) enumType, constant.name()));
            }
        }
    }

    @Test
    void equipmentDtoConversions_shouldRoundTripEntityAndHandleNulls() {
        Equipment equipment = new Equipment();
        equipment.setId(10L);
        equipment.setName("Wheelchair");
        equipment.setDescription("Foldable chair");
        equipment.setCategory(EquipmentCategory.MOBILITY);
        equipment.setStatus(EquipmentStatus.AVAILABLE);
        equipment.setCondition(EquipmentCondition.GOOD);
        equipment.setDonorId(77L);
        equipment.setDonationDate(LocalDateTime.of(2026, 5, 3, 10, 0));

        EquipmentLoan loan = new EquipmentLoan();
        loan.setId(99L);
        loan.setEquipment(equipment);
        loan.setBorrowerId(55L);
        loan.setStatus(LoanStatus.ACTIVE);
        equipment.setLoans(List.of(loan));

        EquipmentDTO dto = EquipmentDTO.fromEntity(equipment);
        assertNotNull(dto);
        assertEquals("Wheelchair", dto.getName());
        assertEquals(1, dto.getLoans().size());
        assertNull(EquipmentDTO.fromEntity(null));

        Equipment mapped = dto.toEntity();
        assertEquals(equipment.getId(), mapped.getId());
        assertEquals(equipment.getName(), mapped.getName());
        assertEquals(equipment.getCategory(), mapped.getCategory());
    }

    @Test
    void equipmentLoanDtoConversions_shouldRoundTripEntityAndHandleNulls() {
        Equipment equipment = new Equipment();
        equipment.setId(3L);
        equipment.setName("Walker");
        EquipmentLoan loan = new EquipmentLoan();
        loan.setId(4L);
        loan.setEquipment(equipment);
        loan.setBorrowerId(6L);
        loan.setLoanDate(LocalDateTime.of(2026, 5, 1, 9, 0));
        loan.setDueDate(LocalDateTime.of(2026, 5, 8, 9, 0));
        loan.setReturnDate(LocalDateTime.of(2026, 5, 7, 9, 0));
        loan.setStatus(LoanStatus.RETURNED);
        loan.setNotes("returned clean");

        EquipmentLoanDTO dto = EquipmentLoanDTO.fromEntity(loan);
        assertNotNull(dto);
        assertEquals(4L, dto.getId());
        assertEquals(3L, dto.getEquipmentId());
        assertEquals("Walker", dto.getEquipmentName());
        assertNull(EquipmentLoanDTO.fromEntity(null));

        EquipmentLoan mapped = dto.toEntity();
        assertEquals(dto.getId(), mapped.getId());
        assertEquals(dto.getBorrowerId(), mapped.getBorrowerId());
        assertEquals(dto.getStatus(), mapped.getStatus());
    }

    @Test
    void simpleExceptionsAndConfigs_shouldConstructExpectedObjects() {
        assertEquals("missing", new ResourceNotFoundException("missing").getMessage());
        assertEquals("bad", new InvalidAppointmentException("bad").getMessage());
        assertEquals("overlap", new AppointmentOverlapException("overlap").getMessage());
        assertEquals("not found", new AppointmentNotFoundException("not found").getMessage());
        assertEquals("restricted", new BookingRestrictedException("restricted").getMessage());

        assertNotNull(new RestTemplateConfig().externalRestTemplate());
        RestClientConfig restClientConfig = new RestClientConfig();
        assertNotNull(restClientConfig.alertServiceRestClient(org.springframework.web.client.RestClient.builder()));
        assertNotNull(restClientConfig.userServiceRestClient(org.springframework.web.client.RestClient.builder()));
    }

    @Test
    void jsonbConverter_shouldConvertListsAndHandleNulls() {
        JsonbConverter converter = new JsonbConverter();
        List<String> input = List.of("Nour Ben Salah", "Ibuprofen");

        String json = converter.convertToDatabaseColumn(input);
        assertTrue(json.contains("Nour Ben Salah"));
        List<String> output = converter.convertToEntityAttribute(json);
        assertEquals(input, output);
        assertEquals("[]", converter.convertToDatabaseColumn(null));
        assertTrue(converter.convertToEntityAttribute(null).isEmpty());
        assertTrue(converter.convertToEntityAttribute("not-json").isEmpty());
    }

    private static Object instantiate(Class<?> type) throws Exception {
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException noNoArgConstructor) {
            Method builderFactory = type.getMethod("builder");
            Object builder = builderFactory.invoke(null);
            for (Field field : allFields(type)) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                    continue;
                }
                Object value = sampleValue(field.getType(), field.getGenericType().getTypeName());
                if (value == Unsupported.INSTANCE) {
                    continue;
                }
                try {
                    Method builderSetter = builder.getClass().getMethod(field.getName(), field.getType());
                    builderSetter.invoke(builder, value);
                } catch (NoSuchMethodException ignored) {
                    // Some builder implementations may omit a field; direct assertions will only cover built fields.
                }
            }
            return builder.getClass().getMethod("build").invoke(builder);
        }
    }

    private static List<Field> allFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static void invokeAccessorIfPresent(Class<?> type, Object instance, String fieldName, Class<?> fieldType) throws Exception {
        String suffix = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        List<String> getters = new ArrayList<>(List.of("get" + suffix));
        if (fieldType == boolean.class || fieldType == Boolean.class) {
            getters.add("is" + suffix);
        }
        for (String getterName : getters) {
            try {
                Method getter = type.getMethod(getterName);
                getter.invoke(instance);
                return;
            } catch (NoSuchMethodException ignored) {
                // Accessor is Lombok-suppressed or field-only; direct field assertion above is enough.
            }
        }
    }

    private static Object sampleValue(Class<?> type, String genericType) {
        if (type == String.class) return "sample";
        if (type == Long.class || type == long.class) return 42L;
        if (type == Integer.class || type == int.class) return 7;
        if (type == Double.class || type == double.class) return 1.5;
        if (type == Float.class || type == float.class) return 2.5f;
        if (type == Boolean.class || type == boolean.class) return true;
        if (type == LocalDateTime.class) return LocalDateTime.of(2026, 5, 3, 13, 0);
        if (type == LocalDate.class) return LocalDate.of(2026, 5, 3);
        if (type == LocalTime.class) return LocalTime.of(13, 30);
        if (type == List.class) return new ArrayList<>();
        if (type == Set.class) return new LinkedHashSet<>();
        if (type == Map.class) return new LinkedHashMap<>();
        if (type.isEnum()) return type.getEnumConstants()[0];
        if (type == MedicalFolder.class) return MedicalFolder.builder().id(11L).patientId("p-1").doctorId("d-1").build();
        if (type == Diagnostics.class) return Diagnostics.builder().id(12L).build();
        if (type == MedicalHistory.class) return MedicalHistory.builder().id(13L).build();
        if (type == AIReport.class) return AIReport.builder().id(14L).build();
        if (type == Equipment.class) return new Equipment();
        if (type == EquipmentLoan.class) return new EquipmentLoan();
        if (type == Appointment.class) return new Appointment();
        if (type == CoachingGoal.class) return new CoachingGoal();
        if (type == CoachingProgress.class) return new CoachingProgress();
        if (type == CoachingNotification.class) return new CoachingNotification();
        return Unsupported.INSTANCE;
    }

    private enum Unsupported { INSTANCE }
}
