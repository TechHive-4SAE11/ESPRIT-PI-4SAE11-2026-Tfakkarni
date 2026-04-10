package org.techhive.assistantservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.techhive.assistantservice.entity.GeneratedVideo;
import org.techhive.assistantservice.entity.enums.VideoStatus;

import java.util.List;

@Repository
public interface GeneratedVideoRepository extends JpaRepository<GeneratedVideo, Long> {

    List<GeneratedVideo> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    List<GeneratedVideo> findByPatientIdAndStatus(Long patientId, VideoStatus status);

    List<GeneratedVideo> findByStatus(VideoStatus status);

    long countByPatientId(Long patientId);
}
