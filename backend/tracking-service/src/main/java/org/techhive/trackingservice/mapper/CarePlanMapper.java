package org.techhive.trackingservice.mapper;

import org.springframework.stereotype.Component;
import org.techhive.trackingservice.dto.CareActivityRequestDTO;
import org.techhive.trackingservice.dto.CareActivityResponseDTO;
import org.techhive.trackingservice.dto.CarePlanRequestDTO;
import org.techhive.trackingservice.dto.CarePlanResponseDTO;
import org.techhive.trackingservice.entity.CareActivity;
import org.techhive.trackingservice.entity.CarePlan;

import java.util.stream.Collectors;

@Component
public class CarePlanMapper {

    // CarePlan Mappings
    public CarePlan toEntity(CarePlanRequestDTO dto) {
        if (dto == null) return null;
        
        CarePlan carePlan = new CarePlan();
        // Session ID is handled by service
        
        // Map activities if present
        if (dto.getActivities() != null) {
            carePlan.setCareActivities(dto.getActivities().stream()
                    .map(this::toActivityEntity)
                    .collect(Collectors.toList()));
        }
        
        return carePlan;
    }

    public CarePlanResponseDTO toResponseDTO(CarePlan carePlan) {
        if (carePlan == null) return null;
        
        CarePlanResponseDTO dto = new CarePlanResponseDTO();
        dto.setId(carePlan.getId());
        
        if (carePlan.getSession() != null) {
            dto.setSessionId(carePlan.getSession().getId());
        }
        
        if (carePlan.getCareActivities() != null) {
            dto.setActivities(carePlan.getCareActivities().stream()
                    .map(this::toActivityResponseDTO)
                    .collect(Collectors.toList()));
        }
        
        dto.setCreatedAt(carePlan.getCreatedAt());
        dto.setUpdatedAt(carePlan.getUpdatedAt());
        
        return dto;
    }

    // CareActivity Mappings
    public CareActivity toActivityEntity(CareActivityRequestDTO dto) {
        if (dto == null) return null;
        
        CareActivity activity = new CareActivity();
        activity.setActivityName(dto.getActivityName());
        activity.setDescription(dto.getDescription());
        activity.setFrequency(dto.getFrequency());
        activity.setDuration(dto.getDuration());
        
        // Default status
        activity.setCompletionStatus("PENDING");
        
        return activity;
    }

    public CareActivityResponseDTO toActivityResponseDTO(CareActivity activity) {
        if (activity == null) return null;
        
        CareActivityResponseDTO dto = new CareActivityResponseDTO();
        dto.setId(activity.getId());
        dto.setActivityName(activity.getActivityName());
        dto.setDescription(activity.getDescription());
        dto.setFrequency(activity.getFrequency());
        dto.setDuration(activity.getDuration());
        dto.setCompletionStatus(activity.getCompletionStatus());
        dto.setCreatedAt(activity.getCreatedAt());
        
        return dto;
    }
}
