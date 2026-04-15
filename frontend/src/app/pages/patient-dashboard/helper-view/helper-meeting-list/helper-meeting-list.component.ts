import {
  Component, Input, Output, EventEmitter, OnInit, inject, signal,
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Meeting, MeetingService } from '@/core/services/meeting.service';
import { RatingService } from '@/core/services/rating.service';
import { MeetingRatingModalComponent } from '@/pages/doctor-dashboard/meeting-room/meeting-rating-modal.component';

@Component({
  selector: 'app-helper-meeting-list',
  standalone: true,
  imports: [CommonModule, DatePipe, MeetingRatingModalComponent],
  template: `
    <div class="space-y-6">
      <div>
        <h2 class="text-2xl font-bold">📹 Réunions vidéo</h2>
        <p class="text-muted-foreground text-sm mt-1">Vos visioconférences planifiées avec le médecin</p>
      </div>

      @if (loading()) {
        <div class="flex justify-center py-12">
          <div class="w-8 h-8 border-2 border-muted border-t-emerald-400 rounded-full animate-spin"></div>
        </div>
      } @else if (meetings().length === 0) {
        <div class="bg-card border border-border rounded-xl p-10 text-center">
          <div class="text-5xl mb-4">📹</div>
          <p class="text-muted-foreground text-sm font-medium">Aucune réunion planifiée pour le moment.</p>
          <p class="text-muted-foreground/60 text-xs mt-1.5">Votre médecin planifiera des réunions vidéo quand nécessaire.</p>
        </div>
      } @else {
        <div class="space-y-3">
          @for (m of meetings(); track m.id) {
            <div class="bg-card border border-border rounded-xl overflow-hidden hover:border-primary/30 transition">

              <!-- ── Main row ── -->
              <div class="flex items-center justify-between p-4 gap-3">
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 mb-1 flex-wrap">
                    <span class="font-medium text-sm">🩺 Dr. {{ m.doctorName }}</span>
                    <span class="text-xs font-medium px-2 py-0.5 rounded-full" [ngClass]="getStatusBadgeClass(m.status)">
                      {{ getStatusLabel(m.status) }}
                    </span>
                    @if (m.durationMinutes) {
                      <span class="text-xs text-muted-foreground">⏱ {{ m.durationMinutes }} min</span>
                    }
                    <!-- Rating badge if already rated -->
                    @if (ratedMeetingIds.has(m.id)) {
                      <span class="text-xs text-yellow-600 dark:text-yellow-400 bg-yellow-50 dark:bg-yellow-950/30 px-2 py-0.5 rounded-full">
                        ⭐ Évalué
                      </span>
                    }
                  </div>
                  <div class="text-xs text-muted-foreground">
                    📅 {{ m.scheduledAt | date:'dd/MM/yyyy HH:mm' }}
                    @if (m.createdAt) { <span class="ml-2 opacity-40 font-mono">{{ m.roomName }}</span> }
                  </div>
                </div>

                <!-- Actions -->
                <div class="flex items-center gap-1.5 flex-wrap justify-end shrink-0">

                  <!-- Rejoindre (SCHEDULED/ACTIVE) -->
                  @if (m.status === 'SCHEDULED' || m.status === 'ACTIVE') {
                    <button (click)="joinMeeting.emit(m)"
                      class="bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold px-4 py-2 rounded-lg transition">
                      🎥 Rejoindre
                    </button>
                  }

                  @if (m.status === 'ENDED') {
                    <!-- ⭐ Évaluer le médecin -->
                    @if (!ratedMeetingIds.has(m.id)) {
                      <button (click)="openRatingModal(m)"
                        class="bg-yellow-500 hover:bg-yellow-600 text-white text-xs font-semibold px-3 py-2 rounded-lg transition flex items-center gap-1">
                        ⭐ Évaluer
                      </button>
                    } @else {
                      <span class="text-xs text-yellow-600 dark:text-yellow-400 bg-yellow-50 dark:bg-yellow-950/30 px-3 py-2 rounded-lg border border-yellow-200 dark:border-yellow-800">
                        ✅ Évaluation envoyée
                      </span>
                    }

                    <!-- Résumé AI -->
                    <button (click)="toggleSummary(m.id)"
                      class="bg-muted hover:bg-muted/80 text-foreground text-xs font-medium px-3 py-2 rounded-lg transition border border-border">
                      @if (expandedSummaryId === m.id) { ▲ Masquer } @else { 🤖 Résumé }
                    </button>

                    <!-- Notes -->
                    @if (m.notes) {
                      <button (click)="openNotesModal(m)"
                        class="bg-blue-600 hover:bg-blue-700 text-white text-xs font-medium px-3 py-2 rounded-lg transition">
                        📋 Notes
                      </button>
                    }

                    <!-- PDF -->
                    <button (click)="downloadPdfBackend(m)"
                      class="bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-medium px-3 py-2 rounded-lg transition"
                      title="Télécharger rapport PDF">
                      ⬇️ PDF
                    </button>
                  }

                  <!-- Delete -->
                  @if (confirmDeleteId === m.id) {
                    <span class="text-xs text-red-500 font-medium">Confirmer ?</span>
                    <button (click)="deleteMeeting(m.id)"
                      class="bg-red-600 hover:bg-red-700 text-white text-xs px-3 py-2 rounded-lg transition">✓</button>
                    <button (click)="confirmDeleteId = null"
                      class="bg-muted text-foreground text-xs px-3 py-2 rounded-lg border border-border transition">✕</button>
                  } @else {
                    <button (click)="confirmDeleteId = m.id" [disabled]="deletingIds.has(m.id)"
                      class="bg-muted hover:bg-red-50 dark:hover:bg-red-950 text-red-500 text-xs px-2 py-2 rounded-lg border border-red-200 dark:border-red-800 transition">
                      @if (deletingIds.has(m.id)) { ⏳ } @else { 🗑️ }
                    </button>
                  }
                </div>
              </div>

              <!-- ── Expanded AI Summary ── -->
              @if (expandedSummaryId === m.id) {
                <div class="border-t border-border px-4 py-3 bg-emerald-50/30 dark:bg-emerald-950/10">
                  @if (m.aiSummary) {
                    @for (section of parseSummary(m.aiSummary); track section.title) {
                      @if (section.title) {
                        <h4 class="text-emerald-600 dark:text-emerald-400 font-semibold text-xs mt-3 mb-1 first:mt-0">{{ section.title }}</h4>
                      }
                      <p class="text-sm leading-relaxed text-muted-foreground whitespace-pre-wrap">{{ section.content }}</p>
                    }
                    <button (click)="copySummary(m.aiSummary, m.id)"
                      class="mt-3 bg-muted hover:bg-muted/80 text-foreground text-xs px-3 py-1.5 rounded-lg border border-border transition">
                      {{ copyTexts[m.id] || '📋 Copier' }}
                    </button>
                  } @else {
                    <p class="text-xs text-muted-foreground italic">Aucun résumé disponible.</p>
                    <button (click)="regenerateSummary(m)" [disabled]="regeneratingIds.has(m.id)"
                      class="mt-2 bg-orange-600 hover:bg-orange-700 disabled:opacity-60 text-white text-xs px-3 py-1.5 rounded-lg transition">
                      @if (regeneratingIds.has(m.id)) { ⏳ Génération... } @else { ✨ Générer résumé AI }
                    </button>
                  }
                </div>
              }

            </div>
          }
        </div>
      }
    </div>

    <!-- ═══ NOTES MODAL ═══ -->
    @if (selectedNotesMeeting) {
      <div class="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4"
           (click)="selectedNotesMeeting = null">
        <div class="bg-background border border-border rounded-2xl shadow-2xl w-full max-w-2xl max-h-[80vh] flex flex-col"
             (click)="$event.stopPropagation()">
          <div class="flex items-center justify-between px-5 py-4 border-b border-border">
            <div>
              <h2 class="font-bold text-base">📋 Notes de la réunion</h2>
              <p class="text-muted-foreground text-xs mt-0.5">
                Dr. {{ selectedNotesMeeting.doctorName }} — {{ selectedNotesMeeting.scheduledAt | date:'dd/MM/yyyy' }}
              </p>
            </div>
            <div class="flex gap-2">
              <button (click)="copyModalNotes()"
                class="bg-muted text-foreground text-xs px-3 py-1.5 rounded-lg border border-border transition">
                {{ copyModalNotesText }}
              </button>
              <button (click)="selectedNotesMeeting = null"
                class="text-muted-foreground hover:text-foreground text-sm px-2 py-1.5 rounded-lg hover:bg-muted transition">✕</button>
            </div>
          </div>
          <div class="flex-1 overflow-y-auto p-5">
            <pre class="text-sm leading-relaxed whitespace-pre-wrap font-sans">{{ selectedNotesMeeting.notes }}</pre>
          </div>
        </div>
      </div>
    }

    <!-- ═══ RATING MODAL ═══ -->
    @if (ratingMeeting) {
      <app-meeting-rating-modal
        [meetingId]="ratingMeeting.id"
        [doctorKeycloakId]="ratingMeeting.doctorKeycloakId"
        [patientKeycloakId]="patientKeycloakId"
        [doctorName]="ratingMeeting.doctorName"
        (close)="closeRatingModal()"
        (rated)="onRated($event, ratingMeeting.id)"
      />
    }
  `,
})
export class HelperMeetingListComponent implements OnInit {
  @Input() patientKeycloakId!: string;
  @Input() currentUserKeycloakId = '';
  @Input() currentUserName = 'Aidant';
  @Output() joinMeeting = new EventEmitter<Meeting>();

