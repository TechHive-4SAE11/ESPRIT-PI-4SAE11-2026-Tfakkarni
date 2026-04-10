package org.techhive.assistantservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.techhive.assistantservice.dto.VideoFeedbackRequest;
import org.techhive.assistantservice.entity.VideoFeedback;
import org.techhive.assistantservice.repository.VideoFeedbackRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoFeedbackService {

    private final VideoFeedbackRepository feedbackRepository;

    /**
     * Record patient feedback for a video.
     */
    public VideoFeedback recordFeedback(Long videoId, VideoFeedbackRequest request) {
        log.info("Recording feedback for video {} from patient {}", videoId, request.getPatientId());

        VideoFeedback feedback = VideoFeedback.builder()
                .videoId(videoId)
                .patientId(request.getPatientId())
                .rating(request.getRating())
                .reaction(request.getReaction())
                .comments(request.getComments())
                .engagedFully(request.getEngagedFully())
                .build();

        return feedbackRepository.save(feedback);
    }

    /**
     * Get all feedback for a video.
     */
    public List<VideoFeedback> getFeedbackByVideoId(Long videoId) {
        return feedbackRepository.findByVideoId(videoId);
    }

    /**
     * Get all feedback from a patient.
     */
    public List<VideoFeedback> getFeedbackByPatientId(Long patientId) {
        return feedbackRepository.findByPatientId(patientId);
    }
}
