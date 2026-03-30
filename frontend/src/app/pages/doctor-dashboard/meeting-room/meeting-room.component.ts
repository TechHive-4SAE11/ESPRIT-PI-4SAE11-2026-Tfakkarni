import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnInit,
  OnDestroy,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Meeting, MeetingService } from '@/core/services/meeting.service';

@Component({
  selector: 'app-meeting-room',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <!-- ═══ FULLSCREEN OVERLAY ═══ -->
    <div class="fixed inset-0 z-50 bg-black flex flex-col">
      <!-- ═══ HEADER ═══ -->
      <div
        class="h-14 bg-gray-900 border-b border-gray-700 flex items-center justify-between px-4 shrink-0"
      >
        <!-- Left: Title + Badge -->
        <div class="flex items-center gap-3">
          <span class="text-white font-semibold text-sm truncate max-w-xs">
            🩺 Réunion — {{ meeting.patientName }}
          </span>

          <!-- Status Badge -->
          <span
            class="text-xs font-medium px-2 py-0.5 rounded-full"
            [ngClass]="statusBadgeClass"
          >
            {{ statusLabel }}
          </span>

          <!-- Timer -->
          @if (meetingStatus === 'ACTIVE') {
            <span class="text-emerald-400 text-xs font-mono bg-emerald-950 px-2 py-0.5 rounded">
              ⏱ {{ timerDisplay }}
            </span>
          }
        </div>

        <!-- Right: Controls -->
        <div class="flex items-center gap-2">
          <!-- Toggle side panel -->
          <button
            (click)="showSidePanel = !showSidePanel"
            class="bg-gray-700 hover:bg-gray-600 text-gray-300 text-xs px-2.5 py-1.5 rounded transition"
            [title]="showSidePanel ? 'Masquer le panneau' : 'Afficher le panneau'"
          >
            {{ showSidePanel ? '◀ Masquer notes' : '▶ Afficher notes' }}
          </button>

          <!-- End Meeting -->
          @if (meetingStatus !== 'ENDED') {
            <button
              (click)="endMeetingAndGenerateSummary()"
              [disabled]="showSummaryLoading"
              class="bg-red-600 hover:bg-red-700 disabled:bg-red-800 disabled:opacity-60 text-white text-xs font-medium px-3 py-1.5 rounded transition"
            >
              @if (showSummaryLoading) {
                ⏳ Génération du résumé...
              } @else {
                ⏹ Terminer la réunion
              }
            </button>
          }

          <!-- Close (only if ended) -->
          @if (meetingStatus === 'ENDED') {
            <button
              (click)="close.emit()"
              class="bg-gray-700 hover:bg-gray-600 text-white text-xs font-medium px-3 py-1.5 rounded transition"
            >
              ✕ Fermer
            </button>
          }
        </div>
      </div>

      <!-- ═══ MAIN AREA ═══ -->
      <div class="flex-1 flex overflow-hidden">
        <!-- ── VIDEO ZONE ── -->
        <div class="flex-1 relative bg-gray-950">
          @if (safeRoomUrl) {
            <iframe
              [src]="safeRoomUrl"
              allow="camera; microphone; fullscreen; speaker; display-capture"
              class="w-full h-full border-0"
            ></iframe>
          } @else {
            <div
              class="flex items-center justify-center h-full text-gray-400 text-sm"
            >
              @if (loadingToken) {
                <div class="flex flex-col items-center gap-3">
                  <div
                    class="w-10 h-10 border-2 border-gray-600 border-t-emerald-400 rounded-full animate-spin"
                  ></div>
                  <span>Connexion à la salle de réunion...</span>
                  <span class="text-gray-600 text-xs">Préparation de la vidéo Daily.co</span>
                </div>
              } @else if (tokenError) {
                <div class="flex flex-col items-center gap-2 text-red-400">
                  <span class="text-3xl">⚠️</span>
                  <span>{{ tokenError }}</span>
                  <button
                    (click)="close.emit()"
                    class="mt-2 bg-gray-700 hover:bg-gray-600 text-white text-xs px-3 py-1.5 rounded"
                  >
                    Retour
                  </button>
                </div>
              }
            </div>
          }
        </div>

        <!-- ── SIDE PANEL ── -->
        @if (showSidePanel) {
          <div
            class="w-80 bg-gray-900 border-l border-gray-700 flex flex-col shrink-0"
          >
            <!-- Notes Section -->
            <div class="flex-1 flex flex-col p-3 border-b border-gray-700 min-h-0">
              <div class="flex items-center justify-between mb-2">
                <h3 class="text-white text-sm font-semibold">📝 Notes de réunion</h3>
                <span class="text-xs" [ngClass]="saveIndicatorClass">
                  {{ saveIndicator }}
                </span>
              </div>

              <textarea
                [(ngModel)]="notesValue"
                (input)="onNotesInput()"
                placeholder="Prenez vos notes ici pendant la réunion...&#10;&#10;• État du patient&#10;• Observations&#10;• Prescriptions à modifier&#10;• Suivi à planifier"
                class="flex-1 bg-gray-800 text-white text-sm resize-none p-3 rounded border border-gray-600 focus:border-emerald-500 focus:outline-none placeholder-gray-500"
                [disabled]="meetingStatus === 'ENDED'"
              ></textarea>
            </div>

            <!-- AI Summary Section -->
            <div class="h-72 flex flex-col p-3 overflow-hidden">
              <h3 class="text-white text-sm font-semibold mb-2">
                🤖 Résumé AI
              </h3>

              @if (showSummaryLoading) {
                <div class="flex-1 flex items-center justify-center">
                  <div class="flex flex-col items-center gap-3">
                    <div
                      class="w-7 h-7 border-2 border-gray-600 border-t-orange-400 rounded-full animate-spin"
                    ></div>
                    <span class="text-gray-400 text-xs text-center">
                      Claude analyse les notes<br/>et génère le résumé...
                    </span>
                  </div>
                </div>
              } @else if (aiSummary) {
                <div
                  class="flex-1 overflow-y-auto bg-gray-800 rounded p-3 text-gray-200 text-xs leading-relaxed border border-gray-700"
                  style="white-space: pre-wrap"
                >
                  {{ aiSummary }}
                </div>
                <button
                  (click)="copySummary()"
                  class="mt-2 bg-gray-700 hover:bg-gray-600 text-gray-300 text-xs px-3 py-1.5 rounded transition w-full"
                >
                  {{ copyButtonText }}
                </button>
              } @else {
                <div
                  class="flex-1 flex items-center justify-center text-gray-500 text-xs text-center px-4"
                >
                  <div>
                    <span class="text-2xl block mb-2">🧠</span>
                    Le résumé AI sera généré automatiquement lorsque vous terminerez la réunion.
                  </div>
                </div>
              }
            </div>
          </div>
        }
      </div>
    </div>
  `,
})
export class MeetingRoomComponent implements OnInit, OnDestroy {
  @Input() meeting!: Meeting;
  @Input() currentUser!: { keycloakId: string; name: string; role: string };
  @Output() meetingEnded = new EventEmitter<{
    summary: string;
    durationMinutes: number;
  }>();
  @Output() close = new EventEmitter<void>();

  private meetingService = inject(MeetingService);
  private sanitizer = inject(DomSanitizer);

  // Video
  safeRoomUrl: SafeResourceUrl | null = null;
  loadingToken = true;
  tokenError = '';

  // Notes
  notesValue = '';
  saveIndicator = '';
  saveIndicatorClass = 'text-gray-500';
  private saveTimeout: any = null;

  // AI Summary
  aiSummary = '';
  showSummaryLoading = false;
  copyButtonText = '📋 Copier le résumé';

  // Timer
  timerDisplay = '00:00';
  private timerInterval: any = null;
  private startTime: Date | null = null;

  // UI
  showSidePanel = true;
  meetingStatus: 'SCHEDULED' | 'ACTIVE' | 'ENDED' = 'SCHEDULED';

  get statusLabel(): string {
    switch (this.meetingStatus) {
      case 'SCHEDULED':
        return 'Planifiée';
      case 'ACTIVE':
        return 'En cours';
      case 'ENDED':
        return 'Terminée';
    }
  }

  get statusBadgeClass(): Record<string, boolean> {
    return {
      'bg-gray-600 text-gray-200': this.meetingStatus === 'SCHEDULED',
      'bg-emerald-900 text-emerald-300': this.meetingStatus === 'ACTIVE',
      'bg-red-900 text-red-300': this.meetingStatus === 'ENDED',
    };
  }

  ngOnInit(): void {
    this.meetingStatus = this.meeting.status;
    this.notesValue = this.meeting.notes || '';
    this.aiSummary = this.meeting.aiSummary || '';

    // Fetch token and build iframe URL
    this.loadingToken = true;
    this.meetingService
      .getMeetingToken(
        this.meeting.id,
        this.currentUser.keycloakId,
        this.currentUser.name
      )
      .subscribe({
        next: (data) => {
          const url =
            data.roomUrl + '?t=' + data.token + '&lang=fr&showLeaveButton=false';
          this.safeRoomUrl =
            this.sanitizer.bypassSecurityTrustResourceUrl(url);
          this.loadingToken = false;
          this.meetingStatus = 'ACTIVE';
          this.startTimer();
        },
        error: (err) => {
          this.loadingToken = false;
          this.tokenError =
            'Impossible de se connecter à la salle de réunion.';
          console.error('Token error:', err);
        },
      });
  }

  // ── Timer ──

  private startTimer(): void {
    this.startTime = this.meeting.startedAt
      ? new Date(this.meeting.startedAt)
      : new Date();

    this.updateTimerDisplay();
    this.timerInterval = setInterval(() => {
      this.updateTimerDisplay();
    }, 1000);
  }

  private updateTimerDisplay(): void {
    if (!this.startTime) return;
    const elapsed = Math.floor(
      (Date.now() - this.startTime.getTime()) / 1000
    );
    const minutes = Math.floor(elapsed / 60);
    const seconds = elapsed % 60;
    this.timerDisplay = `${String(minutes).padStart(2, '0')}:${String(
      seconds
    ).padStart(2, '0')}`;
  }

  // ── Notes auto-save ──

  onNotesInput(): void {
    this.saveIndicator = '✏️ Modification...';
    this.saveIndicatorClass = 'text-yellow-400';

    if (this.saveTimeout) clearTimeout(this.saveTimeout);

    this.saveTimeout = setTimeout(() => {
      this.meetingService
        .updateNotes(this.meeting.id, this.notesValue)
        .subscribe({
          next: () => {
            this.saveIndicator = '✅ Sauvegardé';
            this.saveIndicatorClass = 'text-emerald-400';
          },
          error: () => {
            this.saveIndicator = '❌ Erreur';
            this.saveIndicatorClass = 'text-red-400';
          },
        });
    }, 2000);
  }

  // ── End meeting + AI summary ──

  endMeetingAndGenerateSummary(): void {
    if (this.showSummaryLoading) return;

    this.showSummaryLoading = true;
    this.showSidePanel = true; // Make sure panel is visible for summary

    this.meetingService
      .endMeeting(this.meeting.id, this.notesValue)
      .subscribe({
        next: (result) => {
          this.aiSummary = result.summary;
          this.showSummaryLoading = false;
          this.meetingStatus = 'ENDED';

          if (this.timerInterval) clearInterval(this.timerInterval);

          this.meetingEnded.emit({
            summary: result.summary,
            durationMinutes: result.durationMinutes,
          });
        },
        error: (err) => {
          this.showSummaryLoading = false;
          console.error('Error ending meeting:', err);
        },
      });
  }

  // ── Copy summary ──

  copySummary(): void {
    if (!this.aiSummary) return;
    navigator.clipboard.writeText(this.aiSummary).then(() => {
      this.copyButtonText = '✅ Copié !';
      setTimeout(() => (this.copyButtonText = '📋 Copier le résumé'), 2000);
    });
  }

  ngOnDestroy(): void {
    if (this.timerInterval) clearInterval(this.timerInterval);
    if (this.saveTimeout) clearTimeout(this.saveTimeout);
  }
}
