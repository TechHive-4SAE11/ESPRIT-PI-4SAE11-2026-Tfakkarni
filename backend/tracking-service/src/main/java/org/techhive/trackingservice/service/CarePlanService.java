package org.techhive.trackingservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.trackingservice.entity.CareActivity;
import org.techhive.trackingservice.entity.CarePlan;
import org.techhive.trackingservice.repository.CareActivityRepository;
import org.techhive.trackingservice.repository.CarePlanRepository;
import org.techhive.trackingservice.repository.SessionRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CarePlanService {

    private final CarePlanRepository carePlanRepository;
    private final CareActivityRepository careActivityRepository;
    private final SessionRepository sessionRepository;

    public CarePlan createCarePlan(CarePlan carePlan) {
        if (carePlan.getCareActivities() != null) {
            for (CareActivity activity : carePlan.getCareActivities()) {
                activity.setCarePlan(carePlan);
            }
        }
        return carePlanRepository.save(carePlan);
    }

    public CarePlan createCarePlanForSession(Long sessionId, CarePlan carePlan) {
        return sessionRepository.findById(sessionId)
                .map(session -> {
                    carePlan.setSession(session);
                    if (carePlan.getCareActivities() != null) {
                        for (CareActivity activity : carePlan.getCareActivities()) {
                            activity.setCarePlan(carePlan);
                        }
                    }
                    return carePlanRepository.save(carePlan);
                })
                .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));
    }

    @Transactional(readOnly = true)
    public List<CarePlan> getAllCarePlans() {
        List<CarePlan> plans = carePlanRepository.findAll();
        plans.forEach(p -> {
            if (p.getCareActivities() != null) p.getCareActivities().size();
        });
        return plans;
    }

    @Transactional(readOnly = true)
    public Optional<CarePlan> getCarePlanById(Long id) {
        Optional<CarePlan> plan = carePlanRepository.findById(id);
        plan.ifPresent(p -> {
            if (p.getCareActivities() != null) p.getCareActivities().size();
        });
        return plan;
    }

    @Transactional(readOnly = true)
    public List<CarePlan> getCarePlansBySession(Long sessionId) {
        List<CarePlan> plans = carePlanRepository.findBySessionId(sessionId);
        plans.forEach(p -> {
            if (p.getCareActivities() != null) p.getCareActivities().size();
        });
        return plans;
    }

    @Transactional(readOnly = true)
    public List<CarePlan> getCarePlansByPatient(String idPatient) {
        List<CarePlan> plans = carePlanRepository.findBySessionMedicalFolderIdPatient(idPatient);
        plans.forEach(p -> {
            if (p.getCareActivities() != null) p.getCareActivities().size();
        });
        return plans;
    }
    
    @Transactional(readOnly = true)
    public Page<CarePlan> getCarePlansByPatientPaginated(String idPatient, Pageable pageable) {
        Page<CarePlan> plans = carePlanRepository.findBySessionMedicalFolderIdPatient(idPatient, pageable);
        plans.forEach(p -> {
            if (p.getCareActivities() != null) p.getCareActivities().size();
        });
        return plans;
    }

    public CarePlan updateCarePlan(Long id, CarePlan carePlan) {
        return carePlanRepository.findById(id)
                .map(existing -> {
                    // Clear existing activities
                    existing.getCareActivities().clear();

                    // Add new activities and set bidirectional relationship
                    if (carePlan.getCareActivities() != null) {
                        for (CareActivity activity : carePlan.getCareActivities()) {
                            activity.setCarePlan(existing);
                            existing.getCareActivities().add(activity);
                        }
                    }
                    
                    return carePlanRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("CarePlan not found with id: " + id));
    }

    public void deleteCarePlan(Long id) {
        carePlanRepository.deleteById(id);
    }

    public CareActivity updateActivityStatus(Long activityId, String status) {
        return careActivityRepository.findById(activityId)
                .map(activity -> {
                    activity.setCompletionStatus(status);
                    return careActivityRepository.save(activity);
                })
                .orElseThrow(() -> new RuntimeException("CareActivity not found with id: " + activityId));
    }
}
