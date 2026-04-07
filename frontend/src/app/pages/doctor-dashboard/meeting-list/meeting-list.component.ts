import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnInit,
  inject,
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  Meeting,
  MeetingService,
  CreateMeetingRequest,
} from '@/core/services/meeting.service';
import { UserApiService, UserInfo } from '@/core/services/user-api.service';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardIconComponent } from '@/shared/components/icon';
import { environment } from '@/environments/environment';

// ── Type for parsed transcript segment summaries ──────────────────────────────
interface SegmentSummary { label: string; summary: string; }

@Component({
  selector: 'app-meeting-list',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, ZardButtonComponent, ZardIconComponent],
  template: `
    <div class="space-y-6">

      <!-- ═══ HEADER ═══ -->
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-2xl font-bold">📹 Réunions vidéo</h2>
          <p class="text-muted-foreground text-sm mt-1">
            Visioconférences avec les aidants de vos patients
          </p>
        </div>
        <button z-button (click)="showCreateForm = !showCreateForm">
          <z-icon zType="plus" class="mr-1 h-4 w-4" />
          Nouvelle réunion
        </button>
      </div>

      <!-- ═══ FORMULAIRE CRÉATION ═══ -->
      @if (showCreateForm) {
        <div class="bg-card border border-border rounded-xl p-5 space-y-4">
          <h3 class="font-semibold text-sm">Planifier une réunion</h3>
          <div>
            <label class="block text-muted-foreground text-xs mb-1.5">Patient concerné</label>
            <select
              [(ngModel)]="selectedPatientId"
              class="w-full bg-muted text-foreground text-sm rounded-lg px-3 py-2.5 border border-border focus:border-primary focus:outline-none"
            >
              <option value="">-- Sélectionner un patient --</option>
              @for (p of patients; track p.keycloakId) {
                <option [value]="p.keycloakId">{{ p.firstName }} {{ p.lastName }}</option>
              }
            </select>
          </div>
          <div>
            <label class="block text-muted-foreground text-xs mb-1.5">Date et heure (optionnel)</label>
            <input
              type="datetime-local"
              [(ngModel)]="scheduledAt"
              class="w-full bg-muted text-foreground text-sm rounded-lg px-3 py-2.5 border border-border focus:border-primary focus:outline-none"
            />
          </div>
          <div class="flex items-center gap-3 pt-1">
            <button z-button [disabled]="!selectedPatientId || creating" (click)="createMeeting()">
              {{ creating ? 'Création...' : 'Créer la réunion' }}
            </button>
            <button z-button zType="outline" (click)="showCreateForm = false">Annuler</button>
            @if (createError) {
              <span class="text-destructive text-xs">{{ createError }}</span>
            }
          </div>
        </div>
      }

      <!-- ═══ LISTE ═══ -->
      @if (loading) {
        <div class="flex justify-center py-12">
          <div class="w-8 h-8 border-2 border-muted border-t-primary rounded-full animate-spin"></div>
        </div>
      } @else if (meetings.length === 0) {
        <div class="bg-card border border-border rounded-xl p-10 text-center">
          <z-icon zType="video" class="h-12 w-12 mx-auto mb-4 text-muted-foreground opacity-50" />
          <p class="text-muted-foreground text-sm font-medium">Aucune réunion pour le moment.</p>
          <p class="text-muted-foreground/60 text-xs mt-1.5">Créez une réunion vidéo pour commencer.</p>
        </div>
      } @else {
        <div class="space-y-3">
          @for (m of meetings; track m.id) {
            <div class="bg-card border border-border rounded-xl overflow-hidden transition hover:border-primary/30">

              <!-- ── Card header ── -->
              <div class="flex items-start justify-between gap-3 p-4">

                <!-- Info -->
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 mb-1 flex-wrap">
                    <span class="font-medium text-sm">👤 {{ m.patientName }}</span>
                    <span class="text-xs font-medium px-2 py-0.5 rounded-full"
                          [ngClass]="getStatusBadgeClass(m.status)">
                      {{ getStatusLabel(m.status) }}
                    </span>
                    @if (m.durationMinutes) {
                      <span class="text-xs text-muted-foreground">⏱ {{ m.durationMinutes }} min</span>
                    }
                    @if (m.transcript) {
                      <span class="text-xs text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-950/30 px-1.5 py-0.5 rounded">
                        🎙 Transcript
                      </span>
                    }
                    @if (m.transcriptSummaries) {
                      <span class="text-xs text-purple-600 dark:text-purple-400 bg-purple-50 dark:bg-purple-950/30 px-1.5 py-0.5 rounded">
                        🤖 {{ parseSegmentSummaries(m.transcriptSummaries).length }} résumés
                      </span>
                    }
                  </div>
                  <div class="text-xs text-muted-foreground">
                    📅 {{ m.createdAt | date : 'dd/MM/yyyy HH:mm' }}
                    <span class="opacity-40 font-mono ml-2">{{ m.roomName }}</span>
                  </div>
                </div>

                <!-- Actions -->
                <div class="flex items-center gap-1.5 flex-wrap justify-end shrink-0">

                  <!-- Rejoindre -->
                  @if (m.status === 'SCHEDULED' || m.status === 'ACTIVE') {
                    <button z-button zSize="sm"
                      class="bg-emerald-600 hover:bg-emerald-700 text-white"
                      (click)="openMeeting.emit(m)">
                      🎥 Rejoindre
                    </button>
                  }

                  <!-- Expand sections -->
                  @if (m.status === 'ENDED') {

                    <!-- Résumé AI -->
                    <button z-button zType="outline" zSize="sm"
                      (click)="toggleSection(m.id, 'summary')">
                      @if (isOpen(m.id, 'summary')) { ▲ Résumé } @else { 🤖 Résumé AI }
                    </button>

                    <!-- Notes -->
                    @if (m.notes) {
                      <button z-button zType="outline" zSize="sm"
                        (click)="toggleSection(m.id, 'notes')">
                        @if (isOpen(m.id, 'notes')) { ▲ Notes } @else { 📋 Notes }
                      </button>
                    }

                    <!-- Transcript -->
                    @if (m.transcript) {
                      <button z-button zType="outline" zSize="sm"
                        (click)="toggleSection(m.id, 'transcript')">
                        @if (isOpen(m.id, 'transcript')) { ▲ Transcript } @else { 🎙 Transcript }
                      </button>
                    }

                    <!-- ⬇️ PDF (backend) -->
                    <button z-button zSize="sm"
                      class="bg-indigo-600 hover:bg-indigo-700 text-white"
                      (click)="downloadPdf(m)"
                      [disabled]="pdfLoadingIds.has(m.id)"
                      title="Télécharger le rapport PDF complet">
                      @if (pdfLoadingIds.has(m.id)) { ⏳ PDF... } @else { ⬇️ PDF }
                    </button>
                  }

                  <!-- Supprimer -->
                  @if (confirmDeleteId === m.id) {
                    <span class="text-xs text-red-500 font-medium">Confirmer ?</span>
                    <button z-button zSize="sm"
                      class="bg-red-600 hover:bg-red-700 text-white"
                      (click)="deleteMeeting(m.id)">✓ Oui</button>
                    <button z-button zType="outline" zSize="sm"
                      (click)="confirmDeleteId = null">✕</button>
                  } @else {
                    <button z-button zType="outline" zSize="sm"
                      class="text-red-500 border-red-200 hover:bg-red-50 dark:hover:bg-red-950"
                      (click)="confirmDeleteId = m.id"
                      [disabled]="deletingIds.has(m.id)"
                      title="Supprimer la réunion">
                      @if (deletingIds.has(m.id)) { ⏳ } @else { 🗑️ }
                    </button>
                  }
                </div>
              </div>

              <!-- ══ EXPANDABLE SECTIONS ══ -->
              @if (m.status === 'ENDED') {

                <!-- ── AI SUMMARY ── -->
                @if (isOpen(m.id, 'summary')) {
                  <div class="border-t border-border px-4 py-3 bg-emerald-50/40 dark:bg-emerald-950/10">
                    <div class="flex items-center justify-between mb-2">
                      <span class="text-emerald-700 dark:text-emerald-400 text-xs font-bold">🤖 Résumé AI Final (Groq)</span>
                      <div class="flex gap-1.5">
                        <button z-button zType="outline" zSize="sm" (click)="copySummary(m.aiSummary, m.id)">
                          {{ copyTexts[m.id] || '📋 Copier' }}
                        </button>
                        <button z-button zSize="sm"
                          class="bg-indigo-600 hover:bg-indigo-700 text-white"
                          (click)="downloadPdf(m)">⬇️ PDF</button>
                      </div>
                    </div>
                    @if (m.aiSummary) {
                      @for (section of parseSummary(m.aiSummary); track section.title) {
                        @if (section.title) {
                          <h4 class="text-emerald-600 dark:text-emerald-400 font-semibold text-xs mt-3 mb-1 first:mt-0">
                            {{ section.title }}
                          </h4>
                        }
                        <p class="text-sm leading-relaxed text-muted-foreground whitespace-pre-wrap">{{ section.content }}</p>
                      }
                    } @else {
                      <p class="text-xs text-muted-foreground italic">Aucun résumé AI disponible.</p>
                      @if (m.notes || m.transcript) {
                        <button z-button zType="outline" zSize="sm"
                          class="mt-2 text-orange-600"
                          (click)="regenerateSummary(m)"
                          [disabled]="regeneratingIds.has(m.id)">
                          @if (regeneratingIds.has(m.id)) { ⏳ Génération... } @else { ✨ Générer résumé AI }
                        </button>
                      }
                    }
                  </div>
                }

                <!-- ── NOTES ── -->
                @if (isOpen(m.id, 'notes') && m.notes) {
                  <div class="border-t border-border px-4 py-3 bg-blue-50/30 dark:bg-blue-950/10">
                    <div class="flex items-center justify-between mb-2">
                      <span class="text-blue-700 dark:text-blue-400 text-xs font-bold">📋 Notes du médecin</span>
                      <button z-button zType="outline" zSize="sm" (click)="copyNotes(m.notes, m.id)">
                        {{ copyNotesTexts[m.id] || '📋 Copier' }}
                      </button>
                    </div>
                    <pre class="text-sm leading-relaxed text-foreground/80 whitespace-pre-wrap font-sans bg-background/60 rounded p-3 border border-border max-h-60 overflow-y-auto">{{ m.notes }}</pre>
                  </div>
                }

                <!-- ── TRANSCRIPT ── -->
                @if (isOpen(m.id, 'transcript') && m.transcript) {
                  <div class="border-t border-border px-4 py-3 bg-purple-50/30 dark:bg-purple-950/10">
                    <div class="flex items-center justify-between mb-2">
                      <span class="text-purple-700 dark:text-purple-400 text-xs font-bold">🎙️ Transcription en direct</span>
                      <button z-button zType="outline" zSize="sm" (click)="copyTranscript(m.transcript, m.id)">
                        {{ copyTranscriptTexts[m.id] || '📋 Copier' }}
                      </button>
                    </div>

                    <!-- Segment mini-summaries (if any) -->
                    @if (m.transcriptSummaries) {
                      <div class="mb-3 space-y-2">
                        <p class="text-purple-600 dark:text-purple-400 text-xs font-semibold mb-1.5">
                          🤖 Résumés périodiques Groq ({{ parseSegmentSummaries(m.transcriptSummaries).length }} segments)
                        </p>
                        @for (seg of parseSegmentSummaries(m.transcriptSummaries); track seg.label) {
                          <div class="rounded-lg overflow-hidden border border-purple-200 dark:border-purple-800">
                            <div class="bg-purple-100 dark:bg-purple-950/40 px-3 py-1.5 text-purple-700 dark:text-purple-300 text-xs font-semibold">
                              {{ seg.label }}
                            </div>
                            <div class="bg-white/50 dark:bg-background/30 px-3 py-2 text-xs text-muted-foreground leading-relaxed">
                              {{ seg.summary }}
                            </div>
                          </div>
                        }
                      </div>
                    }

                    <!-- Raw transcript -->
                    <pre class="text-xs leading-relaxed text-foreground/70 whitespace-pre-wrap font-sans bg-background/60 rounded p-3 border border-border max-h-72 overflow-y-auto">{{ m.transcript }}</pre>
                  </div>
                }

              }
            </div>
          }
        </div>
      }
    </div>
  `,
})
export class MeetingListComponent implements OnInit {
  @Input() doctorKeycloakId!: string;
  @Output() openMeeting = new EventEmitter<Meeting>();

