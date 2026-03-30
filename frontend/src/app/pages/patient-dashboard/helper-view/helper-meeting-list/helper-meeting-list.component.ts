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
import {
  Meeting,
  MeetingService,
} from '@/core/services/meeting.service';

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
          <div
            class="w-8 h-8 border-2 border-muted border-t-emerald-400 rounded-full animate-spin"
          ></div>
        </div>
      } @else if (meetings().length === 0) {
        <div
          class="bg-card border border-border rounded-xl p-10 text-center"
        >
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
            <div
              class="bg-card border border-border rounded-xl p-4 hover:border-primary/30 transition"
            >
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
                    <button
                      (click)="toggleSummary(m.id)"
                      class="bg-muted hover:bg-muted/80 text-foreground text-xs font-medium px-3 py-2 rounded-lg transition border border-border"
                    >
                      @if (expandedSummaryId === m.id) {
                        ▲ Masquer
                      } @else {
                        🤖 Voir résumé
                      }
                    </button>
                  }
                </div>
              </div>

              <!-- Expanded Summary -->
              @if (expandedSummaryId === m.id && m.aiSummary) {
                <div
                  class="mt-3 pt-3 border-t border-border text-sm leading-relaxed bg-muted/30 rounded-lg p-4 -mx-1"
                  style="white-space: pre-wrap"
                >
                  {{ m.aiSummary }}
                </div>
              }
              @if (expandedSummaryId === m.id && !m.aiSummary) {
                <div class="mt-3 pt-3 border-t border-border text-xs text-muted-foreground italic">
                  Aucun résumé disponible.
                </div>
              }
            </div>
          }
        </div>
      }
    </div>
  `,
})
export class HelperMeetingListComponent implements OnInit {
  @Input() patientKeycloakId!: string;
  @Output() joinMeeting = new EventEmitter<Meeting>();

  private meetingService = inject(MeetingService);

  meetings = signal<Meeting[]>([]);
  loading = signal(true);
  expandedSummaryId: number | null = null;

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
      error: () => {
        this.loading.set(false);
      },
    });
  }

  toggleSummary(id: number): void {
    this.expandedSummaryId = this.expandedSummaryId === id ? null : id;
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
