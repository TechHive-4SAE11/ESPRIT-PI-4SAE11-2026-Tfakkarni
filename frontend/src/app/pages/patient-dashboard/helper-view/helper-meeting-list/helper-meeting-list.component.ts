import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Meeting, MeetingService } from '@/core/services/meeting.service';

@Component({
  selector: 'app-helper-meeting-list',
  standalone: true,
  imports: [CommonModule, DatePipe],
  template: `
    <div class="space-y-6">
      <!-- ═══ HEADER ═══ -->
      <div>
        <h2 class="text-2xl font-bold">📹 Réunions vidéo</h2>
        <p class="text-muted-foreground text-sm mt-1">
          Vos visioconférences planifiées avec le médecin
        </p>
      </div>

      <!-- ═══ MEETINGS LIST ═══ -->
      @if (loading()) {
        <div class="flex justify-center py-12">
          <div class="w-8 h-8 border-2 border-muted border-t-emerald-400 rounded-full animate-spin"></div>
        </div>
      } @else if (meetings().length === 0) {
        <div class="bg-card border border-border rounded-xl p-10 text-center">
          <div class="text-5xl mb-4">📹</div>
          <p class="text-muted-foreground text-sm font-medium">
            Aucune réunion planifiée pour le moment.
          </p>
          <p class="text-muted-foreground/60 text-xs mt-1.5">
            Votre médecin planifiera des réunions vidéo quand nécessaire.
          </p>
        </div>
      } @else {
        <div class="space-y-3">
          @for (m of meetings(); track m.id) {
            <div class="bg-card border border-border rounded-xl p-4 hover:border-primary/30 transition">
              <div class="flex items-center justify-between">
                <!-- Info -->
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 mb-1">
                    <span class="font-medium text-sm truncate">
                      🩺 Dr. {{ m.doctorName }}
                    </span>
                    <span
                      class="text-xs font-medium px-2 py-0.5 rounded-full"
                      [ngClass]="getStatusBadgeClass(m.status)"
                    >
                      {{ getStatusLabel(m.status) }}
                    </span>
                  </div>
                  <div class="flex items-center gap-4 text-xs text-muted-foreground">
                    <span>📅 {{ m.scheduledAt | date : 'dd/MM/yyyy HH:mm' }}</span>
                    @if (m.durationMinutes) {
                      <span>⏱ {{ m.durationMinutes }} min</span>
                    }
                  </div>
                </div>

                <!-- Actions -->
                <div class="flex items-center gap-2 ml-3">
                  @if (m.status === 'SCHEDULED' || m.status === 'ACTIVE') {
                    <button
                      (click)="joinMeeting.emit(m)"
                      class="bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-medium px-4 py-2 rounded-lg transition"
                    >
                      🎥 Rejoindre
                    </button>
                  }
                  <!-- Supprimer -->
                  @if (confirmDeleteId === m.id) {
                    <span class="text-xs text-red-500 font-medium">Confirmer ?</span>
                    <button
                      (click)="deleteMeeting(m.id)"
                      class="bg-red-600 hover:bg-red-700 text-white text-xs font-medium px-3 py-2 rounded-lg transition"
                    >
                      ✓ Oui
                    </button>
                    <button
                      (click)="confirmDeleteId = null"
                      class="bg-muted hover:bg-muted/80 text-foreground text-xs font-medium px-3 py-2 rounded-lg transition border border-border"
                    >
                      ✕
                    </button>
                  } @else {
                    <button
                      (click)="confirmDeleteId = m.id"
                      [disabled]="deletingIds.has(m.id)"
                      class="bg-muted hover:bg-red-50 dark:hover:bg-red-950 text-red-500 text-xs font-medium px-3 py-2 rounded-lg transition border border-red-200 dark:border-red-800"
                      title="Supprimer"
                    >
                      @if (deletingIds.has(m.id)) { ⏳ } @else { 🗑️ }
                    </button>
                  }
                  @if (m.status === 'ENDED') {
                    <div class="flex gap-2 flex-wrap justify-end">
                      <!-- Résumé AI -->
                      <button
                        (click)="toggleSummary(m.id)"
                        class="bg-muted hover:bg-muted/80 text-foreground text-xs font-medium px-3 py-2 rounded-lg transition border border-border"
                      >
                        @if (expandedSummaryId === m.id) {
                          ▲ Masquer
                        } @else {
                          🤖 Résumé AI
                        }
                      </button>
                      <!-- Voir Notes -->
                      @if (m.notes) {
                        <button
                          (click)="openNotesModal(m)"
                          class="bg-blue-600 hover:bg-blue-700 text-white text-xs font-medium px-3 py-2 rounded-lg transition"
                        >
                          📋 Notes
                        </button>
                      }
                      <!-- Régénérer le résumé (si résumé absent ou en erreur) -->
                      @if (!m.aiSummary || m.aiSummary.includes('non disponible') || m.aiSummary.includes('erreur')) {
                        <button
                          (click)="regenerateSummary(m)"
                          [disabled]="regeneratingIds.has(m.id)"
                          class="bg-orange-600 hover:bg-orange-700 disabled:opacity-60 text-white text-xs font-medium px-3 py-2 rounded-lg transition"
                        >
                          @if (regeneratingIds.has(m.id)) {
                            ⏳ Génération...
                          } @else {
                            ✨ Générer résumé AI
                          }
                        </button>
                      }
                      <!-- Télécharger PDF -->
                      <button
                        (click)="downloadPdf(m)"
                        class="bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-medium px-3 py-2 rounded-lg transition"
                        title="Télécharger PDF"
                      >
                        ⬇️ PDF
                      </button>
                    </div>
                  }
                </div>
              </div>

              <!-- Expanded AI Summary -->
              @if (expandedSummaryId === m.id) {
                @if (m.aiSummary) {
                  <div class="mt-3 pt-3 border-t border-border">
                    <!-- Summary sections parsed -->
                    @for (section of parseSummary(m.aiSummary); track section.title) {
                      @if (section.title) {
                        <h4 class="text-emerald-600 dark:text-emerald-400 font-semibold text-xs mt-3 mb-1 first:mt-0">
                          {{ section.title }}
                        </h4>
                      }
                      <p class="text-sm leading-relaxed text-muted-foreground whitespace-pre-wrap">{{ section.content }}</p>
                    }
                    <button
                      (click)="copySummary(m.aiSummary, m.id)"
                      class="mt-3 bg-muted hover:bg-muted/80 text-foreground text-xs px-3 py-1.5 rounded-lg border border-border transition"
                    >
                      {{ copyTexts[m.id] || '📋 Copier le résumé' }}
                    </button>
                  </div>
                } @else {
                  <div class="mt-3 pt-3 border-t border-border text-xs text-muted-foreground italic">
                    Aucun résumé disponible.
                  </div>
                }
              }
            </div>
          }
        </div>
      }
    </div>

    <!-- ═══ MODAL NOTES COMPLÈTES ═══ -->
    @if (selectedNotesMeeting) {
      <div
        class="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4"
        (click)="selectedNotesMeeting = null"
      >
        <div
          class="bg-background border border-border rounded-2xl shadow-2xl w-full max-w-2xl max-h-[80vh] flex flex-col"
          (click)="$event.stopPropagation()"
        >
          <!-- Modal Header -->
          <div class="flex items-center justify-between px-5 py-4 border-b border-border">
            <div>
              <h2 class="font-bold text-base">📋 Notes complètes de la réunion</h2>
              <p class="text-muted-foreground text-xs mt-0.5">
                Dr. {{ selectedNotesMeeting.doctorName }}
                — {{ selectedNotesMeeting.scheduledAt | date:'dd/MM/yyyy' }}
                @if (selectedNotesMeeting.durationMinutes) {
                  — {{ selectedNotesMeeting.durationMinutes }} min
                }
              </p>
            </div>
            <div class="flex gap-2">
              <button
                (click)="copyModalNotes()"
                class="bg-muted hover:bg-muted/80 text-foreground text-xs px-3 py-1.5 rounded-lg border border-border transition"
              >
                {{ copyModalNotesText }}
              </button>
              <button
                (click)="selectedNotesMeeting = null"
                class="text-muted-foreground hover:text-foreground text-sm px-2 py-1.5 rounded-lg hover:bg-muted transition"
              >
                ✕
              </button>
            </div>
          </div>
          <!-- Modal Body -->
          <div class="flex-1 overflow-y-auto p-5">
            <pre class="text-sm leading-relaxed whitespace-pre-wrap font-sans">{{ selectedNotesMeeting.notes }}</pre>
          </div>
        </div>
      </div>
    }
  `,
})
export class HelperMeetingListComponent implements OnInit {
  @Input() patientKeycloakId!: string;
  @Output() joinMeeting = new EventEmitter<Meeting>();

