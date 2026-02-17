package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarePlanRequestDTO {
    
    private Long sessionId;
    private List<CareActivityRequestDTO> activities = new ArrayList<>();
}
