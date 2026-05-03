package org.techhive.trackingservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.techhive.trackingservice.controller.DoctorRatingController;
import org.techhive.trackingservice.dto.CreateRatingRequest;
import org.techhive.trackingservice.dto.DoctorRankingResponse;
import org.techhive.trackingservice.dto.DoctorRatingResponse;
import org.techhive.trackingservice.entity.DoctorRating;
import org.techhive.trackingservice.entity.MedicalMeeting;
import org.techhive.trackingservice.repository.DoctorRatingRepository;
import org.techhive.trackingservice.repository.MedicalMeetingRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorRatingServiceCoverageTest {

    @Mock DoctorRatingRepository ratingRepository;
    @Mock MedicalMeetingRepository meetingRepository;

    private DoctorRatingService ratingService;

    @BeforeEach
    void setUp() {
        ratingService = new DoctorRatingService(ratingRepository, meetingRepository);
    }

    @Test
    void serviceSubmitsRatingsRanksDoctorsAndChecksExistingRatings() {
        CreateRatingRequest request = new CreateRatingRequest(7L, "doctor-kc", "patient-kc", 5, "  Excellent suivi  ");
        MedicalMeeting meeting = MedicalMeeting.builder()
                .id(7L)
                .doctorName("Dr Sarra Mansouri")
                .patientName("Nour Trabelsi")
                .build();
        when(ratingRepository.existsByMeetingIdAndPatientKeycloakId(7L, "patient-kc")).thenReturn(false);
        when(meetingRepository.findById(7L)).thenReturn(Optional.of(meeting));
        when(ratingRepository.save(any(DoctorRating.class))).thenAnswer(inv -> {
            DoctorRating rating = inv.getArgument(0);
            rating.setId(70L);
            rating.setCreatedAt(LocalDateTime.of(2026, 5, 3, 15, 32));
            return rating;
        });

        DoctorRatingResponse submitted = ratingService.submitRating(request);

        assertThat(submitted.getId()).isEqualTo(70L);
        assertThat(submitted.getDoctorName()).isEqualTo("Dr Sarra Mansouri");
        assertThat(submitted.getPatientName()).isEqualTo("Nour Trabelsi");
        assertThat(submitted.getReview()).isEqualTo("Excellent suivi");

        DoctorRating recentOne = rating(1L, "doctor-a", "Dr A", 5, "Bravo");
        DoctorRating recentTwo = rating(2L, "doctor-a", "Dr A", 4, "Bien");
        when(ratingRepository.findDoctorAverageRatings()).thenReturn(List.of(
                new Object[]{"doctor-a", "Dr A", 4.56d, 9L},
                new Object[]{"doctor-b", null, null, null}
        ));
        when(ratingRepository.findByDoctorKeycloakIdOrderByCreatedAtDesc("doctor-a"))
                .thenReturn(List.of(recentOne, recentTwo));
        when(ratingRepository.findByDoctorKeycloakIdOrderByCreatedAtDesc("doctor-b"))
                .thenReturn(List.of());

        List<DoctorRankingResponse> ranking = ratingService.getDoctorRanking();

        assertThat(ranking).hasSize(2);
        assertThat(ranking.get(0).getDoctorName()).isEqualTo("Dr A");
        assertThat(ranking.get(0).getAverageRating()).isEqualTo(4.6d);
        assertThat(ranking.get(0).getTotalRatings()).isEqualTo(9L);
        assertThat(ranking.get(0).getRank()).isEqualTo(1);
        assertThat(ranking.get(0).getRecentReviews()).extracting(DoctorRatingResponse::getReview).containsExactly("Bravo", "Bien");
        assertThat(ranking.get(1).getDoctorName()).isEqualTo("doctor-b");
        assertThat(ranking.get(1).getAverageRating()).isEqualTo(0.0d);
        assertThat(ranking.get(1).getTotalRatings()).isEqualTo(0L);

        when(ratingRepository.findByDoctorKeycloakIdOrderByCreatedAtDesc("doctor-a"))
                .thenReturn(List.of(recentOne));
        assertThat(ratingService.getRatingsForDoctor("doctor-a")).extracting(DoctorRatingResponse::getId).containsExactly(1L);
        when(ratingRepository.existsByMeetingIdAndPatientKeycloakId(8L, "patient-kc")).thenReturn(true);
        assertThat(ratingService.hasRated(8L, "patient-kc")).isTrue();
    }

    @Test
    void serviceRejectsInvalidLowDuplicateAndFallbackNameCases() {
        assertThatThrownBy(() -> ratingService.submitRating(new CreateRatingRequest(1L, "doctor", "patient", null, "ok")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entre 1 et 5");
        assertThatThrownBy(() -> ratingService.submitRating(new CreateRatingRequest(1L, "doctor", "patient", 0, "ok")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entre 1 et 5");
        assertThatThrownBy(() -> ratingService.submitRating(new CreateRatingRequest(1L, "doctor", "patient", 6, "ok")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entre 1 et 5");
        assertThatThrownBy(() -> ratingService.submitRating(new CreateRatingRequest(1L, "doctor", "patient", 3, "  ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obligatoire");

        when(ratingRepository.existsByMeetingIdAndPatientKeycloakId(2L, "patient")).thenReturn(true);
        assertThatThrownBy(() -> ratingService.submitRating(new CreateRatingRequest(2L, "doctor", "patient", 4, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("déjà évalué");

        when(ratingRepository.existsByMeetingIdAndPatientKeycloakId(3L, "patient")).thenReturn(false);
        when(meetingRepository.findById(3L)).thenReturn(Optional.empty());
        when(ratingRepository.save(any(DoctorRating.class))).thenAnswer(inv -> {
            DoctorRating rating = inv.getArgument(0);
            rating.setId(3L);
            return rating;
        });
        DoctorRatingResponse fallback = ratingService.submitRating(new CreateRatingRequest(3L, "doctor", "patient", 4, null));
        assertThat(fallback.getDoctorName()).isEqualTo("Médecin");
        assertThat(fallback.getPatientName()).isEqualTo("Patient");
    }

    @Test
    @SuppressWarnings("unchecked")
    void controllerMapsSuccessValidationConflictAndFallbackResponses() {
        DoctorRatingService mockService = mock(DoctorRatingService.class);
        DoctorRatingController controller = new DoctorRatingController(mockService);
        CreateRatingRequest request = new CreateRatingRequest(10L, "doctor", "patient", 5, "merci");
        DoctorRatingResponse response = DoctorRatingResponse.builder().id(10L).rating(5).build();

        when(mockService.submitRating(request)).thenReturn(response);
        ResponseEntity<?> created = controller.submitRating(request);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isSameAs(response);

        doThrow(new IllegalArgumentException("bad rating")).when(mockService).submitRating(request);
        ResponseEntity<?> bad = controller.submitRating(request);
        assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((Map<String, String>) bad.getBody()).containsEntry("error", "bad rating");

        doThrow(new IllegalStateException("duplicate")).when(mockService).submitRating(request);
        ResponseEntity<?> conflict = controller.submitRating(request);
        assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat((Map<String, String>) conflict.getBody()).containsEntry("error", "duplicate");

        doThrow(new RuntimeException("db down")).when(mockService).submitRating(request);
        ResponseEntity<?> error = controller.submitRating(request);
        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat((Map<String, String>) error.getBody()).containsEntry("error", "Erreur lors de l'enregistrement de l'évaluation.");
        assertThat((Map<String, String>) error.getBody()).containsEntry("cause", "RuntimeException: db down");

        DoctorRankingResponse ranking = DoctorRankingResponse.builder().doctorKeycloakId("doctor").rank(1).build();
        when(mockService.getDoctorRanking()).thenReturn(List.of(ranking));
        assertThat(controller.getRanking().getBody()).containsExactly(ranking);
        when(mockService.getDoctorRanking()).thenThrow(new RuntimeException("ranking down"));
        assertThat(controller.getRanking().getBody()).isEmpty();

        when(mockService.getRatingsForDoctor("doctor")).thenReturn(List.of(response));
        assertThat(controller.getRatingsForDoctor("doctor").getBody()).containsExactly(response);
        when(mockService.getRatingsForDoctor("doctor")).thenThrow(new RuntimeException("ratings down"));
        assertThat(controller.getRatingsForDoctor("doctor").getBody()).isEmpty();

        when(mockService.hasRated(10L, "patient")).thenReturn(true);
        assertThat(controller.checkRated(10L, "patient").getBody()).containsEntry("rated", true);
        when(mockService.hasRated(11L, "patient")).thenThrow(new RuntimeException("check down"));
        assertThat(controller.checkRated(11L, "patient").getBody()).containsEntry("rated", false);
    }

    private DoctorRating rating(Long id, String doctorId, String doctorName, Integer stars, String review) {
        return DoctorRating.builder()
                .id(id)
                .meetingId(id + 100)
                .doctorKeycloakId(doctorId)
                .patientKeycloakId("patient-" + id)
                .rating(stars)
                .review(review)
                .doctorName(doctorName)
                .patientName("Patient " + id)
                .createdAt(LocalDateTime.of(2026, 5, 3, 15, 30).minusMinutes(id))
                .build();
    }
}
