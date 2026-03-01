package org.techhive.trackingservice.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.techhive.trackingservice.entity.Medication;
import org.techhive.trackingservice.entity.Prescription;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
public class PrescriptionPdfService {

    /**
     * Generate a prescription PDF.
     * If signatureImage is non-null, the doctor's signature will appear at the bottom.
     */
    public byte[] generatePrescriptionPdf(Prescription prescription, byte[] signatureImage) throws DocumentException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, out);

        document.open();

        // Colors
        Color primaryColor = new Color(59, 130, 246); // Blue-500
        Color listHeaderColor = new Color(243, 244, 246); // Gray-100

        // Add Header Logo/Title
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, primaryColor);
        Paragraph title = new Paragraph("Tfakkarni Medical Prescription", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        
        Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
        Paragraph subTitle = new Paragraph("Alzheimer Care & Monitoring Platform", subTitleFont);
        subTitle.setAlignment(Element.ALIGN_CENTER);
        subTitle.setSpacingAfter(20f);
        document.add(subTitle);

        // Add Separator Line
        com.lowagie.text.pdf.draw.LineSeparator separator = new com.lowagie.text.pdf.draw.LineSeparator();
        separator.setLineColor(Color.LIGHT_GRAY);
        document.add(separator);
        document.add(Chunk.NEWLINE);

        // Add Date
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
        
        String dateStr = prescription.getCreatedAt().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy - HH:mm"));
        Paragraph datePara = new Paragraph();
        datePara.add(new Chunk("Date: ", labelFont));
        datePara.add(new Chunk(dateStr, valueFont));
        document.add(datePara);
        
        document.add(Chunk.NEWLINE);
        document.add(new Paragraph("Prescribed Medications:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
        document.add(Chunk.NEWLINE);

        // Add Medications Table
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 2, 2, 4});
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        addTableHeader(table, listHeaderColor);
        
        if (prescription.getMedications() != null) {
            for (Medication med : prescription.getMedications()) {
                addRows(table, med);
            }
        }

        document.add(table);

        // Doctor Signature Section (if available)
        if (signatureImage != null && signatureImage.length > 0) {
            document.add(Chunk.NEWLINE);
            document.add(Chunk.NEWLINE);

            // Right-aligned signature block
            PdfPTable sigTable = new PdfPTable(1);
            sigTable.setWidthPercentage(40);
            sigTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            // Label
            PdfPCell labelCell = new PdfPCell(new Phrase("Signature du médecin:", 
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.DARK_GRAY)));
            labelCell.setBorder(Rectangle.NO_BORDER);
            labelCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            labelCell.setPaddingBottom(8f);
            sigTable.addCell(labelCell);

            // Signature image
            try {
                Image signatureImg = Image.getInstance(signatureImage);
                signatureImg.scaleToFit(150, 60);
                PdfPCell imgCell = new PdfPCell(signatureImg, false);
                imgCell.setBorder(Rectangle.NO_BORDER);
                imgCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                imgCell.setPaddingBottom(4f);
                sigTable.addCell(imgCell);
            } catch (Exception e) {
                // If the image fails to load, add a placeholder text
                PdfPCell errorCell = new PdfPCell(new Phrase("[Signature]", 
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, Color.GRAY)));
                errorCell.setBorder(Rectangle.NO_BORDER);
                errorCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                sigTable.addCell(errorCell);
            }

            // Separator line under signature
            PdfPCell lineCell = new PdfPCell();
            lineCell.setBorder(Rectangle.TOP);
            lineCell.setBorderColor(Color.DARK_GRAY);
            lineCell.setBorderWidth(1f);
            lineCell.setFixedHeight(2f);
            sigTable.addCell(lineCell);

            document.add(sigTable);
        }
        
        // Footer Note
        document.add(Chunk.NEWLINE);
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, Color.GRAY);
        Paragraph footer = new Paragraph("Please follow the instructions carefully. consult your doctor for any side effects.", footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();

        return out.toByteArray();
    }

    /**
     * Overload for backward compatibility (no signature).
     */
    public byte[] generatePrescriptionPdf(Prescription prescription) throws DocumentException, IOException {
        return generatePrescriptionPdf(prescription, null);
    }

    private void addTableHeader(PdfPTable table, Color bgColor) {
        String[] headers = {"Medication Name", "Dosage", "Frequency", "Instructions"};
        for (String header : headers) {
            PdfPCell headerCell = new PdfPCell();
            headerCell.setBackgroundColor(bgColor);
            headerCell.setBorderWidth(1);
            headerCell.setBorderColor(Color.LIGHT_GRAY);
            headerCell.setPhrase(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
            headerCell.setPadding(8);
            headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(headerCell);
        }
    }

    private void addRows(PdfPTable table, Medication med) {
        addCell(table, med.getMedicationName());
        addCell(table, med.getDosage());
        addCell(table, med.getFrequency());
        addCell(table, med.getInstructions() != null ? med.getInstructions() : "");
    }
    
    private void addCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setPadding(6);
        cell.setBorderColor(Color.LIGHT_GRAY);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }
}