  private meetingService = inject(MeetingService);

  meetings = signal<Meeting[]>([]);
  loading = signal(true);

  // Summary panel
  expandedSummaryId: number | null = null;
  copyTexts: Record<number, string> = {};
  regeneratingIds = new Set<number>();

  // Delete
  confirmDeleteId: number | null = null;
  deletingIds = new Set<number>();

  // Notes modal
  selectedNotesMeeting: Meeting | null = null;
  copyModalNotesText = '📋 Copier';

  @Input() currentUserKeycloakId = ''; // helper's keycloakId for token pre-fetch
  @Input() currentUserName = 'Aidant';

  ngOnInit(): void {
    this.loadMeetings();
  }

  loadMeetings(): void {
    this.loading.set(true);
    this.meetingService.getMeetingsForPatient(this.patientKeycloakId).subscribe({
      next: (meetings) => {
        this.meetings.set(meetings);
        this.loading.set(false);
        // Pre-fetch tokens for ACTIVE meetings in background
        // so join is near-instant when user clicks
        if (this.currentUserKeycloakId) {
          meetings
            .filter(m => m.status === 'ACTIVE' || m.status === 'SCHEDULED')
            .forEach(m => {
              this.meetingService.prefetchToken(
                m.id,
                this.currentUserKeycloakId,
                this.currentUserName
              );
            });
        }
      },
      error: () => this.loading.set(false),
    });
  }

