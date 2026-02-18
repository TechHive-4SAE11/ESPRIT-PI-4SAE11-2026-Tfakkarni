package org.techhive.medicalservice.dto;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSessionRequest {

	@NotNull(message = "Medical folder ID cannot be null")
	private Long medicalFolderId;

	@NotNull(message = "Session date cannot be null")
	private LocalDateTime sessionDate;

	private String notes;
}