  private meetingService = inject(MeetingService);
  private ratingService  = inject(RatingService);

  meetings = signal<Meeting[]>([]);
  loading  = signal(true);

  expandedSummaryId: number | null = null;
  copyTexts: Record<number, string> = {};
  regeneratingIds = new Set<number>();
  confirmDeleteId: number | null = null;
  deletingIds = new Set<number>();
  selectedNotesMeeting: Meeting | null = null;
  copyModalNotesText = '📋 Copier';

  /** Meetings already rated by this patient */
  ratedMeetingIds = new Set<number>();

  /** Meeting currently being rated */
  ratingMeeting: (Meeting & { doctorKeycloakId?: string }) | null = null;

  ngOnInit(): void {
    this.loadMeetings();
  }

  loadMeetings(): void {
    this.loading.set(true);
    this.meetingService.getMeetingsForPatient(this.patientKeycloakId).subscribe({
      next: (meetings) => {
        this.meetings.set(meetings);
        this.loading.set(false);
        // Pre-fetch tokens for active meetings
        if (this.currentUserKeycloakId) {
          meetings.filter(m => m.status === 'ACTIVE' || m.status === 'SCHEDULED')
            .forEach(m => this.meetingService.prefetchToken(m.id, this.currentUserKeycloakId, this.currentUserName));
        }
        // Check which ended meetings have already been rated
        meetings.filter(m => m.status === 'ENDED').forEach(m => {
          this.ratingService.checkRated(m.id, this.patientKeycloakId).subscribe(res => {
            if (res.rated) this.ratedMeetingIds.add(m.id);
          });
        });
      },
      error: () => this.loading.set(false),
    });
  }

