package org.techhive.assistantservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.techhive.assistantservice.entity.VideoFeedback;

import java.util.List;

@Repository
public interface VideoFeedbackRepository extends JpaRepository<VideoFeedback, Long> {

    List<VideoFeedback> findByVideoId(Long videoId);

    List<VideoFeedback> findByPatientId(Long patientId);

    Double findAverageRatingByVideoId(Long videoId);
}
