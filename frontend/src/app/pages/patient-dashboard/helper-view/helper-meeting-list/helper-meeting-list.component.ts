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
                  @if (m.status === 'ENDED') {
                    <div class="flex gap-2">
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
                          📋 Voir les notes
                        </button>
                      }
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

  // Notes modal
  selectedNotesMeeting: Meeting | null = null;
  copyModalNotesText = '📋 Copier';

  ngOnInit(): void {
    this.loadMeetings();
  }

  loadMeetings(): void {
    this.loading.set(true);
    this.meetingService.getMeetingsForPatient(this.patientKeycloakId).subscribe({
      next: (meetings) => {
        this.meetings.set(meetings);
        this.loading.set(false);
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

  copySummary(summary: string, meetingId: number): void {
    navigator.clipboard.writeText(summary).then(() => {
      this.copyTexts[meetingId] = '✅ Copié !';
      setTimeout(() => delete this.copyTexts[meetingId], 2000);
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
