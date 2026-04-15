package org.techhive.trackingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.techhive.trackingservice.dto.*;
import org.techhive.trackingservice.entity.DoctorRating;
import org.techhive.trackingservice.entity.MedicalMeeting;
import org.techhive.trackingservice.repository.DoctorRatingRepository;
import org.techhive.trackingservice.repository.MedicalMeetingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorRatingService {

    private final DoctorRatingRepository ratingRepository;
    private final MedicalMeetingRepository meetingRepository;

    // ── Submit a rating ──────────────────────────────────────────────────────

    @Transactional
    public DoctorRatingResponse submitRating(CreateRatingRequest req) {

        // Validate rating value
        if (req.getRating() == null || req.getRating() < 1 || req.getRating() > 5) {
            throw new IllegalArgumentException("La note doit être entre 1 et 5 étoiles.");
        }

        // Review mandatory when rating <= 3
        if (req.getRating() <= 3 && (req.getReview() == null || req.getReview().trim().isEmpty())) {
            throw new IllegalArgumentException("Un avis est obligatoire pour une note ≤ 3 étoiles.");
        }

        // Prevent double rating for same meeting
        if (ratingRepository.existsByMeetingIdAndPatientKeycloakId(
                req.getMeetingId(), req.getPatientKeycloakId())) {
            throw new IllegalStateException("Vous avez déjà évalué cette réunion.");
        }

        // Fetch names
        MedicalMeeting meeting = meetingRepository.findById(req.getMeetingId()).orElse(null);
        String doctorName  = meeting != null ? meeting.getDoctorName()  : "Médecin";
        String patientName = meeting != null ? meeting.getPatientName() : "Patient";

        DoctorRating rating = DoctorRating.builder()
                .meetingId(req.getMeetingId())
                .doctorKeycloakId(req.getDoctorKeycloakId())
                .patientKeycloakId(req.getPatientKeycloakId())
                .rating(req.getRating())
                .review(req.getReview() != null ? req.getReview().trim() : null)
                .doctorName(doctorName)
                .patientName(patientName)
                .build();

        rating = ratingRepository.save(rating);
        log.info("Rating submitted: meeting={} doctor={} stars={}", req.getMeetingId(),
                req.getDoctorKeycloakId(), req.getRating());

        return toResponse(rating);
    }

    // ── Get rankings (for admin podium) ─────────────────────────────────────

    public List<DoctorRankingResponse> getDoctorRanking() {
        List<Object[]> rows = ratingRepository.findDoctorAverageRatings();
        List<DoctorRankingResponse> result = new ArrayList<>();
        int rank = 1;
        for (Object[] row : rows) {
            String keycloakId = (String) row[0];
            String name       = (String) row[1];
            Double avg        = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            Long   count      = row[3] != null ? ((Number) row[3]).longValue() : 0L;

            // Fetch last 5 reviews for this doctor
            List<DoctorRatingResponse> recent = ratingRepository
                    .findByDoctorKeycloakIdOrderByCreatedAtDesc(keycloakId)
                    .stream().limit(5).map(this::toResponse).collect(Collectors.toList());

            result.add(DoctorRankingResponse.builder()
                    .doctorKeycloakId(keycloakId)
                    .doctorName(name != null ? name : keycloakId)
                    .averageRating(Math.round(avg * 10.0) / 10.0)
                    .totalRatings(count)
                    .rank(rank++)
                    .recentReviews(recent)
                    .build());
        }
        return result;
    }

    // ── Get ratings for a specific doctor ───────────────────────────────────

    public List<DoctorRatingResponse> getRatingsForDoctor(String doctorKeycloakId) {
        return ratingRepository
                .findByDoctorKeycloakIdOrderByCreatedAtDesc(doctorKeycloakId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── Check if already rated ───────────────────────────────────────────────

    public boolean hasRated(Long meetingId, String patientKeycloakId) {
        return ratingRepository.existsByMeetingIdAndPatientKeycloakId(meetingId, patientKeycloakId);
    }

    // ── Mapper ───────────────────────────────────────────────────────────────

    private DoctorRatingResponse toResponse(DoctorRating r) {
        return DoctorRatingResponse.builder()
                .id(r.getId())
                .meetingId(r.getMeetingId())
                .doctorKeycloakId(r.getDoctorKeycloakId())
                .patientKeycloakId(r.getPatientKeycloakId())
                .rating(r.getRating())
                .review(r.getReview())
                .doctorName(r.getDoctorName())
                .patientName(r.getPatientName())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
