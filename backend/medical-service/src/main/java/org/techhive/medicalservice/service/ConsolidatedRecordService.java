package org.techhive.medicalservice.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import org.techhive.medicalservice.entity.MedicalFolder;
import org.techhive.medicalservice.entity.MedicalHistory;
import org.techhive.medicalservice.client.GameServiceClient;
import org.techhive.medicalservice.client.TrackingServiceClient;
import org.techhive.medicalservice.client.UserServiceClient;
import org.techhive.medicalservice.dto.game.GameStatsDTO;
import org.techhive.medicalservice.dto.game.GameAttemptDTO;
import org.techhive.medicalservice.dto.tracking.MedicationComplianceDTO;
import org.techhive.medicalservice.dto.tracking.IncidentStatsDTO;
import org.techhive.medicalservice.repository.MedicalFolderRepository;
import org.techhive.medicalservice.exception.ResourceNotFoundException;

import java.io.ByteArrayOutputStream;
import java.awt.Color;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ConsolidatedRecordService {

    @Autowired
    private MedicalFolderRepository medicalFolderRepository;

    @Autowired
    private GameServiceClient gameServiceClient;

    @Autowired
    private TrackingServiceClient trackingServiceClient;

    @Autowired
    private UserServiceClient userServiceClient;

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

            // Resolve patient & doctor names
            String patientName = resolveUserName(folder.getPatientId());
            String doctorName  = resolveUserName(folder.getDoctorId());

            // Patient Information Section
            addSectionTitle(document, "Patient Information", sectionFont);
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(15);
            addTableCell(infoTable, "Patient:", labelFont);
            addTableCell(infoTable, patientName, bodyFont);
            addTableCell(infoTable, "Doctor:", labelFont);
            addTableCell(infoTable, doctorName, bodyFont);
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
                    hPara.add(new Chunk("Symptoms: ", labelFont));
                    hPara.add(new Chunk(history.getSymptoms() + "\n", bodyFont));
                    hPara.add(new Chunk("Recommended Treatment: ", labelFont));
                    hPara.add(new Chunk(history.getRecommendedTreatment() + "\n", bodyFont));
                    hPara.setSpacingAfter(10);
                    document.add(hPara);
                }
            }

            // Game Performance (from Game Service)
            try {
                addSectionTitle(document, "Cognitive Performance (Memory Games)", sectionFont);
                GameStatsDTO gameStats = gameServiceClient.getPatientGameStats(folder.getPatientId());
                if (gameStats != null) {
                    int gamesPlayed = gameStats.getTotalGamesPlayed() != null ? gameStats.getTotalGamesPlayed() : 0;
                    double avgScore = gameStats.getAverageScore() != null ? gameStats.getAverageScore() : 0.0;
                    Paragraph gPara = new Paragraph();
                    gPara.add(new Chunk("Total Games Played: ", labelFont));
                    gPara.add(new Chunk(gamesPlayed + "\n", bodyFont));
                    gPara.add(new Chunk("Average Score: ", labelFont));
                    gPara.add(new Chunk(String.format("%.2f", avgScore) + "/100\n", bodyFont));
                    document.add(gPara);

                    if (gameStats.getRecentAttempts() != null && !gameStats.getRecentAttempts().isEmpty()) {
                        PdfPTable gameTable = new PdfPTable(3);
                        gameTable.setWidthPercentage(100);
                        gameTable.setSpacingBefore(5);
                        addTableCell(gameTable, "Game", labelFont);
                        addTableCell(gameTable, "Score", labelFont);
                        addTableCell(gameTable, "Date", labelFont);
                        for (GameAttemptDTO attempt : gameStats.getRecentAttempts()) {
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

                // Per-medication compliance table
                List<MedicationComplianceDTO> drugCompliance =
                        trackingServiceClient.getPatientMedicationComplianceByDrug(folder.getPatientId());
                if (drugCompliance != null && !drugCompliance.isEmpty()) {
                    PdfPTable medTable = new PdfPTable(4);
                    medTable.setWidthPercentage(100);
                    medTable.setSpacingBefore(5);
                    medTable.setSpacingAfter(10);
                    medTable.setWidths(new float[]{3f, 2.5f, 1.2f, 1.3f});

                    // Header row
                    Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
                    for (String header : new String[]{"Medication", "Date Range", "Taken / Total", "Adherence"}) {
                        PdfPCell hCell = new PdfPCell(new Phrase(header, tableHeaderFont));
                        hCell.setBackgroundColor(new Color(41, 98, 255));
                        hCell.setPadding(6);
                        medTable.addCell(hCell);
                    }

                    // Data rows
                    int globalTaken = 0, globalTotal = 0;
                    for (MedicationComplianceDTO drug : drugCompliance) {
                        int taken = drug.getTaken();
                        int total = taken + drug.getMissed();
                        double pct = total > 0 ? (taken * 100.0) / total : 0;
                        globalTaken += taken;
                        globalTotal += total;

                        String dateRange = (drug.getStartDate() != null ? drug.getStartDate() : "?") +
                                " → " + (drug.getEndDate() != null ? drug.getEndDate() : "?");

                        addTableCell(medTable, drug.getMedicationName() != null ? drug.getMedicationName() : "Unknown", bodyFont);
                        addTableCell(medTable, dateRange, bodyFont);
                        addTableCell(medTable, taken + " / " + total, bodyFont);
                        addTableCell(medTable, String.format("%.0f %%", pct), bodyFont);
                    }
                    document.add(medTable);

                    // Overall compliance
                    double overallPct = globalTotal > 0 ? (globalTaken * 100.0) / globalTotal : 0;
                    Paragraph overall = new Paragraph();
                    overall.add(new Chunk("Overall Medication Adherence: ", labelFont));
                    overall.add(new Chunk(String.format("%.1f %%", overallPct) +
                            "  (" + globalTaken + " / " + globalTotal + " doses)", bodyFont));
                    overall.setSpacingAfter(10);
                    document.add(overall);
                } else {
                    document.add(new Paragraph("No medication data available for this patient.", bodyFont));
                }

                // Incidents summary
                IncidentStatsDTO incidents = trackingServiceClient.getPatientIncidentStats(folder.getPatientId());
                if (incidents != null && incidents.getLabels() != null && !incidents.getLabels().isEmpty()) {
                    document.add(new Paragraph("Recent Incidents (last 30 days):", labelFont));
                    for (int i = 0; i < incidents.getLabels().size(); i++) {
                        String label = incidents.getLabels().get(i);
                        int count = (incidents.getValues() != null && i < incidents.getValues().size())
                                ? incidents.getValues().get(i) : 0;
                        document.add(new Paragraph("- " + label + ": " + count, bodyFont));
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

    private String resolveUserName(String keycloakId) {
        try {
            com.fasterxml.jackson.databind.JsonNode user = userServiceClient.getUserByKeycloakId(keycloakId);
            if (user != null) {
                String first = user.has("firstName") ? user.get("firstName").asText("") : "";
                String last  = user.has("lastName")  ? user.get("lastName").asText("")  : "";
                String full  = (first + " " + last).trim();
                if (!full.isEmpty()) return full;
            }
        } catch (Exception ignored) { }
        return keycloakId;
    }
}
