package org.techhive.trackingservice.coverage;

import org.junit.jupiter.api.Test;
import org.techhive.trackingservice.enums.CareActivityType;
import org.techhive.trackingservice.enums.MedicationStatus;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DataObjectCoverageTest {

    private static final List<Class<?>> DATA_CLASSES = List.of(
            org.techhive.trackingservice.dto.ActivityEntryRequest.class,
            org.techhive.trackingservice.dto.ActivityEntryResponse.class,
            org.techhive.trackingservice.dto.ActivityTrendResponse.class,
            org.techhive.trackingservice.dto.AvailableMedicationDTO.class,
            org.techhive.trackingservice.dto.CareActivityRequestDTO.class,
            org.techhive.trackingservice.dto.CareActivityResponseDTO.class,
            org.techhive.trackingservice.dto.CarePlanRequestDTO.class,
            org.techhive.trackingservice.dto.CarePlanResponseDTO.class,
            org.techhive.trackingservice.dto.CreateMeetingRequest.class,
            org.techhive.trackingservice.dto.CreateRatingRequest.class,
            org.techhive.trackingservice.dto.DailyLogResponse.class,
            org.techhive.trackingservice.dto.DoctorRankingResponse.class,
            org.techhive.trackingservice.dto.DoctorRatingResponse.class,
            org.techhive.trackingservice.dto.EndMeetingRequest.class,
            org.techhive.trackingservice.dto.ErrorResponse.class,
            org.techhive.trackingservice.dto.FollowUpReminderResponse.class,
            org.techhive.trackingservice.dto.HealthScoreResponse.class,
            org.techhive.trackingservice.dto.HealthScoreResponse.CategoryBreakdown.class,
            org.techhive.trackingservice.dto.HydrationTrendResponse.class,
            org.techhive.trackingservice.dto.IncidentEntryRequest.class,
            org.techhive.trackingservice.dto.IncidentEntryResponse.class,
            org.techhive.trackingservice.dto.IncidentStatsResponse.class,
            org.techhive.trackingservice.dto.MedicalFolderRequestDTO.class,
            org.techhive.trackingservice.dto.MedicalFolderResponseDTO.class,
            org.techhive.trackingservice.dto.MedicamentValidationResultDTO.class,
            org.techhive.trackingservice.dto.MedicationComplianceResponse.class,
            org.techhive.trackingservice.dto.MedicationComplianceResponse.CompliancePoint.class,
            org.techhive.trackingservice.dto.MedicationIntakeLogRequest.class,
            org.techhive.trackingservice.dto.MedicationIntakeLogResponse.class,
            org.techhive.trackingservice.dto.MedicationIntakeRequest.class,
            org.techhive.trackingservice.dto.MedicationIntakeResponse.class,
            org.techhive.trackingservice.dto.MedicationRequestDTO.class,
            org.techhive.trackingservice.dto.MedicationResponseDTO.class,
            org.techhive.trackingservice.dto.MeetingResponse.class,
            org.techhive.trackingservice.dto.MeetingSummaryResponse.class,
            org.techhive.trackingservice.dto.NotificationResponse.class,
            org.techhive.trackingservice.dto.NutritionEntryRequest.class,
            org.techhive.trackingservice.dto.NutritionEntryResponse.class,
            org.techhive.trackingservice.dto.PagedResponse.class,
            org.techhive.trackingservice.dto.PartialSummaryResponse.class,
            org.techhive.trackingservice.dto.PatientAnswerDTO.class,
            org.techhive.trackingservice.dto.PatientMedicationAuditRequest.class,
            org.techhive.trackingservice.dto.PatientMedicationAuditResponse.class,
            org.techhive.trackingservice.dto.PatientMedicationSummaryDto.class,
            org.techhive.trackingservice.dto.PatientQuestionDTO.class,
            org.techhive.trackingservice.dto.PrescriptionRequestDTO.class,
            org.techhive.trackingservice.dto.PrescriptionResponseDTO.class,
            org.techhive.trackingservice.dto.PrescriptionTemplateRequestDTO.class,
            org.techhive.trackingservice.dto.PrescriptionTemplateResponseDTO.class,
            org.techhive.trackingservice.dto.QuestionnaireSubmissionDTO.class,
            org.techhive.trackingservice.dto.RecommendationResponseDTO.class,
            org.techhive.trackingservice.dto.SaveTranscriptRequest.class,
            org.techhive.trackingservice.dto.ScoreTrendResponse.class,
            org.techhive.trackingservice.dto.SessionRequestDTO.class,
            org.techhive.trackingservice.dto.SessionResponseDTO.class,
            org.techhive.trackingservice.dto.StreakResponse.class,
            org.techhive.trackingservice.dto.StreakResponse.StreakDay.class,
            org.techhive.trackingservice.dto.TemplateMedicationDTO.class,
            org.techhive.trackingservice.dto.UpdateNotesRequest.class,
            org.techhive.trackingservice.entity.ActivityEntry.class,
            org.techhive.trackingservice.entity.CareActivity.class,
            org.techhive.trackingservice.entity.CarePlan.class,
            org.techhive.trackingservice.entity.DailyLog.class,
            org.techhive.trackingservice.entity.DoctorNotification.class,
            org.techhive.trackingservice.entity.DoctorRating.class,
            org.techhive.trackingservice.entity.FollowUpReminder.class,
            org.techhive.trackingservice.entity.IncidentEntry.class,
            org.techhive.trackingservice.entity.MedicalFolder.class,
            org.techhive.trackingservice.entity.MedicalMeeting.class,
            org.techhive.trackingservice.entity.Medication.class,
            org.techhive.trackingservice.entity.MedicationIntakeLog.class,
            org.techhive.trackingservice.entity.NutritionEntry.class,
            org.techhive.trackingservice.entity.PatientAnswer.class,
            org.techhive.trackingservice.entity.Prescription.class,
            org.techhive.trackingservice.entity.PrescriptionTemplate.class,
            org.techhive.trackingservice.entity.Question.class,
            org.techhive.trackingservice.entity.Questionnaire.class,
            org.techhive.trackingservice.entity.Session.class,
            org.techhive.trackingservice.entity.TemplateMedication.class
    );

    @Test
    void noArgDataObjectsExposeWritableAndReadableProperties() throws Exception {
        int exercisedClasses = 0;
        int exercisedAccessors = 0;

        for (Class<?> type : DATA_CLASSES) {
            Object instance = instantiateNoArg(type);
            if (instance == null) {
                continue;
            }
            exercisedClasses++;
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                Object value = sampleValue(field.getType(), field.getName());
                if (value == Unsupported.INSTANCE) {
                    continue;
                }
                Method setter = findSetter(type, field, value);
                Method getter = findGetter(type, field);
                if (setter == null || getter == null) {
                    continue;
                }
                setter.invoke(instance, value);
                assertThat(getter.invoke(instance)).isEqualTo(value);
                exercisedAccessors++;
            }
            invokeLifecycle(instance, "onCreate");
            invokeLifecycle(instance, "onUpdate");
        }

        assertThat(exercisedClasses).isGreaterThan(70);
        assertThat(exercisedAccessors).isGreaterThan(200);
    }

    @Test
    void enumsExposeExpectedValues() {
        assertThat(MedicationStatus.valueOf("ACTIVE")).isEqualTo(MedicationStatus.ACTIVE);
        assertThat(MedicationStatus.values()).contains(
                MedicationStatus.ACTIVE,
                MedicationStatus.EXPIRED,
                MedicationStatus.ONGOING,
                MedicationStatus.DISCONTINUED
        );
        assertThat(CareActivityType.valueOf("PHYSICAL_ACTIVITY")).isEqualTo(CareActivityType.PHYSICAL_ACTIVITY);
        assertThat(CareActivityType.values()).contains(CareActivityType.PHYSICAL_ACTIVITY);
    }

    private static Object instantiateNoArg(Class<?> type) throws Exception {
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Method findSetter(Class<?> type, Field field, Object value) {
        String name = "set" + capitalize(field.getName());
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name)
                    && method.getParameterCount() == 1
                    && wraps(method.getParameterTypes()[0]).isAssignableFrom(wraps(value.getClass()))) {
                return method;
            }
        }
        return null;
    }

    private static Method findGetter(Class<?> type, Field field) {
        List<String> names = field.getType() == boolean.class || field.getType() == Boolean.class
                ? List.of("is" + capitalize(field.getName()), "get" + capitalize(field.getName()))
                : List.of("get" + capitalize(field.getName()));
        for (String name : names) {
            try {
                return type.getMethod(name);
            } catch (NoSuchMethodException ignored) {
                // try next getter naming convention
            }
        }
        return null;
    }

    private static void invokeLifecycle(Object instance, String methodName) throws Exception {
        try {
            Method method = instance.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(instance);
        } catch (NoSuchMethodException ignored) {
            // Not every data class is a JPA entity with lifecycle callbacks.
        }
    }

    private static Object sampleValue(Class<?> type, String fieldName) {
        Class<?> wrapped = wraps(type);
        if (wrapped == String.class) return fieldName + "-value";
        if (wrapped == Long.class) return 42L;
        if (wrapped == Integer.class) return 7;
        if (wrapped == Double.class) return 3.5d;
        if (wrapped == Float.class) return 2.5f;
        if (wrapped == Boolean.class) return true;
        if (type == LocalDate.class) return LocalDate.of(2026, 5, 3);
        if (type == LocalDateTime.class) return LocalDateTime.of(2026, 5, 3, 15, 0);
        if (List.class.isAssignableFrom(type)) return new ArrayList<>();
        if (type.isEnum()) return type.getEnumConstants()[0];
        return Unsupported.INSTANCE;
    }

    private static Class<?> wraps(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == char.class) return Character.class;
        return type;
    }

    private static String capitalize(String value) {
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    private enum Unsupported { INSTANCE }
}
