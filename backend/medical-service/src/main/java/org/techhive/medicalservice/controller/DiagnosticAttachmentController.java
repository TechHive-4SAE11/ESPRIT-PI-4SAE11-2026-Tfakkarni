package org.techhive.medicalservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.techhive.medicalservice.dto.DiagnosticAttachmentResponse;
import org.techhive.medicalservice.entity.DiagnosticAttachment;
import org.techhive.medicalservice.service.FileStorageService;

import java.util.List;

@RestController
@RequestMapping("/api/diagnostic-attachments")
@RequiredArgsConstructor
@Slf4j
public class DiagnosticAttachmentController {

	private final FileStorageService fileStorageService;

	@PostMapping("/upload")
	public ResponseEntity<DiagnosticAttachmentResponse> uploadFile(
			@RequestParam("file") MultipartFile file,
			@RequestParam(value = "description", required = false) String description,
			@RequestParam(value = "diagnosticId", required = false) Long diagnosticId
	) {
		try {
			String fileType = fileStorageService.determineFileType(file.getContentType(), file.getOriginalFilename());
			DiagnosticAttachment attachment = fileStorageService.createAttachment(file, description, fileType);
			
			DiagnosticAttachmentResponse response = DiagnosticAttachmentResponse.builder()
				.id(attachment.getId())
				.diagnosticId(diagnosticId)
				.fileName(attachment.getFileName())
				.originalFileName(attachment.getOriginalFileName())
				.contentType(attachment.getContentType())
				.fileSize(attachment.getFileSize())
				.description(attachment.getDescription())
				.fileType(attachment.getFileType())
				.createdAt(attachment.getCreatedAt())
				.updatedAt(attachment.getUpdatedAt())
				.build();
				
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException e) {
			log.error("File upload validation error: {}", e.getMessage());
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		} catch (Exception e) {
			log.error("Error uploading file: {}", e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file");
		}
	}

	@PostMapping("/upload-multiple")
	public ResponseEntity<List<DiagnosticAttachmentResponse>> uploadMultipleFiles(
			@RequestParam("files") MultipartFile[] files,
			@RequestParam(value = "descriptions", required = false) String[] descriptions,
			@RequestParam(value = "diagnosticId", required = false) Long diagnosticId
	) {
		try {
			List<DiagnosticAttachmentResponse> responses = new java.util.ArrayList<>();
			
			for (int i = 0; i < files.length; i++) {
				MultipartFile file = files[i];
				String description = (descriptions != null && i < descriptions.length) ? descriptions[i] : null;
				String fileType = fileStorageService.determineFileType(file.getContentType(), file.getOriginalFilename());
				DiagnosticAttachment attachment = fileStorageService.createAttachment(file, description, fileType);
				
				DiagnosticAttachmentResponse response = DiagnosticAttachmentResponse.builder()
					.id(attachment.getId())
					.diagnosticId(diagnosticId)
					.fileName(attachment.getFileName())
					.originalFileName(attachment.getOriginalFileName())
					.contentType(attachment.getContentType())
					.fileSize(attachment.getFileSize())
					.description(attachment.getDescription())
					.fileType(attachment.getFileType())
					.createdAt(attachment.getCreatedAt())
					.updatedAt(attachment.getUpdatedAt())
					.build();
					
				responses.add(response);
			}
			
			return ResponseEntity.ok(responses);
		} catch (IllegalArgumentException e) {
			log.error("File upload validation error: {}", e.getMessage());
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		} catch (Exception e) {
			log.error("Error uploading files: {}", e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload files");
		}
	}

	@GetMapping("/download/{id}")
	public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
		try {
			// This would need repository to fetch the attachment
			// For now, returning a placeholder response
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File download not implemented yet");
		} catch (Exception e) {
			log.error("Error downloading file: {}", e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to download file");
		}
	}

	@GetMapping("/view/{id}")
	public ResponseEntity<Resource> viewFile(@PathVariable Long id) {
		try {
			// This would need repository to fetch the attachment
			// For now, returning a placeholder response
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File view not implemented yet");
		} catch (Exception e) {
			log.error("Error viewing file: {}", e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to view file");
		}
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteFile(@PathVariable Long id) {
		try {
			// This would need repository to delete the attachment
			// For now, returning a placeholder response
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File deletion not implemented yet");
		} catch (Exception e) {
			log.error("Error deleting file: {}", e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete file");
		}
	}

	@GetMapping("/diagnostic/{diagnosticId}")
	public ResponseEntity<List<DiagnosticAttachmentResponse>> getAttachmentsByDiagnostic(@PathVariable Long diagnosticId) {
		try {
			// This would need repository to fetch attachments
			// For now, returning a placeholder response
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Get attachments not implemented yet");
		} catch (Exception e) {
			log.error("Error fetching attachments: {}", e.getMessage(), e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch attachments");
		}
	}
}
