package org.techhive.medicalservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.techhive.medicalservice.service.ConsolidatedRecordService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/medical-folders")
public class ConsolidatedRecordController {

    @Autowired
    private ConsolidatedRecordService consolidatedRecordService;

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadConsolidatedPdf(@PathVariable("id") Long id) {
        byte[] pdfContent = consolidatedRecordService.generateConsolidatedPdf(id);

        String filename = "MedicalRecord_Folder_" + id + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfContent);
    }
}
