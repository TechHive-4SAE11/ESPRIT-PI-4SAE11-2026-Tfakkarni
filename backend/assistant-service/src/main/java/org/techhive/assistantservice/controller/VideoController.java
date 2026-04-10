package org.techhive.assistantservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.assistantservice.dto.VideoFeedbackRequest;
import org.techhive.assistantservice.dto.VideoGenerateRequest;
import org.techhive.assistantservice.dto.VideoGenerateResponse;
import org.techhive.assistantservice.entity.VideoFeedback;
import org.techhive.assistantservice.service.VideoApiIntegrationService;
import org.techhive.assistantservice.service.VideoFeedbackService;
import org.techhive.assistantservice.service.VideoScriptService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai/video")
@RequiredArgsConstructor
public class VideoController {

    private final VideoScriptService videoScriptService;
    private final VideoApiIntegrationService videoApiIntegrationService;
    private final VideoFeedbackService videoFeedbackService;

    /**
     * POST /api/ai/video/generate
     * Generate a personalized memory video script and storyboard.
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateVideo(@Valid @RequestBody VideoGenerateRequest request) {
        log.info("Video generation request: patient={}, topic={}, type={}, duration={}",
                request.getPatientId(), request.getTopic(), request.getMemoryType(), request.getDuration());

        try {
            VideoGenerateResponse response = videoScriptService.generateVideoScript(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Video generation failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Video generation failed",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * GET /api/ai/video/patient/{patientId}
     * Get all videos generated for a patient.
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<VideoGenerateResponse>> getVideosByPatient(@PathVariable Long patientId) {
        log.info("Fetching videos for patient: {}", patientId);

        List<VideoGenerateResponse> videos = videoScriptService.getVideosByPatient(patientId);
        return ResponseEntity.ok(videos);
    }

    /**
     * GET /api/ai/video/{videoId}/watch
     * Get the video details and URL for playback.
     */
    @GetMapping("/{videoId}/watch")
    public ResponseEntity<?> watchVideo(@PathVariable Long videoId) {
        log.info("Watch video request: {}", videoId);

        try {
            VideoGenerateResponse video = videoScriptService.getVideoById(videoId);
            return ResponseEntity.ok(video);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/ai/video/{videoId}/render
     * Trigger external API rendering for a video script.
     */
    @PostMapping("/{videoId}/render")
    public ResponseEntity<?> renderVideo(@PathVariable Long videoId) {
        log.info("Render video request: {}", videoId);

        try {
            VideoGenerateResponse video = videoScriptService.getVideoById(videoId);
            String videoUrl = videoApiIntegrationService.generateVideoFromScript(videoId, video.getScript());

            return ResponseEntity.ok(Map.of(
                    "videoId", videoId,
                    "videoUrl", videoUrl != null ? videoUrl : "Script-only mode: no video URL generated",
                    "status", videoUrl != null ? "RENDERING" : "SCRIPT_ONLY"
            ));
        } catch (Exception e) {
            log.error("Video rendering failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Video rendering failed", "message", e.getMessage()));
        }
    }

    /**
     * POST /api/ai/video/{videoId}/feedback
     * Record patient feedback for a video.
     */
    @PostMapping("/{videoId}/feedback")
    public ResponseEntity<?> recordFeedback(
            @PathVariable Long videoId,
            @Valid @RequestBody VideoFeedbackRequest request) {
        log.info("Recording feedback for video {} from patient {}", videoId, request.getPatientId());

        try {
            VideoFeedback feedback = videoFeedbackService.recordFeedback(videoId, request);
            return new ResponseEntity<>(feedback, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to record feedback", "message", e.getMessage()));
        }
    }

    /**
     * GET /api/ai/video/{videoId}/feedback
     * Get all feedback for a video.
     */
    @GetMapping("/{videoId}/feedback")
    public ResponseEntity<List<VideoFeedback>> getVideoFeedback(@PathVariable Long videoId) {
        return ResponseEntity.ok(videoFeedbackService.getFeedbackByVideoId(videoId));
    }
}
