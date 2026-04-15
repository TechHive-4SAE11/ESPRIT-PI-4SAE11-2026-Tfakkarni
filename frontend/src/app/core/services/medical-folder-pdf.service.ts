import { Injectable } from '@angular/core';
import { jsPDF } from 'jspdf';
import type { MedicalFolder } from './medical-folder.service';
import type { Diagnostics } from './diagnostics.service';
import type { MedicalHistory } from './medical-history.service';

const MARGIN = 18;
const PAGE_W = 210; // A4 mm
const CONTENT_W = PAGE_W - MARGIN * 2;
const FOOTER_Y = 287;
const GRAY = { r: 120, g: 120, b: 120 };
const LIGHT_GRAY = { r: 248, g: 248, b: 248 };
const LINE_GRAY = { r: 220, g: 220, b: 220 };

@Injectable({
  providedIn: 'root',
})
export class MedicalFolderPdfService {
  /**
   * Generates a PDF dossier for a medical folder with its diagnostics and medical history.
   * Returns a Blob so the caller can trigger download.
   */
  exportDossier(
    folder: MedicalFolder,
    diagnostics: Diagnostics[],
    history: MedicalHistory[],
    patientName?: string
  ): Blob {
    const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
    let y = MARGIN;
    let pageNum = 1;
    const totalPages = 1; // updated after first pass if needed

    const pushY = (dy: number): void => {
      y += dy;
      if (y > 265) {
        drawFooter(doc, pageNum);
        doc.addPage();
        pageNum++;
        y = MARGIN;
      }
    };

    const drawFooter = (d: jsPDF, current: number) => {
      d.setDrawColor(LINE_GRAY.r, LINE_GRAY.g, LINE_GRAY.b);
      d.setLineWidth(0.2);
      d.line(MARGIN, FOOTER_Y - 8, PAGE_W - MARGIN, FOOTER_Y - 8);
      d.setFontSize(8);
      d.setFont('helvetica', 'normal');
      d.setTextColor(GRAY.r, GRAY.g, GRAY.b);
      d.text(
        `Document confidentiel · Dossier #${folder.id} · Page ${current}`,
        PAGE_W / 2,
        FOOTER_Y - 4,
        { align: 'center' }
      );
      d.setTextColor(0, 0, 0);
    };

    const drawText = (text: string, fontSize: number, bold = false, indent = 0): void => {
      doc.setFontSize(fontSize);
      doc.setFont('helvetica', bold ? 'bold' : 'normal');
      const lines = doc.splitTextToSize(text || '—', CONTENT_W - indent);
      const x = MARGIN + indent;
      for (const line of lines) {
        doc.text(line, x, y);
        pushY(5);
      }
    };

    const drawSectionTitle = (title: string): void => {
      pushY(6);
      doc.setDrawColor(LINE_GRAY.r, LINE_GRAY.g, LINE_GRAY.b);
      doc.setLineWidth(0.3);
      doc.setFillColor(LIGHT_GRAY.r, LIGHT_GRAY.g, LIGHT_GRAY.b);
      doc.rect(MARGIN, y, CONTENT_W, 10, 'FD');
      doc.setFontSize(12);
      doc.setFont('helvetica', 'bold');
      doc.setTextColor(40, 40, 40);
      doc.text(title, MARGIN + 4, y + 7);
      doc.setTextColor(0, 0, 0);
      y += 12;
    };

    const drawBoxedBlock = (title: string, rows: [string, string][]): void => {
      if (y > 238) {
        drawFooter(doc, pageNum);
        doc.addPage();
        pageNum++;
        y = MARGIN;
      }
      doc.setFillColor(240, 243, 248);
      doc.rect(MARGIN, y, 3, 14, 'F');
      doc.setFontSize(10);
      doc.setFont('helvetica', 'bold');
      doc.text(title, MARGIN + 6, y + 6);
      y += 10;
      doc.setFont('helvetica', 'normal');
      doc.setFontSize(9);
      for (const [label, value] of rows) {
        const content = `${label}: ${value || '—'}`;
        const lines = doc.splitTextToSize(content, CONTENT_W - 12);
        for (const line of lines) {
          doc.text(line, MARGIN + 5, y + 4);
          pushY(5);
        }
      }
      doc.setDrawColor(LINE_GRAY.r, LINE_GRAY.g, LINE_GRAY.b);
      doc.setLineWidth(0.15);
      doc.line(MARGIN, y + 2, MARGIN + CONTENT_W, y + 2);
      pushY(6);
    };

    // —— Header ——
    doc.setFillColor(245, 247, 250);
    doc.rect(0, 0, PAGE_W, 32, 'F');
    doc.setDrawColor(200, 205, 215);
    doc.setLineWidth(0.4);
    doc.line(0, 32, PAGE_W, 32);

    doc.setFontSize(20);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(30, 41, 59);
    doc.text('DOSSIER MÉDICAL', MARGIN, 18);
    doc.setFontSize(9);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(GRAY.r, GRAY.g, GRAY.b);
    doc.text('Document officiel · À conserver avec le dossier patient', MARGIN, 25);
    doc.setTextColor(0, 0, 0);
    y = 40;

    // —— Patient & folder info (record header) ——
    doc.setDrawColor(LINE_GRAY.r, LINE_GRAY.g, LINE_GRAY.b);
    doc.setLineWidth(0.25);
    doc.rect(MARGIN, y, CONTENT_W, 36, 'D');
    doc.setFillColor(250, 250, 252);
    doc.rect(MARGIN + 0.5, y + 0.5, CONTENT_W - 1, 35, 'F');

    const col1 = MARGIN + 6;
    const col2 = MARGIN + 70;
    const col3 = MARGIN + 135;
    let rowY = y + 3;

    // Column 1: Patient
    doc.setFontSize(8);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(GRAY.r, GRAY.g, GRAY.b);
    doc.text('Patient', col1, rowY);
    doc.setTextColor(0, 0, 0);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(11);
    doc.text(patientName || folder.patientId, col1, rowY + 6);
    
    // Column 1: Created date
    doc.setFontSize(8);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(GRAY.r, GRAY.g, GRAY.b);
    doc.text('Créé le', col1, rowY + 14);
    doc.setTextColor(0, 0, 0);
    doc.setFontSize(9);
    doc.text(formatDate(folder.createdAt), col1, rowY + 20);
    
    // Column 1: Patient ID (bottom, gray)
    doc.setFontSize(7);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(GRAY.r, GRAY.g, GRAY.b);
    doc.text(folder.patientId, col1, rowY + 28);

    // Column 2: N° Dossier
    doc.setFontSize(8);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(GRAY.r, GRAY.g, GRAY.b);
    doc.text('N° Dossier', col2, rowY);
    doc.setTextColor(0, 0, 0);
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(10);
    doc.text(`#${folder.id}`, col2, rowY + 6);
    
    // Column 2: Last update
    doc.setFontSize(8);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(GRAY.r, GRAY.g, GRAY.b);
    doc.text('Dernière mise à jour', col2, rowY + 14);
    doc.setTextColor(0, 0, 0);
    doc.setFontSize(9);
    doc.text(formatDate(folder.updatedAt), col2, rowY + 20);

    // Column 3: Médecin
    doc.setFontSize(8);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(GRAY.r, GRAY.g, GRAY.b);
    doc.text('Médecin', col3, rowY);
    doc.setTextColor(0, 0, 0);
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9);
    doc.text(folder.doctorId, col3, rowY + 6);