  // ── Rating ─────────────────────────────────────────────────────────────────

  openRatingModal(m: Meeting): void {
    this.ratingMeeting = m as Meeting & { doctorKeycloakId?: string };
  }

  closeRatingModal(): void {
    this.ratingMeeting = null;
  }

  onRated(stars: number, meetingId: number): void {
    this.ratedMeetingIds.add(meetingId);
    this.ratingMeeting = null;
  }

  // ── Toggle summary ─────────────────────────────────────────────────────────

  toggleSummary(id: number): void {
    this.expandedSummaryId = this.expandedSummaryId === id ? null : id;
  }

  copySummary(summary: string, id: number): void {
    navigator.clipboard.writeText(summary).then(() => {
      this.copyTexts[id] = '✅ Copié !';
      setTimeout(() => delete this.copyTexts[id], 2000);
    });
  }

  regenerateSummary(m: Meeting): void {
    if (this.regeneratingIds.has(m.id)) return;
    this.regeneratingIds.add(m.id);
    this.meetingService.regenerateSummary(m.id).subscribe({
      next: (result) => {
        this.regeneratingIds.delete(m.id);
        this.meetings.update(list => list.map(x => x.id === m.id ? { ...x, aiSummary: result.summary } : x));
      },
      error: () => {
        this.regeneratingIds.delete(m.id);
        alert('Impossible de régénérer le résumé.');
      },
    });
  }

  // ── Notes modal ────────────────────────────────────────────────────────────

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

  // ── Delete ─────────────────────────────────────────────────────────────────

  deleteMeeting(id: number): void {
    this.confirmDeleteId = null;
    this.deletingIds.add(id);
    this.meetingService.deleteMeeting(id).subscribe({
      next: () => { this.deletingIds.delete(id); this.meetings.update(l => l.filter(m => m.id !== id)); },
      error: () => { this.deletingIds.delete(id); alert('Erreur lors de la suppression.'); },
    });
  }

  // ── PDF ────────────────────────────────────────────────────────────────────

  downloadPdfBackend(m: Meeting): void {
    this.meetingService.downloadMeetingPdf(m.id, m.patientName);
  }

  // ── Parsers ────────────────────────────────────────────────────────────────

  parseSummary(text: string): { title: string; content: string }[] {
    const sections: { title: string; content: string }[] = [];
    let currentTitle = '';
    let currentContent: string[] = [];
    for (const line of text.split('\n')) {
      const m = line.match(/^##\s+(.+)/);
      if (m) {
        if (currentContent.length || currentTitle)
          sections.push({ title: currentTitle, content: currentContent.join('\n').trim() });
        currentTitle = m[1].trim();
        currentContent = [];
      } else {
        currentContent.push(line);
      }
    }
    if (currentContent.length || currentTitle)
      sections.push({ title: currentTitle, content: currentContent.join('\n').trim() });
    return sections.length ? sections : [{ title: '', content: text }];
  }

  getStatusLabel(status: string): string {
    return ({ SCHEDULED: 'Planifiée', ACTIVE: 'En cours', ENDED: 'Terminée' } as any)[status] ?? status;
  }

  getStatusBadgeClass(status: string): Record<string, boolean> {
    return {
      'bg-gray-200 text-gray-700 dark:bg-gray-700 dark:text-gray-300': status === 'SCHEDULED',
      'bg-emerald-100 text-emerald-800 dark:bg-emerald-900 dark:text-emerald-300': status === 'ACTIVE',
      'bg-red-100 text-red-700 dark:bg-red-900 dark:text-red-300': status === 'ENDED',
    };
  }
}
