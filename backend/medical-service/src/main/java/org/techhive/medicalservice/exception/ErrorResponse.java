package org.techhive.medicalservice.exception;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime timestamp;

	private Integer status;

	private String error;

	private String message;

	private String path;

	private List<ValidationError> validationErrors;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ValidationError {
		private String field;
		private String message;
	}
}