  toggleSummary(id: number): void {
    this.expandedSummaryId = this.expandedSummaryId === id ? null : id;
  }

  openNotesModal(m: Meeting): void {
    this.selectedNotesMeeting = m;
    this.copyModalNotesText = '📋 Copier';
  }

  copyModalNotes(): void {
    if (!this.selectedNotesMeeting?.notes) return;
    navigator.clipboard.writeText(this.selectedNotesMeeting.notes).then(() => {
      this.copyModalNotesText = '✅ Copié !';
      setTimeout(() => (this.copyModalNotesText = '📋 Copier'), 2000);
    });
  }

  deleteMeeting(id: number): void {
    this.confirmDeleteId = null;
    this.deletingIds.add(id);
    this.meetingService.deleteMeeting(id).subscribe({
      next: () => {
        this.deletingIds.delete(id);
        this.meetings.update(list => list.filter(m => m.id !== id));
      },
      error: () => {
        this.deletingIds.delete(id);
        alert('Erreur lors de la suppression.');
      },
    });
  }

  downloadPdf(m: Meeting): void {
    const date = m.scheduledAt
      ? new Date(m.scheduledAt).toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric' })
      : '';
    const duration = m.durationMinutes ? `${m.durationMinutes} minutes` : 'N/A';

    const summaryHtml = m.aiSummary
      ? this.parseSummary(m.aiSummary).map(s =>
          `${s.title ? `<h3 style="color:#059669;font-size:13px;margin:14px 0 6px">${this.esc(s.title)}</h3>` : ''}
           <p style="font-size:13px;line-height:1.7;color:#374151;white-space:pre-wrap;margin:0">${this.esc(s.content)}</p>`
        ).join('')
      : '<p style="color:#6b7280;font-style:italic">Aucun résumé disponible.</p>';

    const html = `<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Réunion médicale — ${this.esc(m.patientName)}</title>
  <style>
    body{font-family:'Segoe UI',Arial,sans-serif;color:#1f2937;margin:0;padding:40px;background:#fff}
    .header{background:linear-gradient(135deg,#059669,#047857);color:#fff;padding:28px 32px;border-radius:12px;margin-bottom:28px}
    .header h1{margin:0 0 6px;font-size:22px;font-weight:700}
    .header p{margin:0;opacity:.85;font-size:13px}
    .meta{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:28px}
    .meta-item{background:#f9fafb;border:1px solid #e5e7eb;border-radius:8px;padding:14px 16px}
    .meta-item .label{font-size:11px;color:#6b7280;text-transform:uppercase;letter-spacing:.05em;margin-bottom:4px}
    .meta-item .value{font-size:14px;font-weight:600;color:#111827}
    .section{margin-bottom:24px}
    .section h2{font-size:15px;font-weight:700;color:#059669;border-bottom:2px solid #d1fae5;padding-bottom:8px;margin-bottom:14px}
    .notes-box{background:#f9fafb;border:1px solid #e5e7eb;border-radius:8px;padding:16px;white-space:pre-wrap;font-size:13px;line-height:1.7;color:#374151}
    .footer{margin-top:36px;padding-top:16px;border-top:1px solid #e5e7eb;text-align:center;color:#9ca3af;font-size:11px}
  </style>
</head>
<body>
  <div class="header">
    <h1>📋 Rapport de réunion médicale</h1>
    <p>Plateforme Tfakkarni – Suivi des patients Alzheimer</p>
  </div>
  <div class="meta">
    <div class="meta-item"><div class="label">Patient</div><div class="value">${this.esc(m.patientName)}</div></div>
    <div class="meta-item"><div class="label">Médecin</div><div class="value">Dr. ${this.esc(m.doctorName)}</div></div>
    <div class="meta-item"><div class="label">Date</div><div class="value">${date}</div></div>
    <div class="meta-item"><div class="label">Durée</div><div class="value">${duration}</div></div>
  </div>
  ${m.notes ? `<div class="section"><h2>📝 Notes de la réunion</h2><div class="notes-box">${this.esc(m.notes)}</div></div>` : ''}
  <div class="section"><h2>🤖 Résumé AI</h2>${summaryHtml}</div>
  <div class="footer">Généré le ${new Date().toLocaleDateString('fr-FR')} à ${new Date().toLocaleTimeString('fr-FR')} — Tfakkarni © 2026</div>
</body>
</html>`;

    const win = window.open('', '_blank', 'width=900,height=700');
    if (win) { win.document.write(html); win.document.close(); setTimeout(() => { win.focus(); win.print(); }, 500); }
  }