  private meetingService = inject(MeetingService);
  private userApiService = inject(UserApiService);

  meetings: Meeting[] = [];
  patients: UserInfo[] = [];
  loading = true;

  showCreateForm = false;
  selectedPatientId = '';
  scheduledAt = '';
  creating = false;
  createError = '';

  // ── Open sections (per meeting id) ────────────────────────────────────────
  /** Map of meetingId → set of open section names */
  private openSections = new Map<number, Set<string>>();

  isOpen(id: number, section: string): boolean {
    return this.openSections.get(id)?.has(section) ?? false;
  }

  toggleSection(id: number, section: string): void {
    if (!this.openSections.has(id)) this.openSections.set(id, new Set());
    const s = this.openSections.get(id)!;
    s.has(section) ? s.delete(section) : s.add(section);
  }

  // ── Copy / Download buttons ────────────────────────────────────────────────
  copyTexts: Record<number, string>          = {};
  copyNotesTexts: Record<number, string>     = {};
  copyTranscriptTexts: Record<number, string> = {};
  pdfLoadingIds = new Set<number>();
  regeneratingIds = new Set<number>();
  confirmDeleteId: number | null = null;
  deletingIds = new Set<number>();

  ngOnInit(): void {
    this.loadMeetings();
    this.loadPatients();
  }

