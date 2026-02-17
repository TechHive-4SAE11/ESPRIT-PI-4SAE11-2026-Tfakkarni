package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionRequestDTO {
    
    private Long medicalFolderId;
    private LocalDateTime sessionDate;
    private String notes;
}
