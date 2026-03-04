package org.techhive.medicalservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.techhive.medicalservice.dto.AIReportResponse;
import org.techhive.medicalservice.service.AIReportService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/ai-reports")
@RequiredArgsConstructor
@Slf4j
public class AIReportController {

	private final AIReportService aiReportService;

	@GetMapping
	public ResponseEntity<List<AIReportResponse>> getByFolderId(@RequestParam Long folderId) {
		log.info("GET /api/ai-reports?folderId={}", folderId);
		List<AIReportResponse> list = aiReportService.getByFolderId(folderId);
		return ResponseEntity.ok(list);
	}

	@GetMapping("/latest")
	public ResponseEntity<AIReportResponse> getLatest(@RequestParam Long folderId) {
		log.info("GET /api/ai-reports/latest?folderId={}", folderId);
		return aiReportService.getLatestByFolderId(folderId)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.noContent().build());
	}

	@PostMapping("/generate/{folderId}")
	public ResponseEntity<AIReportResponse> generate(@PathVariable Long folderId) {
		log.info("POST /api/ai-reports/generate/{}", folderId);
		AIReportResponse report = aiReportService.generateReport(folderId);
		return ResponseEntity.accepted().body(report);
	}
}
