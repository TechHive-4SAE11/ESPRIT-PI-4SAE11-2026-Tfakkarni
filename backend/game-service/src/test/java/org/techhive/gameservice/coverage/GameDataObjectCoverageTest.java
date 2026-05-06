package org.techhive.gameservice.coverage;

import org.junit.jupiter.api.Test;
import org.techhive.gameservice.dto.*;
import org.techhive.gameservice.entity.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GameDataObjectCoverageTest {

    private static final List<Class<?>> DATA_CLASSES = List.of(
            AnswerDTO.class,
            AudioGenerateRequest.class,
            BooleanResponseDTO.class,
            CountResponseDTO.class,
            CreateCustomGameRequest.class,
            CreateCustomGameRequest.GameItemEntry.class,
            CreateMemoryPlaceRequest.class,
            CreateMovieMemoryRequest.class,
            CreatePhotoRequest.class,
            CreateQuestionMemoryRequest.class,
            CustomGameDetailResponse.class,
            CustomGameResponse.class,
            DataPointSummary.class,
            EditCustomGameRequest.class,
            EditCustomGameRequest.GameItemEntry.class,
            EditGameRequest.class,
            EditGameRequest.EditImageEntry.class,
            FeatureGateResponse.class,
            QuestionDTO.class,
            QuizDTO.class,
            ScoreAnalyticsResponse.class,
            ScoreAnalyticsResponse.AttemptPoint.class,
            SubmissionRequestDTO.class,
            SubmissionResponseDTO.class,
            TagRequest.class,
            TagResponse.class,
            UnifiedPlayData.class,
            UnifiedPlayData.UnifiedPlayItem.class,
            UnifiedPlayResult.class,
            UnifiedPlayResult.ItemResult.class,
            UnifiedSubmitRequest.class,
            UnifiedSubmitRequest.AnswerEntry.class,
            UpdateDataPointRequest.class,
            UserResponse.class,
            ValidationRequestDTO.class,
            ValidationResponseDTO.class,
            Answer.class,
            CustomGameAttempt.class,
            DataPointPerformance.class,
            GameAttempt.class,
            MovieGameAttempt.class,
            PersonalQuestionAttempt.class
    );

    @Test
    void noArgDataObjectsExposeReadableWritableProperties() throws Exception {
        int exercisedClasses = 0;
        int exercisedProperties = 0;

        for (Class<?> type : DATA_CLASSES) {
            Object instance = instantiate(type);
            if (instance == null) {
                continue;
            }
            exercisedClasses++;

            for (Field field : allFields(type)) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                    continue;
                }
                Object value = sampleValue(field.getType(), field.getName());
                if (value == Unsupported.INSTANCE) {
                    continue;
                }
                field.setAccessible(true);
                field.set(instance, value);
                assertThat(field.get(instance)).isEqualTo(value);
                invokeAccessorIfPresent(type, instance, field);
                exercisedProperties++;
            }

            assertThat(instance.toString()).contains(type.getSimpleName());
            instance.hashCode();
        }

        assertThat(exercisedClasses).isGreaterThan(35);
        assertThat(exercisedProperties).isGreaterThan(120);
    }

    @Test
    void dataObjectsWithGeneratedEqualityCompareSupportedFields() throws Exception {
        int exercisedClasses = 0;
        int exercisedFieldDifferences = 0;

        for (Class<?> type : DATA_CLASSES) {
            if (!declaresEquals(type)) {
                continue;
            }

            Object left = instantiate(type);
            Object right = instantiate(type);
            if (left == null || right == null) {
                continue;
            }

            List<Field> supportedFields = new ArrayList<>();
            for (Field field : allFields(type)) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                    continue;
                }
                Object value = sampleValue(field.getType(), field.getName());
                if (value == Unsupported.INSTANCE) {
                    continue;
                }
                field.setAccessible(true);
                field.set(left, value);
                field.set(right, value);
                supportedFields.add(field);
            }

            assertThat(left).isEqualTo(left);
            assertThat(left).isNotEqualTo(null);
            assertThat(left).isNotEqualTo("not a " + type.getSimpleName());
            assertThat(left).isEqualTo(right);
            assertThat(left.hashCode()).isEqualTo(right.hashCode());
            exercisedClasses++;

            for (Field field : supportedFields) {
                Object changed = instantiate(type);
                for (Field copied : supportedFields) {
                    copied.set(changed, copied.get(left));
                }
                Object different = alternateSampleValue(field.getType(), field.getName());
                if (different == Unsupported.INSTANCE) {
                    continue;
                }
                field.set(changed, different);
                assertThat(left)
                        .as("%s equality should include %s", type.getSimpleName(), field.getName())
                        .isNotEqualTo(changed);
                exercisedFieldDifferences++;

                if (!field.getType().isPrimitive()) {
                    Object nullChanged = instantiate(type);
                    for (Field copied : supportedFields) {
                        copied.set(nullChanged, copied.get(left));
                    }
                    field.set(nullChanged, null);
                    assertThat(left)
                            .as("%s equality should include null mismatch for %s", type.getSimpleName(), field.getName())
                            .isNotEqualTo(nullChanged);
                    exercisedFieldDifferences++;

                    Object reverseNullChanged = instantiate(type);
                    for (Field copied : supportedFields) {
                        copied.set(reverseNullChanged, copied.get(left));
                    }
                    field.set(left, null);
                    assertThat(left)
                            .as("%s equality should include reverse null mismatch for %s", type.getSimpleName(), field.getName())
                            .isNotEqualTo(reverseNullChanged);
                    field.set(reverseNullChanged, null);
                    assertThat(left)
                            .as("%s equality should accept matching null values for %s", type.getSimpleName(), field.getName())
                            .isEqualTo(reverseNullChanged);
                    field.set(left, field.get(right));
                    exercisedFieldDifferences++;
                }
            }
        }

        assertThat(exercisedClasses).isGreaterThan(25);
        assertThat(exercisedFieldDifferences).isGreaterThan(170);
    }

    private static Object instantiate(Class<?> type) throws Exception {
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException ignored) {
            try {
                Method builderFactory = type.getMethod("builder");
                Object builder = builderFactory.invoke(null);
                return builder.getClass().getMethod("build").invoke(builder);
            } catch (NoSuchMethodException noBuilder) {
                return null;
            }
        }
    }

    private static List<Field> allFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            fields.addAll(List.of(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

    private static void invokeAccessorIfPresent(Class<?> type, Object instance, Field field) throws Exception {
        String suffix = Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
        List<String> getters = new ArrayList<>(List.of("get" + suffix));
        if (field.getType() == boolean.class || field.getType() == Boolean.class) {
            getters.add("is" + suffix);
        }
        for (String getterName : getters) {
            try {
                Method getter = type.getMethod(getterName);
                assertThat(getter.invoke(instance)).isEqualTo(field.get(instance));
                return;
            } catch (NoSuchMethodException ignored) {
                // Direct field assertion above still verifies the object stores the value.
            }
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
        if (type == LocalDateTime.class) return LocalDateTime.of(2026, 5, 3, 12, 0);
        if (List.class.isAssignableFrom(type)) return new ArrayList<>(List.of("value"));
        if (Set.class.isAssignableFrom(type)) return new LinkedHashSet<>(Set.of("value"));
        if (type.isEnum()) return type.getEnumConstants()[0];
        return Unsupported.INSTANCE;
    }

    private static Object alternateSampleValue(Class<?> type, String fieldName) {
        Class<?> wrapped = wraps(type);
        if (wrapped == String.class) return fieldName + "-other";
        if (wrapped == Long.class) return 84L;
        if (wrapped == Integer.class) return 14;
        if (wrapped == Double.class) return 7.0d;
        if (wrapped == Float.class) return 5.0f;
        if (wrapped == Boolean.class) return false;
        if (type == LocalDateTime.class) return LocalDateTime.of(2026, 5, 4, 13, 0);
        if (List.class.isAssignableFrom(type)) return new ArrayList<>(List.of("other"));
        if (Set.class.isAssignableFrom(type)) return new LinkedHashSet<>(Set.of("other"));
        if (type.isEnum()) {
            Object[] values = type.getEnumConstants();
            return values.length > 1 ? values[1] : values[0];
        }
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

    private static boolean declaresEquals(Class<?> type) {
        try {
            return type.getDeclaredMethod("equals", Object.class).getDeclaringClass() != Object.class;
        } catch (NoSuchMethodException ignored) {
            return false;
        }
    }

    private enum Unsupported { INSTANCE }
}