  loadMeetings(): void {
    this.loading = true;
    this.meetingService.getMeetingsForDoctor(this.doctorKeycloakId).subscribe({
      next: (meetings) => { this.meetings = meetings; this.loading = false; },
      error: () => { this.loading = false; },
    });
  }

  loadPatients(): void {
    this.userApiService.getUsersByRole('patient').subscribe({
      next: (p) => { this.patients = p; },
      error: (err) => { console.warn('Could not load patients:', err); },
    });
  }

  createMeeting(): void {
    if (!this.selectedPatientId || this.creating) return;
    this.creating = true;
    this.createError = '';
    const req: CreateMeetingRequest = {
      patientKeycloakId: this.selectedPatientId,
      doctorKeycloakId: this.doctorKeycloakId,
      scheduledAt: this.scheduledAt || undefined,
    };
    this.meetingService.createMeeting(req).subscribe({
      next: (m) => {
        this.creating = false;
        this.showCreateForm = false;
        this.selectedPatientId = '';
        this.scheduledAt = '';
        this.meetings.unshift(m);
        this.openMeeting.emit(m);
      },
      error: () => {
        this.creating = false;
        this.createError = 'Erreur lors de la création de la réunion.';
      },
    });
  }

  // ── PDF (backend) ──────────────────────────────────────────────────────────

