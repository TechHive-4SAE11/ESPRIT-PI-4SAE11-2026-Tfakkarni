package org.techhive.trackingservice.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.techhive.trackingservice.entity.MedicalMeeting;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/**
 * Generates a rich PDF report for a completed MedicalMeeting.
 * Uses OpenPDF (com.github.librepdf:openpdf) — already in pom.xml.
 *
 * Sections included:
 *   1. Meeting metadata (patient, doctor, date, duration)
 *   2. Doctor notes
 *   3. Live transcript
 *   4. Periodic Groq mini-summaries (from transcriptSummaries JSON)
 *   5. Final Groq AI summary
 */
@Service
@Slf4j
public class MeetingPdfService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Design palette
    private static final Color PRIMARY  = new Color(5, 150, 105);    // emerald-600
    private static final Color PURPLE   = new Color(109, 40, 217);   // purple-700
    private static final Color DARK     = new Color(17, 24, 39);     // gray-900
    private static final Color MID      = new Color(75, 85, 99);     // gray-600
    private static final Color LIGHT_BG = new Color(243, 244, 246);  // gray-100
    private static final Color BORDER   = new Color(209, 213, 219);  // gray-300

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    public byte[] generateMeetingPdf(MedicalMeeting m) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 45, 45, 50, 50);
        PdfWriter.getInstance(doc, out);
        doc.open();

        addHeader(doc);
        addMetaTable(doc, m);

        if (hasContent(m.getNotes())) {
            addSection(doc, "\u00a0 \ud83d\udcdd  Notes de la R\u00e9union", DARK);
            addTextBox(doc, m.getNotes(), LIGHT_BG, MID);
        }

        if (hasContent(m.getTranscript())) {
            addSection(doc, "\u00a0 \ud83c\udfa4  Transcription en Direct", new Color(30, 64, 175));
            addTextBox(doc, m.getTranscript(),
                    new Color(239, 246, 255), new Color(30, 64, 175));
        }

        if (hasContent(m.getTranscriptSummaries())) {
            addSection(doc, "\u00a0 \ud83e\udd16  R\u00e9sum\u00e9s P\u00e9riodiques (Groq)", PURPLE);
            addTranscriptSummariesSection(doc, m.getTranscriptSummaries());
        }

        if (hasContent(m.getAiSummary())) {
            addSection(doc, "\u00a0 \u2728  R\u00e9sum\u00e9 AI Final (Groq)", PRIMARY);
            addAiSummaryBox(doc, m.getAiSummary());
        }

        addFooter(doc);
        doc.close();
        log.info("PDF generated for meeting {} ({} bytes)", m.getId(), out.size());
        return out.toByteArray();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTIONS
    // ─────────────────────────────────────────────────────────────────────────

    private void addHeader(Document doc) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, PRIMARY);
        Paragraph title = new Paragraph("Rapport de R\u00e9union M\u00e9dicale", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        doc.add(title);

        Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 9, MID);
        Paragraph sub = new Paragraph(
                "Plateforme Tfakkarni \u2014 Suivi des Patients Alzheimer", subFont);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(14f);
        doc.add(sub);

        addHR(doc, PRIMARY, 1.5f);
        doc.add(Chunk.NEWLINE);
    }

    private void addMetaTable(Document doc, MedicalMeeting m) throws DocumentException {
        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1.4f, 2.6f, 1.4f, 2.6f});
        t.setSpacingAfter(16f);

        String dateStr = m.getCreatedAt() != null ? m.getCreatedAt().format(DATE_FMT) : "N/A";
        String durStr  = m.getDurationMinutes() != null ? m.getDurationMinutes() + " min" : "N/A";
        String endStr  = m.getEndedAt() != null ? m.getEndedAt().format(DATE_FMT) : "\u2014";
        String status  = switch (m.getStatus().name()) {
            case "ENDED"     -> "Termin\u00e9e";
            case "ACTIVE"    -> "En cours";
            default          -> "Planifi\u00e9e";
        };

        metaRow(t, "Patient",  safe(m.getPatientName()),   "M\u00e9decin",  "Dr. " + safe(m.getDoctorName()));
        metaRow(t, "Date",     dateStr,                     "Dur\u00e9e",    durStr);
        metaRow(t, "Statut",   status,                      "Fin",           endStr);
        doc.add(t);
    }

    private void addTranscriptSummariesSection(Document doc, String json) throws DocumentException {
        // JSON: [{"label":"...","summary":"..."}, ...]
        String inner = json.trim().replaceAll("^\\[|\\]$", "");
        String[] entries = inner.split("\\},\\s*\\{");
        for (String entry : entries) {
            String label   = extractField(entry, "label");
            String summary = extractField(entry, "summary");
            if (label != null && summary != null) {
                addMiniCard(doc, label, summary);
            }
        }
    }

    private void addAiSummaryBox(Document doc, String text) throws DocumentException {
        PdfPTable box = new PdfPTable(1);
        box.setWidthPercentage(100);
        box.setSpacingAfter(10f);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new Color(236, 253, 245)); // emerald-50
        cell.setBorderColor(new Color(110, 231, 183));
        cell.setBorderWidth(0.6f);
        cell.setPadding(10f);

        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("## ")) {
                Font hf = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, PRIMARY);
                Paragraph h = new Paragraph(trimmed.substring(3), hf);
                h.setSpacingBefore(8f);
                h.setSpacingAfter(3f);
                cell.addElement(h);
            } else if (!trimmed.isEmpty()) {
                Font pf = FontFactory.getFont(FontFactory.HELVETICA, 9, DARK);
                Paragraph p = new Paragraph(trimmed, pf);
                p.setSpacingAfter(2f);
                cell.addElement(p);
            }
        }
        box.addCell(cell);
        doc.add(box);
    }

    private void addFooter(Document doc) throws DocumentException {
        doc.add(Chunk.NEWLINE);
        addHR(doc, BORDER, 0.7f);
        Font f = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, new Color(156, 163, 175));
        Paragraph footer = new Paragraph(
                "G\u00e9n\u00e9r\u00e9 automatiquement le "
                + java.time.LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy '\u00e0' HH:mm"))
                + " \u2014 Tfakkarni \u00a9 2026 \u2014 Document confidentiel", f);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(6f);
        doc.add(footer);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOW-LEVEL HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private void addHR(Document doc, Color color, float width) throws DocumentException {
        com.lowagie.text.pdf.draw.LineSeparator sep =
                new com.lowagie.text.pdf.draw.LineSeparator();
        sep.setLineColor(color);
        sep.setLineWidth(width);
        doc.add(new Chunk(sep));
    }

    private void addSection(Document doc, String text, Color color) throws DocumentException {
        doc.add(Chunk.NEWLINE);
        Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, color);
        Paragraph p = new Paragraph(text, f);
        p.setSpacingBefore(4f);
        p.setSpacingAfter(6f);
        doc.add(p);
    }

    private void addTextBox(Document doc, String text,
                             Color bg, Color textColor) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(10f);
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(bg);
        c.setBorderColor(BORDER);
        c.setBorderWidth(0.5f);
        c.setPadding(10f);
        c.addElement(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 9, textColor)));
        t.addCell(c);
        doc.add(t);
    }

    private void addMiniCard(Document doc, String label, String summary) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(7f);

        // label row
        PdfPCell lc = new PdfPCell();
        lc.setBackgroundColor(new Color(233, 213, 255));
        lc.setBorderColor(new Color(192, 132, 252));
        lc.setBorderWidth(0.5f);
        lc.setPadding(5f);
        lc.addElement(new Phrase(label,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(91, 33, 182))));
        t.addCell(lc);

        // summary row
        PdfPCell sc = new PdfPCell();
        sc.setBackgroundColor(new Color(250, 245, 255));
        sc.setBorderColor(new Color(192, 132, 252));
        sc.setBorderWidth(0.5f);
        sc.setBorderWidthTop(0);
        sc.setPadding(7f);
        sc.addElement(new Phrase(summary,
                FontFactory.getFont(FontFactory.HELVETICA, 9, MID)));
        t.addCell(sc);

        doc.add(t);
    }

    private void metaRow(PdfPTable table,
                         String l1, String v1,
                         String l2, String v2) {
        Font lf = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, MID);
        Font vf = FontFactory.getFont(FontFactory.HELVETICA, 10, DARK);
        for (int i = 0; i < 4; i++) {
            PdfPCell c = new PdfPCell();
            c.setBackgroundColor(i % 2 == 0 ? LIGHT_BG : Color.WHITE);
            c.setBorderColor(BORDER);
            c.setBorderWidth(0.5f);
            c.setPadding(8f);
            String txt = switch (i) {
                case 0 -> l1; case 1 -> v1 != null ? v1 : "\u2014";
                case 2 -> l2; case 3 -> v2 != null ? v2 : "\u2014";
                default -> "";
            };
            c.addElement(new Phrase(txt, i % 2 == 0 ? lf : vf));
            table.addCell(c);
        }
    }

    private boolean hasContent(String s) {
        return s != null && !s.isBlank();
    }

    private String safe(String s) {
        return s != null ? s : "\u2014";
    }

    /** Simple JSON string-field extractor (no external dependency needed). */
    private String extractField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start = json.indexOf(key);
        if (start < 0) return null;
        start += key.length();
        int end = start;
        while (end < json.length()) {
            char ch = json.charAt(end);
            if (ch == '"' && (end == 0 || json.charAt(end - 1) != '\\')) break;
            end++;
        }
        return json.substring(start, end)
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\\\", "\\");
    }
}
