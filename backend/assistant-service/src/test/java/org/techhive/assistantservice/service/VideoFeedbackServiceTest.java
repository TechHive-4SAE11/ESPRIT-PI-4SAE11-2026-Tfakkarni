package org.techhive.assistantservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.assistantservice.dto.VideoFeedbackRequest;
import org.techhive.assistantservice.entity.VideoFeedback;
import org.techhive.assistantservice.repository.VideoFeedbackRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoFeedbackServiceTest {

    @Mock
    private VideoFeedbackRepository feedbackRepository;

    @InjectMocks
    private VideoFeedbackService videoFeedbackService;

    private VideoFeedback sampleFeedback;

    @BeforeEach
    void setUp() {
        sampleFeedback = VideoFeedback.builder()
                .id(1L)
                .videoId(10L)
                .patientId(5L)
                .rating(4)
                .reaction("HAPPY")
                .comments("Very soothing video")
                .engagedFully(true)
                .build();
    }

    @Test
    void recordFeedback_shouldSaveAndReturn() {
        VideoFeedbackRequest request = VideoFeedbackRequest.builder()
                .patientId(5L)
                .rating(4)
                .reaction("HAPPY")
                .comments("Very soothing video")
                .engagedFully(true)
                .build();

        when(feedbackRepository.save(any(VideoFeedback.class))).thenReturn(sampleFeedback);

        VideoFeedback result = videoFeedbackService.recordFeedback(10L, request);

        assertNotNull(result);
        assertEquals(4, result.getRating());
        assertEquals("HAPPY", result.getReaction());
        verify(feedbackRepository).save(any(VideoFeedback.class));
    }

    @Test
    void getFeedbackByVideoId_shouldReturnList() {
        when(feedbackRepository.findByVideoId(10L)).thenReturn(List.of(sampleFeedback));

        List<VideoFeedback> results = videoFeedbackService.getFeedbackByVideoId(10L);

        assertEquals(1, results.size());
        assertEquals(10L, results.get(0).getVideoId());
    }

    @Test
    void getFeedbackByPatientId_shouldReturnList() {
        when(feedbackRepository.findByPatientId(5L)).thenReturn(List.of(sampleFeedback));

        List<VideoFeedback> results = videoFeedbackService.getFeedbackByPatientId(5L);

        assertEquals(1, results.size());
        assertEquals(5L, results.get(0).getPatientId());
    }
}
