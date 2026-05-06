package org.techhive.assistantservice.dto;

import org.junit.jupiter.api.Test;
import org.techhive.assistantservice.client.dto.AnswerDTO;
import org.techhive.assistantservice.client.dto.EquipmentDTO;
import org.techhive.assistantservice.client.dto.EquipmentLoanDTO;
import org.techhive.assistantservice.client.dto.QuestionDTO;
import org.techhive.assistantservice.client.dto.QuizDTO;
import org.techhive.assistantservice.entity.GeneratedVideo;
import org.techhive.assistantservice.entity.VideoFeedback;
import org.techhive.assistantservice.entity.enums.MemoryType;
import org.techhive.assistantservice.entity.enums.VideoStatus;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AssistantDataObjectsTest {

    @Test
    void lombokDataObjects_shouldHonorAccessorAndEqualityContracts() {
        List<Class<?>> classes = List.of(
                AIReportDTO.class,
                EquipmentRecommendRequest.class,
                EquipmentRecommendResponse.class,
                EquipmentRecommendation.class,
                MedicalFolderDTO.class,
                PatientDTO.class,
                QuizGenerateRequest.class,
                ReportAnalysisResult.class,
                ReportBasedQuizByNameRequest.class,
                ReportBasedQuizRequest.class,
                VideoFeedbackRequest.class,
                VideoGenerateRequest.class,
                VideoGenerateResponse.class,
                VideoGenerateResponse.StoryboardScene.class,
                VoiceCommandRequest.class,
                VoiceCommandResponse.class,
                AnswerDTO.class,
                EquipmentDTO.class,
                EquipmentLoanDTO.class,
                QuestionDTO.class,
                QuizDTO.class,
                GeneratedVideo.class,
                VideoFeedback.class
        );

        for (Class<?> type : classes) {
            assertDataObjectContract(type);
        }
    }

    @Test
    void equipmentLoanDto_shouldExposeBuilderAndAccessors() {
        LocalDateTime loanDate = LocalDateTime.of(2026, 5, 3, 10, 15);
        LocalDateTime dueDate = loanDate.plusDays(7);
        LocalDateTime returnDate = loanDate.plusDays(2);

        EquipmentLoanDTO loan = EquipmentLoanDTO.builder()
                .id(1L)
                .equipmentId(2L)
                .equipmentName("Walker")
                .borrowerId(3L)
                .loanDate(loanDate)
                .dueDate(dueDate)
                .returnDate(returnDate)
                .purpose("home mobility")
                .notes("use with supervision")
                .status("RETURNED")
                .build();

        assertEquals(1L, loan.getId());
        assertEquals(2L, loan.getEquipmentId());
        assertEquals("Walker", loan.getEquipmentName());
        assertEquals(3L, loan.getBorrowerId());
        assertEquals(loanDate, loan.getLoanDate());
        assertEquals(dueDate, loan.getDueDate());
        assertEquals(returnDate, loan.getReturnDate());
        assertEquals("home mobility", loan.getPurpose());
        assertEquals("use with supervision", loan.getNotes());
        assertEquals("RETURNED", loan.getStatus());
        assertTrue(loan.toString().contains("Walker"));
    }

    @Test
    void answerAndQuestionDto_shouldExposeBuilderAndAccessors() {
        AnswerDTO answer = AnswerDTO.builder()
                .id(4L)
                .text("La cuisine")
                .isCorrect(true)
                .explanation("Lieu familier")
                .questionId(5L)
                .build();
        QuestionDTO question = QuestionDTO.builder()
                .id(5L)
                .text("Où prépare-t-on le couscous ?")
                .difficultyLevel(2)
                .mediaAttachment("kitchen.jpg")
                .quizId(6L)
                .answers(List.of(answer))
                .build();

        assertEquals("La cuisine", answer.getText());
        assertEquals(true, answer.getIsCorrect());
        assertEquals("Lieu familier", answer.getExplanation());
        assertEquals(5L, answer.getQuestionId());
        assertEquals("Où prépare-t-on le couscous ?", question.getText());
        assertEquals(2, question.getDifficultyLevel());
        assertEquals("kitchen.jpg", question.getMediaAttachment());
        assertEquals(6L, question.getQuizId());
        assertEquals(List.of(answer), question.getAnswers());
    }

    @Test
    void generatedVideoLifecycleCallbacks_shouldSetTimestamps() {
        GeneratedVideo video = GeneratedVideo.builder()
                .id(8L)
                .patientId(9L)
                .topic("Sfax childhood")
                .memoryType(MemoryType.PHOTO)
                .duration(45)
                .status(VideoStatus.GENERATING)
                .videoUrl("https://example.test/video.mp4")
                .thumbnailUrl("https://example.test/thumb.jpg")
                .script("Opening scene")
                .storyboardJson("[]")
                .patientName("Hela Ben Salem")
                .patientAge(76)
                .interests("family photos")
                .build();

        video.setCreatedAt(null);
        video.setUpdatedAt(null);
        invoke(video, "onCreate");
        invoke(video, "onUpdate");

        assertEquals(9L, video.getPatientId());
        assertEquals("Sfax childhood", video.getTopic());
        assertEquals(MemoryType.PHOTO, video.getMemoryType());
        assertEquals(45, video.getDuration());
        assertEquals(VideoStatus.GENERATING, video.getStatus());
        assertEquals("https://example.test/video.mp4", video.getVideoUrl());
        assertEquals("https://example.test/thumb.jpg", video.getThumbnailUrl());
        assertEquals("Opening scene", video.getScript());
        assertEquals("[]", video.getStoryboardJson());
        assertEquals("Hela Ben Salem", video.getPatientName());
        assertEquals(76, video.getPatientAge());
        assertEquals("family photos", video.getInterests());
        assertNotNull(video.getCreatedAt());
        assertNotNull(video.getUpdatedAt());
    }

    @Test
    void videoFeedbackLifecycleCallback_shouldSetCreatedAt() {
        VideoFeedback feedback = VideoFeedback.builder()
                .id(1L)
                .videoId(8L)
                .patientId(9L)
                .rating(5)
                .reaction("POSITIVE")
                .comments("Smiled during the full clip")
                .engagedFully(true)
                .build();

        invoke(feedback, "onCreate");

        assertEquals(8L, feedback.getVideoId());
        assertEquals(9L, feedback.getPatientId());
        assertEquals(5, feedback.getRating());
        assertEquals("POSITIVE", feedback.getReaction());
        assertEquals("Smiled during the full clip", feedback.getComments());
        assertEquals(true, feedback.getEngagedFully());
        assertNotNull(feedback.getCreatedAt());
    }

    private void invoke(Object target, String methodName) {
        try {
            var method = target.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(target);
        } catch (Exception e) {
            throw new AssertionError("Could not invoke " + methodName, e);
        }
    }

    private void assertDataObjectContract(Class<?> type) {
        Object populated = newInstance(type);
        Object sameValues = newInstance(type);

        List<Field> fields = fieldsOf(type);
        for (Field field : fields) {
            Object value = sampleValue(field.getType(), 0);
            setProperty(populated, field, value);
            setProperty(sameValues, field, value);
            assertEquals(value, getProperty(populated, field), type.getSimpleName() + "." + field.getName());
        }

        assertEquals(populated, populated);
        assertNotEquals(populated, null);
        assertNotEquals(populated, "different type");
        assertEquals(populated, sameValues, type.getSimpleName() + " should compare equal for same field values");
        assertEquals(populated.hashCode(), sameValues.hashCode());
        assertTrue(populated.toString().contains(type.getSimpleName()));

        Object defaultValues = newInstance(type);
        Object sameDefaults = newInstance(type);
        assertEquals(defaultValues, sameDefaults);
        assertEquals(defaultValues.hashCode(), sameDefaults.hashCode());

        for (Field field : fields) {
            Object changed = newInstance(type);
            for (Field copied : fields) {
                setProperty(changed, copied, getProperty(populated, copied));
            }
            setProperty(changed, field, sampleValue(field.getType(), 1));
            assertNotEquals(populated, changed, type.getSimpleName() + " should include " + field.getName() + " in equals");

            if (!field.getType().isPrimitive()) {
                Object oneNull = newInstance(type);
                Object oneValue = newInstance(type);
                setProperty(oneValue, field, sampleValue(field.getType(), 0));
                assertNotEquals(oneNull, oneValue, type.getSimpleName() + " should distinguish null " + field.getName());
            }
        }
    }

    private List<Field> fieldsOf(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .toList();
    }

    private Object newInstance(Class<?> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new AssertionError("Could not instantiate " + type.getName(), e);
        }
    }

    private void setProperty(Object target, Field field, Object value) {
        try {
            Method setter = target.getClass().getMethod("set" + capitalize(field.getName()), field.getType());
            setter.invoke(target, value);
        } catch (Exception e) {
            throw new AssertionError("Could not set " + target.getClass().getSimpleName() + "." + field.getName(), e);
        }
    }

    private Object getProperty(Object target, Field field) {
        try {
            Method getter = target.getClass().getMethod("get" + capitalize(field.getName()));
            return getter.invoke(target);
        } catch (Exception e) {
            throw new AssertionError("Could not get " + target.getClass().getSimpleName() + "." + field.getName(), e);
        }
    }

    private String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private Object sampleValue(Class<?> type, int variant) {
        if (type == String.class) {
            return variant == 0 ? "primary-value" : "alternate-value";
        }
        if (type == Long.class || type == long.class) {
            return variant == 0 ? 101L : 202L;
        }
        if (type == Integer.class || type == int.class) {
            return variant == 0 ? 11 : 22;
        }
        if (type == Double.class || type == double.class) {
            return variant == 0 ? 0.75 : 0.25;
        }
        if (type == Boolean.class || type == boolean.class) {
            return variant == 0;
        }
        if (type == LocalDateTime.class) {
            return LocalDateTime.of(2026, 5, variant == 0 ? 5 : 6, 9, 30);
        }
        if (List.class.isAssignableFrom(type)) {
            return variant == 0 ? List.of("primary") : List.of("alternate");
        }
        if (type == Object.class) {
            return variant == 0 ? "payload" : 42;
        }
        if (type.isEnum()) {
            Object[] constants = type.getEnumConstants();
            return constants[Math.min(variant, constants.length - 1)];
        }
        throw new AssertionError("Unsupported sample type " + type.getName());
    }
}
