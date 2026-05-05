package org.techhive.assistantservice.dto;

import org.junit.jupiter.api.Test;
import org.techhive.assistantservice.client.dto.AnswerDTO;
import org.techhive.assistantservice.client.dto.EquipmentLoanDTO;
import org.techhive.assistantservice.client.dto.QuestionDTO;
import org.techhive.assistantservice.entity.GeneratedVideo;
import org.techhive.assistantservice.entity.VideoFeedback;
import org.techhive.assistantservice.entity.enums.MemoryType;
import org.techhive.assistantservice.entity.enums.VideoStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssistantDataObjectsTest {

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
}