  downloadPdf(m: Meeting): void {
    this.pdfLoadingIds.add(m.id);
    // Use backend endpoint — direct link with timeout feedback
    try {
      this.meetingService.downloadMeetingPdf(m.id, m.patientName);
    } finally {
      setTimeout(() => this.pdfLoadingIds.delete(m.id), 3000);
    }
  }

  // ── Delete ─────────────────────────────────────────────────────────────────

  deleteMeeting(id: number): void {
    this.confirmDeleteId = null;
    this.deletingIds.add(id);
    this.meetingService.deleteMeeting(id).subscribe({
      next: () => {
        this.deletingIds.delete(id);
        this.meetings = this.meetings.filter(m => m.id !== id);
      },
      error: () => {
        this.deletingIds.delete(id);
        alert('Erreur lors de la suppression.');
      },
    });
  }

  // ── Summary ────────────────────────────────────────────────────────────────

  copySummary(summary: string, id: number): void {
    navigator.clipboard.writeText(summary || '').then(() => {
      this.copyTexts[id] = '✅ Copié !';
      setTimeout(() => delete this.copyTexts[id], 2000);
    });
  }

  copyNotes(notes: string, id: number): void {
    navigator.clipboard.writeText(notes || '').then(() => {
      this.copyNotesTexts[id] = '✅ Copié !';
      setTimeout(() => delete this.copyNotesTexts[id], 2000);
    });
  }

