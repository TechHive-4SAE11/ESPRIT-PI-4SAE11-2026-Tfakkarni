package org.techhive.medicalservice.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import org.techhive.medicalservice.entity.MedicalFolder;
import org.techhive.medicalservice.entity.MedicalHistory;
import org.techhive.medicalservice.entity.Diagnostics;
import org.techhive.medicalservice.client.GameServiceClient;
import org.techhive.medicalservice.client.TrackingServiceClient;
import org.techhive.medicalservice.repository.MedicalFolderRepository;
import org.techhive.medicalservice.exception.ResourceNotFoundException;

import java.io.ByteArrayOutputStream;
import java.awt.Color;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
public class ConsolidatedRecordService {

    @Autowired
    private MedicalFolderRepository medicalFolderRepository;

    @Autowired
    private GameServiceClient gameServiceClient;

    @Autowired
    private TrackingServiceClient trackingServiceClient;

    public byte[] generateConsolidatedPdf(Long folderId) {
        MedicalFolder folder = medicalFolderRepository.findById(folderId)
                .orElseThrow(() -> new ResourceNotFoundException("Medical Folder not found"));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.DARK_GRAY);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLUE);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

            // Header
            Paragraph title = new Paragraph("Tfakkarni - Patient Medical Record", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Patient Information Section
            addSectionTitle(document, "Patient Information", sectionFont);
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(15);
            addTableCell(infoTable, "Patient ID:", labelFont);
            addTableCell(infoTable, folder.getPatientId(), bodyFont);
            addTableCell(infoTable, "Doctor ID:", labelFont);
            addTableCell(infoTable, folder.getDoctorId(), bodyFont);
            addTableCell(infoTable, "Created Date:", labelFont);
            addTableCell(infoTable, folder.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), bodyFont);
            document.add(infoTable);

            // Medical History (Allergies, Conditions, Medications)
            if (folder.getMedicalHistories() != null && !folder.getMedicalHistories().isEmpty()) {
                addSectionTitle(document, "Medical History", sectionFont);
                for (MedicalHistory history : folder.getMedicalHistories()) {
                    Paragraph hPara = new Paragraph();
                    hPara.add(new Chunk("Conditions: ", labelFont));
                    hPara.add(new Chunk(history.getConditions() + "\n", bodyFont));
                    hPara.add(new Chunk("Allergies: ", labelFont));
                    hPara.add(new Chunk(history.getAllergies() + "\n", bodyFont));
                    hPara.add(new Chunk("Daily Medications: ", labelFont));
                    hPara.add(new Chunk(history.getMedications() + "\n", bodyFont));
                    hPara.setSpacingAfter(10);
                    document.add(hPara);
                }
            }

            // Game Performance (from Game Service)
            try {
                addSectionTitle(document, "Cognitive Performance (Memory Games)", sectionFont);
                GameServiceClient.GameStatsDTO gameStats = gameServiceClient.getPatientGameStats(folder.getPatientId());
                if (gameStats != null) {
                    Paragraph gPara = new Paragraph();
                    gPara.add(new Chunk("Total Games Played: ", labelFont));
                    gPara.add(new Chunk(gameStats.getGamesPlayed() + "\n", bodyFont));
                    gPara.add(new Chunk("Average Score: ", labelFont));
                    gPara.add(new Chunk(String.format("%.2f", gameStats.getAverageScore()) + "/100\n", bodyFont));
                    document.add(gPara);

                    if (gameStats.getRecentAttempts() != null && !gameStats.getRecentAttempts().isEmpty()) {
                        PdfPTable gameTable = new PdfPTable(3);
                        gameTable.setWidthPercentage(100);
                        gameTable.setSpacingBefore(5);
                        addTableCell(gameTable, "Game", labelFont);
                        addTableCell(gameTable, "Score", labelFont);
                        addTableCell(gameTable, "Date", labelFont);
                        for (GameServiceClient.GameAttemptDTO attempt : gameStats.getRecentAttempts()) {
                            addTableCell(gameTable, attempt.getGameName(), bodyFont);
                            addTableCell(gameTable, attempt.getScore().toString(), bodyFont);
                            addTableCell(gameTable, attempt.getPlayedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), bodyFont);
                        }
                        document.add(gameTable);
                    }
                }
            } catch (Exception e) {
                document.add(new Paragraph("Unable to retrieve game statistics: Service unavailable.", bodyFont));
            }

            // Tracking Data (from Tracking Service)
            try {
                addSectionTitle(document, "Daily Tracking & Compliance", sectionFont);
                TrackingServiceClient.TrackingSummaryDTO trackingData = trackingServiceClient.getPatientTrackingSummary(folder.getPatientId());
                if (trackingData != null) {
                    Paragraph tPara = new Paragraph();
                    tPara.add(new Chunk("Medication Compliance: ", labelFont));
                    tPara.add(new Chunk(String.format("%.1f %%", trackingData.getMedicationCompliance() * 100) + "\n", bodyFont));
                    document.add(tPara);

                    if (trackingData.getRecentIncidents() != null && !trackingData.getRecentIncidents().isEmpty()) {
                        document.add(new Paragraph("Recent Incidents/Alerts:", labelFont));
                        for (TrackingServiceClient.IncidentDTO incident : trackingData.getRecentIncidents()) {
                            document.add(new Paragraph("- [" + incident.getType() + "]: " + incident.getDescription(), bodyFont));
                        }
                    }
                }
            } catch (Exception e) {
                document.add(new Paragraph("Unable to retrieve tracking data: Service unavailable.", bodyFont));
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF report", e);
        }
    }

    private void addSectionTitle(Document document, String title, Font font) throws DocumentException {
        Paragraph p = new Paragraph(title, font);
        p.setSpacingBefore(15);
        p.setSpacingAfter(10);
        document.add(p);
    }

    private void addTableCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        table.addCell(cell);
    }
}