  private esc(s: string | null | undefined): string {
    if (!s) return '';
    return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
  }

  copySummary(summary: string, meetingId: number): void {
    navigator.clipboard.writeText(summary).then(() => {
      this.copyTexts[meetingId] = '✅ Copié !';
      setTimeout(() => delete this.copyTexts[meetingId], 2000);
    });
  }

  regenerateSummary(meeting: Meeting): void {
    if (this.regeneratingIds.has(meeting.id)) return;
    this.regeneratingIds.add(meeting.id);

    this.meetingService.regenerateSummary(meeting.id).subscribe({
      next: (result) => {
        this.regeneratingIds.delete(meeting.id);
        // Update the meeting in the list
        this.meetings.update(list =>
          list.map(m => m.id === meeting.id ? { ...m, aiSummary: result.summary } : m)
        );
      },
      error: () => {
        this.regeneratingIds.delete(meeting.id);
        alert('Impossible de régénérer le résumé. Vérifiez la clé API Claude.');
      },
    });
  }

  /** Parse AI summary markdown sections for display */
  parseSummary(text: string): { title: string; content: string }[] {
    const sections: { title: string; content: string }[] = [];
    const lines = text.split('\n');
    let currentTitle = '';
    let currentContent: string[] = [];

    for (const line of lines) {
      const headingMatch = line.match(/^##\s+(.+)/);
      if (headingMatch) {
        if (currentContent.length > 0 || currentTitle) {
          sections.push({ title: currentTitle, content: currentContent.join('\n').trim() });
        }
        currentTitle = headingMatch[1].trim();
        currentContent = [];
      } else {
        currentContent.push(line);
      }
    }
    if (currentContent.length > 0 || currentTitle) {
      sections.push({ title: currentTitle, content: currentContent.join('\n').trim() });
    }
    return sections.length > 0 ? sections : [{ title: '', content: text }];
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'SCHEDULED': return 'Planifiée';
      case 'ACTIVE': return 'En cours';
      case 'ENDED': return 'Terminée';
      default: return status;
    }
  }

  getStatusBadgeClass(status: string): Record<string, boolean> {
    return {
      'bg-gray-200 text-gray-700 dark:bg-gray-700 dark:text-gray-300': status === 'SCHEDULED',
      'bg-emerald-100 text-emerald-800 dark:bg-emerald-900 dark:text-emerald-300': status === 'ACTIVE',
      'bg-red-100 text-red-700 dark:bg-red-900 dark:text-red-300': status === 'ENDED',
    };
  }
}