    doc.setDrawColor(LINE_GRAY.r, LINE_GRAY.g, LINE_GRAY.b);
    doc.setLineWidth(0.15);
    doc.line(col2 - 8, y, col2 - 8, y + 36);
    doc.line(col3 - 8, y, col3 - 8, y + 36);

    y += 40;
    pushY(4);

    // —— Antécédents médicaux ——
    drawSectionTitle('ANTÉCÉDENTS MÉDICAUX');
    if (history.length === 0) {
      doc.setFontSize(10);
      doc.setFont('helvetica', 'normal');
      doc.setTextColor(GRAY.r, GRAY.g, GRAY.b);
      doc.text('Aucun antécédent enregistré.', MARGIN + 4, y + 5);
      doc.setTextColor(0, 0, 0);
      pushY(12);
    } else {
      history.forEach((h, i) => {
        drawBoxedBlock(`Entrée n°${i + 1}`, [
          ['Allergies', h.allergies ?? ''],
          ['Affections / pathologies', h.conditions ?? ''],
          ['Chirurgies', h.surgeries ?? ''],
        ]);
      });
    }

    // —— Diagnostics ——
    drawSectionTitle('DIAGNOSTICS');
    if (diagnostics.length === 0) {
      doc.setFontSize(10);
      doc.setFont('helvetica', 'normal');
      doc.setTextColor(GRAY.r, GRAY.g, GRAY.b);
      doc.text('Aucun diagnostic enregistré.', MARGIN + 4, y + 5);
      doc.setTextColor(0, 0, 0);
      pushY(12);
    } else {
      diagnostics.forEach((d, i) => {
        drawBoxedBlock(`Diagnostic n°${i + 1} — ${d.diseaseName}`, [
          ['Stade', d.stage ?? ''],
          ['Comorbidités', d.comorbidities ?? ''],
          ['Date du diagnostic', formatDate(d.diagnosisDate) ?? ''],
        ]);
      });
    }

