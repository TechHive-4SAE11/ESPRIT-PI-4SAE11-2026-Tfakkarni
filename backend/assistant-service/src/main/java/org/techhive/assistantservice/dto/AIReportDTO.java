package org.techhive.assistantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIReportDTO {
    private Long id;
    private Long medicalFolderId;
    private LocalDateTime generatedAt;
    private String reportJson; // Contient les recommandations et le résumé de l'état du patient
    private String status;
}
