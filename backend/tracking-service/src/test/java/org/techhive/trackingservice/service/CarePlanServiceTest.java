package org.techhive.trackingservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.techhive.trackingservice.entity.CareActivity;
import org.techhive.trackingservice.entity.CarePlan;
import org.techhive.trackingservice.entity.Session;
import org.techhive.trackingservice.repository.CareActivityRepository;
import org.techhive.trackingservice.repository.CarePlanRepository;
import org.techhive.trackingservice.repository.SessionRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarePlanServiceTest {

    @Mock
    private CarePlanRepository carePlanRepository;
    @Mock
    private CareActivityRepository careActivityRepository;
    @Mock
    private SessionRepository sessionRepository;

    @InjectMocks
    private CarePlanService carePlanService;

    private CarePlan carePlan;
    private CareActivity careActivity;
    private Session session;

    @BeforeEach
    void setUp() {
        carePlan = new CarePlan();
        carePlan.setId(1L);
        carePlan.setCareActivities(new ArrayList<>());

        careActivity = new CareActivity();
        careActivity.setId(10L);
        careActivity.setActivityName("Daily Walk");
        carePlan.getCareActivities().add(careActivity);

        session = new Session();
        session.setId(100L);
    }

    @Test
    void createCarePlan_ShouldSaveWithBidirectionalLink() {
        when(carePlanRepository.save(any(CarePlan.class))).thenReturn(carePlan);

        CarePlan created = carePlanService.createCarePlan(carePlan);

        assertThat(created).isNotNull();
        assertThat(careActivity.getCarePlan()).isEqualTo(carePlan);
        verify(carePlanRepository).save(carePlan);
    }

    @Test
    void createCarePlanForSession_ShouldLinkSessionAndSave() {
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));
        when(carePlanRepository.save(any(CarePlan.class))).thenReturn(carePlan);

        CarePlan created = carePlanService.createCarePlanForSession(100L, carePlan);

        assertThat(created).isNotNull();
        assertThat(carePlan.getSession()).isEqualTo(session);
        verify(carePlanRepository).save(carePlan);
    }

    @Test
    void getCarePlanById_ShouldReturnOptionalPlan() {
        when(carePlanRepository.findById(1L)).thenReturn(Optional.of(carePlan));

        Optional<CarePlan> result = carePlanService.getCarePlanById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    void updateCarePlan_ShouldClearAndReplaceActivities() {
        CarePlan updatedData = new CarePlan();
        updatedData.setCareActivities(new ArrayList<>());
        CareActivity newActivity = new CareActivity();
        newActivity.setActivityName("New Activity");
        updatedData.getCareActivities().add(newActivity);

        when(carePlanRepository.findById(1L)).thenReturn(Optional.of(carePlan));
        when(carePlanRepository.save(any(CarePlan.class))).thenReturn(carePlan);

        CarePlan result = carePlanService.updateCarePlan(1L, updatedData);

        assertThat(result).isNotNull();
        assertThat(carePlan.getCareActivities()).hasSize(1);
        assertThat(carePlan.getCareActivities().get(0).getActivityName()).isEqualTo("New Activity");
        verify(carePlanRepository).save(carePlan);
    }

    @Test
    void updateActivityStatus_ShouldUpdateStatus() {
        when(careActivityRepository.findById(10L)).thenReturn(Optional.of(careActivity));
        when(careActivityRepository.save(any(CareActivity.class))).thenReturn(careActivity);

        CareActivity updated = carePlanService.updateActivityStatus(10L, "COMPLETED");

        assertThat(updated).isNotNull();
        assertThat(updated.getCompletionStatus()).isEqualTo("COMPLETED");
        verify(careActivityRepository).save(careActivity);
    }

    @Test
    void getCarePlansByPatientPaginated_ShouldReturnPage() {
        Page<CarePlan> page = new PageImpl<>(Collections.singletonList(carePlan));
        when(carePlanRepository.findBySessionMedicalFolderIdPatient(eq("patient-id"), any())).thenReturn(page);

        Page<CarePlan> result = carePlanService.getCarePlansByPatientPaginated("patient-id", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        verify(carePlanRepository).findBySessionMedicalFolderIdPatient(eq("patient-id"), any());
    }
}