  copyTranscript(transcript: string, id: number): void {
    navigator.clipboard.writeText(transcript || '').then(() => {
      this.copyTranscriptTexts[id] = '✅ Copié !';
      setTimeout(() => delete this.copyTranscriptTexts[id], 2000);
    });
  }

  regenerateSummary(m: Meeting): void {
    if (this.regeneratingIds.has(m.id)) return;
    this.regeneratingIds.add(m.id);
    this.meetingService.regenerateSummary(m.id).subscribe({
      next: (result) => {
        this.regeneratingIds.delete(m.id);
        this.meetings = this.meetings.map(x =>
          x.id === m.id ? { ...x, aiSummary: result.summary } : x
        );
      },
      error: () => {
        this.regeneratingIds.delete(m.id);
        alert('Impossible de générer le résumé AI.');
      },
    });
  }

  // ── Parsers ────────────────────────────────────────────────────────────────

  parseSummary(text: string): { title: string; content: string }[] {
    if (!text) return [];
    const sections: { title: string; content: string }[] = [];
    let currentTitle = '';
    let currentContent: string[] = [];
    for (const line of text.split('\n')) {
      const match = line.match(/^##\s+(.+)/);
      if (match) {
        if (currentContent.length || currentTitle)
          sections.push({ title: currentTitle, content: currentContent.join('\n').trim() });
        currentTitle   = match[1].trim();
        currentContent = [];
      } else {
        currentContent.push(line);
      }
    }
    if (currentContent.length || currentTitle)
      sections.push({ title: currentTitle, content: currentContent.join('\n').trim() });
    return sections.length ? sections : [{ title: '', content: text }];
  }

  parseSegmentSummaries(json: string): SegmentSummary[] {
    if (!json) return [];
    try {
      return JSON.parse(json) as SegmentSummary[];
    } catch {
      // Fallback manual parse
      const results: SegmentSummary[] = [];
      const inner = json.replace(/^\[|\]$/g, '');
      const entries = inner.split(/\},\s*\{/);
      for (const entry of entries) {
        const label   = this.extractField(entry, 'label');
        const summary = this.extractField(entry, 'summary');
        if (label && summary) results.push({ label, summary });
      }
      return results;
    }
  }

  private extractField(json: string, field: string): string | null {
    const key = `"${field}":"`;
    const start = json.indexOf(key);
    if (start < 0) return null;
    let i = start + key.length;
    let result = '';
    while (i < json.length) {
      const ch = json[i];
      if (ch === '"' && json[i - 1] !== '\\') break;
      result += ch;
      i++;
    }
    return result.replace(/\\"/g, '"').replace(/\\n/g, '\n').replace(/\\\\/g, '\\');
  }

  // ── Utils ──────────────────────────────────────────────────────────────────

  getStatusLabel(status: string): string {
    switch (status) {
      case 'SCHEDULED': return 'Planifiée';
      case 'ACTIVE':    return 'En cours';
      case 'ENDED':     return 'Terminée';
      default:          return status;
    }
  }

  getStatusBadgeClass(status: string): Record<string, boolean> {
    return {
      'bg-muted text-muted-foreground':
        status === 'SCHEDULED',
      'bg-emerald-100 text-emerald-800 dark:bg-emerald-900 dark:text-emerald-300':
        status === 'ACTIVE',
      'bg-red-100 text-red-700 dark:bg-red-900 dark:text-red-300':
        status === 'ENDED',
    };
  }
}