    drawFooter(doc, pageNum);

    return doc.output('blob');
  }

  /**
   * Exports a cross-patient disease analysis report (list of diagnostics matching disease/stage) as PDF.
   */
  exportCrossPatientReport(
    results: Array<{
      patientId: string;
      patientDisplayName?: string | null;
      doctorId: string;
      doctorDisplayName?: string | null;
      medicalFolderId: number;
      diseaseName: string;
      stage: string | null;
      diagnosisDate: string;
    }>,
    title: string
  ): Blob {
    const doc = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });
    const pageW = 297;
    const pageH = 210;
    const M = 14;
    const contentW = pageW - M * 2;
    let y = M;

    doc.setFillColor(245, 247, 250);
    doc.rect(0, 0, pageW, 28, 'F');
    doc.setFontSize(16);
    doc.setFont('helvetica', 'bold');
    doc.text(title, M, 16);
    doc.setFontSize(9);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(120, 120, 120);
    doc.text(`${results.length} result(s)`, M, 23);
    doc.setTextColor(0, 0, 0);
    y = 36;

    const colW = [32, 32, 18, 36, 20, 36];
    const headers = ['Patient', 'Doctor', 'Folder #', 'Disease', 'Stage', 'Diagnosis date'];
    const toStr = (v: unknown) => (v == null || v === '' ? '—' : String(v));
    const formatD = (iso: string) => {
      try {
        return new Date(iso).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' });
      } catch {
        return toStr(iso);
      }
    };

    doc.setFontSize(8);
    doc.setFont('helvetica', 'bold');
    doc.setFillColor(248, 248, 248);
    doc.rect(M, y, contentW, 8, 'F');
    let x = M + 2;
    headers.forEach((h, i) => {
      doc.text(h, x, y + 5.5);
      x += colW[i];
    });
    y += 10;

    doc.setFont('helvetica', 'normal');
    const personCell = (name: string | null | undefined, id: string) => {
      const line1 = name && String(name).trim() ? String(name).trim() : id;
      const line2 = name && String(name).trim() ? id : '';
      return line2 ? `${line1}\n${line2}` : line1;
    };
    for (const r of results) {
      if (y > pageH - 22) {
        doc.addPage('landscape');
        y = M;
      }
      x = M + 2;
      const row = [
        personCell(r.patientDisplayName, r.patientId),
        personCell(r.doctorDisplayName, r.doctorId),
        `#${r.medicalFolderId}`,
        r.diseaseName,
        toStr(r.stage),
        formatD(r.diagnosisDate),
      ];
      let rowH = 6;
      row.forEach((cell, i) => {
        const text = doc.splitTextToSize(toStr(cell), colW[i] - 2);
        doc.text(text, x, y + 4);
        const lines = text.length;
        rowH = Math.max(rowH, lines * 3.6 + 2);
        x += colW[i];
      });
      y += rowH;
    }

    doc.setDrawColor(220, 220, 220);
    doc.setLineWidth(0.2);
    doc.line(M, pageH - 12, pageW - M, pageH - 12);
    doc.setFontSize(8);
    doc.setTextColor(120, 120, 120);
    doc.text('Cross-patient disease analysis · Export PDF', pageW / 2, pageH - 6, { align: 'center' });
    doc.setTextColor(0, 0, 0);

    return doc.output('blob');
  }
}

function formatDate(iso?: string): string {
  if (!iso) return '—';
  try {
    const d = new Date(iso);
    return d.toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: 'long',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return iso;
  }
}
