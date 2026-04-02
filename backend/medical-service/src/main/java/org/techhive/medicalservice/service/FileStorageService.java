package org.techhive.medicalservice.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.techhive.medicalservice.entity.DiagnosticAttachment;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
public class FileStorageService {

	private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
		"image/jpeg", "image/png", "image/gif", "image/webp",
		"application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
		"application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
		"text/plain", "text/csv"
	);

	private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

	public DiagnosticAttachment createAttachment(MultipartFile file, String description, String fileType) {
		validateFile(file);
		
		try {
			byte[] fileData = file.getBytes();
			
			return DiagnosticAttachment.builder()
				.fileName(generateFileName(file.getOriginalFilename()))
				.originalFileName(file.getOriginalFilename())
				.contentType(file.getContentType())
				.fileSize(file.getSize())
				.fileData(fileData)
				.description(description)
				.fileType(fileType)
				.build();
		} catch (IOException e) {
			throw new RuntimeException("Failed to process file: " + e.getMessage(), e);
		}
	}

	private void validateFile(MultipartFile file) {
		if (file.isEmpty()) {
			throw new IllegalArgumentException("File cannot be empty");
		}

		if (file.getSize() > MAX_FILE_SIZE) {
			throw new IllegalArgumentException("File size cannot exceed 10MB");
		}

		String contentType = file.getContentType();
		if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
			throw new IllegalArgumentException("File type not allowed. Allowed types: " + ALLOWED_CONTENT_TYPES);
		}
	}

	private String generateFileName(String originalFilename) {
		if (originalFilename == null || originalFilename.isEmpty()) {
			return "unknown_file_" + System.currentTimeMillis();
		}
		
		String extension = "";
		int lastDotIndex = originalFilename.lastIndexOf('.');
		if (lastDotIndex > 0) {
			extension = originalFilename.substring(lastDotIndex);
		}
		
		return "file_" + System.currentTimeMillis() + extension;
	}

	public String determineFileType(String contentType, String originalFilename) {
		if (contentType == null) return "OTHER";
		
		if (contentType.startsWith("image/")) return "PHOTO";
		if (contentType.equals("application/pdf")) return "DOCUMENT";
		if (contentType.contains("word") || contentType.contains("document")) return "DOCUMENT";
		if (contentType.contains("excel") || contentType.contains("spreadsheet")) return "DOCUMENT";
		if (contentType.contains("text")) return "DOCUMENT";
		
		// Check filename for medical imaging patterns
		String lowerFilename = originalFilename.toLowerCase();
		if (lowerFilename.contains("xray") || lowerFilename.contains("radiography")) return "XRAY";
		if (lowerFilename.contains("mri")) return "MRI";
		if (lowerFilename.contains("ct") || lowerFilename.contains("scan")) return "CT_SCAN";
		if (lowerFilename.contains("ultrasound") || lowerFilename.contains("sonography")) return "ULTRASOUND";
		
		return "OTHER";
	}
}
