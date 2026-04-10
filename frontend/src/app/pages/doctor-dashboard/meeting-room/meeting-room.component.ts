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
import { LiveTranscriptComponent } from '../live-transcript/live-transcript.component';

type SideTab = 'notes' | 'transcript' | 'summary';

@Component({
  selector: 'app-meeting-room',
  standalone: true,
  imports: [CommonModule, FormsModule, LiveTranscriptComponent],
  template: `
    <!-- ═══ FULLSCREEN OVERLAY ═══ -->
    <div class="fixed inset-0 z-50 bg-black flex flex-col">

      <!-- ═══ HEADER ═══ -->
      <div class="h-14 bg-gray-900 border-b border-gray-700 flex items-center justify-between px-4 shrink-0">
        <div class="flex items-center gap-3">
          <span class="text-white font-semibold text-sm truncate max-w-xs">
            🩺 Réunion — {{ meeting.patientName }}
          </span>
          <span class="text-xs font-medium px-2 py-0.5 rounded-full" [ngClass]="statusBadgeClass">
            {{ statusLabel }}
          </span>
          @if (meetingStatus === 'ACTIVE') {
            <span class="text-emerald-400 text-xs font-mono bg-emerald-950 px-2 py-0.5 rounded">
              ⏱ {{ timerDisplay }}
            </span>
          }
        </div>
        <div class="flex items-center gap-2">
          <button
            (click)="showSidePanel = !showSidePanel"
            class="bg-gray-700 hover:bg-gray-600 text-gray-300 text-xs px-2.5 py-1.5 rounded transition"
          >
            {{ showSidePanel ? '◀ Masquer' : '▶ Afficher' }}
          </button>
          @if (meetingStatus !== 'ENDED') {
            <button
              (click)="endMeetingAndGenerateSummary()"
              [disabled]="showSummaryLoading"
              class="bg-red-600 hover:bg-red-700 disabled:bg-red-800 disabled:opacity-60 text-white text-xs font-medium px-3 py-1.5 rounded transition"
            >
              @if (showSummaryLoading) { ⏳ Génération... } @else { ⏹ Terminer }
            </button>
          }
          @if (meetingStatus === 'ENDED') {
            <button (click)="close.emit()"
              class="bg-gray-700 hover:bg-gray-600 text-white text-xs font-medium px-3 py-1.5 rounded transition">
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
            <div class="flex items-center justify-center h-full text-gray-400 text-sm">
              @if (loadingToken) {
                <div class="flex flex-col items-center gap-3">
                  <div class="w-10 h-10 border-2 border-gray-600 border-t-emerald-400 rounded-full animate-spin"></div>
                  <span>Connexion à la salle...</span>
                </div>
              } @else if (tokenError) {
                <div class="flex flex-col items-center gap-2 text-red-400">
                  <span class="text-3xl">⚠️</span>
                  <span>{{ tokenError }}</span>
                  <button (click)="close.emit()" class="mt-2 bg-gray-700 hover:bg-gray-600 text-white text-xs px-3 py-1.5 rounded">Retour</button>
                </div>
              }
            </div>
          }
        </div>

        <!-- ── SIDE PANEL ── -->
        @if (showSidePanel) {
          <div class="w-96 bg-gray-900 border-l border-gray-700 flex flex-col shrink-0 overflow-hidden">

            <!-- ── TAB BAR ── -->
            <div class="flex border-b border-gray-700 shrink-0">
              @for (tab of tabs; track tab.id) {
                <button
                  (click)="activeTab = tab.id"
                  class="flex-1 text-xs py-2.5 font-medium transition border-b-2"
                  [ngClass]="activeTab === tab.id
                    ? 'text-white border-emerald-500 bg-gray-800'
                    : 'text-gray-400 border-transparent hover:text-gray-200 hover:bg-gray-800/50'"
                >
                  {{ tab.icon }} {{ tab.label }}
                </button>
              }
            </div>

            <!-- ── TAB: NOTES ── -->
            @if (activeTab === 'notes') {
              <div class="flex flex-col p-3 flex-1 overflow-hidden">
                <div class="flex items-center justify-between mb-2">
                  <h3 class="text-white text-xs font-semibold">📝 Notes de réunion</h3>
                  <div class="flex items-center gap-2">
                    @if (notesValue && meetingStatus === 'ENDED') {
                      <button (click)="showFullNotes = true"
                        class="bg-blue-700 hover:bg-blue-600 text-white text-xs px-2.5 py-1 rounded transition">
                        📋 Voir tout
                      </button>
                    }
                    <span class="text-xs" [ngClass]="saveIndicatorClass">{{ saveIndicator }}</span>
                  </div>
                </div>
                <textarea
                  [(ngModel)]="notesValue"
                  (input)="onNotesInput()"
                  placeholder="Prenez vos notes ici...&#10;&#10;• État du patient&#10;• Observations&#10;• Prescriptions"
                  class="flex-1 bg-gray-800 text-white text-xs resize-none p-3 rounded border border-gray-600 focus:border-emerald-500 focus:outline-none placeholder-gray-500"
                  [disabled]="meetingStatus === 'ENDED'"
                ></textarea>

                <!-- Import from transcript -->
                @if (latestTranscriptText && meetingStatus !== 'ENDED') {
                  <button
                    (click)="mergeTranscriptIntoNotes()"
                    class="mt-2 w-full text-xs bg-gray-700 hover:bg-gray-600 text-gray-300 py-1.5 rounded transition"
                  >
                    📥 Importer le transcript dans les notes
                  </button>
                }
              </div>
            }

            <!-- ── TAB: TRANSCRIPT ── -->
            @if (activeTab === 'transcript') {
              <div class="flex-1 overflow-hidden">
                <app-live-transcript
                  [meetingId]="meeting.id"
                  [meetingEnded]="meetingStatus === 'ENDED'"
                  [patientName]="meeting.patientName"
                  [doctorName]="meeting.doctorName"
                  (transcriptChange)="onTranscriptChange($event)"
                  class="flex flex-col h-full"
                ></app-live-transcript>
              </div>
            }

            <!-- ── TAB: AI SUMMARY ── -->
            @if (activeTab === 'summary') {
              <div class="flex flex-col p-3 flex-1 overflow-hidden">
                <div class="flex items-center justify-between mb-2">
                  <h3 class="text-white text-xs font-semibold">🤖 Résumé AI final</h3>
                  <div class="flex gap-1">
                    @if (aiSummary) {
                      <button (click)="showFullSummary = true"
                        class="bg-purple-700 hover:bg-purple-600 text-white text-xs px-2.5 py-1 rounded transition">
                        🔍 Plein écran
                      </button>
                      <button (click)="copySummary()"
                        class="bg-gray-700 hover:bg-gray-600 text-gray-300 text-xs px-2.5 py-1 rounded transition">
                        {{ copyButtonText }}
                      </button>
                    }
                  </div>
                </div>

                @if (showSummaryLoading) {
                  <div class="flex-1 flex items-center justify-center">
                    <div class="flex flex-col items-center gap-3">
                      <div class="w-8 h-8 border-2 border-gray-600 border-t-orange-400 rounded-full animate-spin"></div>
                      <span class="text-orange-400 text-xs font-semibold">Groq analyse les notes...</span>
                    </div>
                  </div>
                } @else if (summaryError) {
                  <div class="flex-1 flex flex-col items-center justify-center gap-3 text-red-400">
                    <span class="text-xs text-center">{{ summaryError }}</span>
                    <button (click)="retrySummary()"
                      class="bg-red-900 hover:bg-red-800 text-red-300 text-xs px-3 py-1.5 rounded transition">
                      🔄 Réessayer
                    </button>
                  </div>
                } @else if (aiSummary) {
                  <div
                    class="flex-1 overflow-y-auto bg-gray-800 rounded p-3 text-gray-200 text-xs leading-relaxed border border-gray-700 cursor-pointer hover:border-purple-600 transition"
                    style="white-space: pre-wrap"
                    (click)="showFullSummary = true"
                  >{{ aiSummary }}</div>
                } @else {
                  <div class="flex-1 flex items-center justify-center text-gray-500 text-xs text-center px-4">
                    <div>
                      <span class="text-3xl block mb-2">🧠</span>
                      <span class="text-gray-400 text-xs font-medium block mb-1">Résumé AI automatique</span>
                      Le résumé sera généré par Groq lorsque vous terminerez la réunion.
                    </div>
                  </div>
                }
              </div>
            }

          </div>
        }
      </div>
    </div>

    <!-- ═══ MODAL : NOTES COMPLÈTES ═══ -->
    @if (showFullNotes) {
      <div class="fixed inset-0 z-[60] bg-black/70 flex items-center justify-center p-6"
           (click)="showFullNotes = false">
        <div class="bg-gray-900 rounded-2xl border border-gray-700 shadow-2xl w-full max-w-2xl max-h-[85vh] flex flex-col"
             (click)="$event.stopPropagation()">
          <div class="flex items-center justify-between px-5 py-4 border-b border-gray-700">
            <div>
              <h2 class="text-white font-bold text-base">📝 Notes complètes</h2>
              <p class="text-gray-400 text-xs mt-0.5">{{ meeting.patientName }} — {{ meeting.doctorName }}</p>
            </div>
            <div class="flex gap-2">
              <button (click)="copyNotes()"
                class="bg-gray-700 hover:bg-gray-600 text-gray-300 text-xs px-3 py-1.5 rounded transition">
                {{ copyNotesButtonText }}
              </button>
              <button (click)="showFullNotes = false"
                class="bg-gray-700 hover:bg-gray-600 text-white text-xs px-3 py-1.5 rounded transition">
                ✕ Fermer
              </button>
            </div>
          </div>
          <div class="flex-1 overflow-y-auto p-5">
            <pre class="text-gray-200 text-sm leading-relaxed whitespace-pre-wrap font-sans">{{ notesValue || 'Aucune note.' }}</pre>
          </div>
        </div>
      </div>
    }

    <!-- ═══ MODAL : RÉSUMÉ AI COMPLET ═══ -->
    @if (showFullSummary) {
      <div class="fixed inset-0 z-[60] bg-black/70 flex items-center justify-center p-6"
           (click)="showFullSummary = false">
        <div class="bg-gray-900 rounded-2xl border border-gray-700 shadow-2xl w-full max-w-2xl max-h-[85vh] flex flex-col"
             (click)="$event.stopPropagation()">
          <div class="flex items-center justify-between px-5 py-4 border-b border-gray-700">
            <div>
              <h2 class="text-white font-bold text-base">🤖 Résumé AI — Réunion médicale</h2>
              <p class="text-gray-400 text-xs mt-0.5">Généré par Groq pour {{ meeting.patientName }}</p>
            </div>
            <div class="flex gap-2">
              <button (click)="copySummary()"
                class="bg-purple-700 hover:bg-purple-600 text-white text-xs px-3 py-1.5 rounded transition">
                {{ copyButtonText }}
              </button>
              <button (click)="showFullSummary = false"
                class="bg-gray-700 hover:bg-gray-600 text-white text-xs px-3 py-1.5 rounded transition">
                ✕ Fermer
              </button>
            </div>
          </div>
          <div class="flex-1 overflow-y-auto p-5">
            <div class="prose prose-invert prose-sm max-w-none">
              @for (section of parsedSummary; track section.title) {
                @if (section.title) {
                  <h3 class="text-emerald-400 font-semibold text-sm mt-4 mb-2 first:mt-0">{{ section.title }}</h3>
                }
                <p class="text-gray-200 text-sm leading-relaxed whitespace-pre-wrap">{{ section.content }}</p>
              }
              @if (parsedSummary.length === 0) {
                <pre class="text-gray-200 text-sm leading-relaxed whitespace-pre-wrap font-sans">{{ aiSummary }}</pre>
              }
            </div>
          </div>
        </div>
      </div>
    }
  `,
})
export class MeetingRoomComponent implements OnInit, OnDestroy {
  @Input() meeting!: Meeting;
  @Input() currentUser!: { keycloakId: string; name: string; role: string };
  @Output() meetingEnded = new EventEmitter<{ summary: string; durationMinutes: number }>();
  @Output() close = new EventEmitter<void>();

  private meetingService = inject(MeetingService);
  private sanitizer = inject(DomSanitizer);

  // ── Tabs ───────────────────────────────────────────────────────────────────
  readonly tabs: { id: SideTab; icon: string; label: string }[] = [
    { id: 'notes',      icon: '📝', label: 'Notes'      },
    { id: 'transcript', icon: '🎙️', label: 'Transcript' },
    { id: 'summary',    icon: '🤖', label: 'Résumé AI'  },
  ];
  activeTab: SideTab = 'transcript'; // open on transcript by default

  // ── Video ──────────────────────────────────────────────────────────────────
  safeRoomUrl: SafeResourceUrl | null = null;
  loadingToken = true;
  tokenError = '';

  // ── Notes ──────────────────────────────────────────────────────────────────
  notesValue = '';
  saveIndicator = '';
  saveIndicatorClass = 'text-gray-500';
  private saveTimeout: any = null;
  showFullNotes = false;
  copyNotesButtonText = '📋 Copier';

  // ── Transcript ─────────────────────────────────────────────────────────────
  latestTranscriptText = '';

  // ── AI Summary ─────────────────────────────────────────────────────────────
  aiSummary = '';
  showSummaryLoading = false;
  summaryError = '';
  showFullSummary = false;
  copyButtonText = '📋 Copier';
  parsedSummary: { title: string; content: string }[] = [];

  // ── PDF banner ─────────────────────────────────────────────────────────────
  showPdfBanner = false;

  downloadPdf(): void {
    this.meetingService.downloadMeetingPdf(this.meeting.id, this.meeting.patientName);
    this.showPdfBanner = false;
  }

  // ── Timer ──────────────────────────────────────────────────────────────────
  timerDisplay = '00:00';
  private timerInterval: any = null;
  private startTime: Date | null = null;

  // ── UI ─────────────────────────────────────────────────────────────────────
  showSidePanel = true;
  meetingStatus: 'SCHEDULED' | 'ACTIVE' | 'ENDED' = 'SCHEDULED';

  get statusLabel(): string {
    const map: Record<string, string> = { SCHEDULED: 'Planifiée', ACTIVE: 'En cours', ENDED: 'Terminée' };
    return map[this.meetingStatus] ?? this.meetingStatus;
  }

  get statusBadgeClass(): Record<string, boolean> {
    return {
      'bg-gray-600 text-gray-200':     this.meetingStatus === 'SCHEDULED',
      'bg-emerald-900 text-emerald-300': this.meetingStatus === 'ACTIVE',
      'bg-red-900 text-red-300':       this.meetingStatus === 'ENDED',
    };
  }

  // ── Lifecycle ──────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.meetingStatus = this.meeting.status;
    this.notesValue   = this.meeting.notes || '';
    this.aiSummary    = this.meeting.aiSummary || '';
    if (this.aiSummary) this.parseSummary(this.aiSummary);

    this.loadingToken = true;
    this.meetingService
      .getMeetingToken(this.meeting.id, this.currentUser.keycloakId, this.currentUser.name)
      .subscribe({
        next: (data) => {
          const url = data.roomUrl + '?t=' + data.token + '&lang=fr&showLeaveButton=false';
          this.safeRoomUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
          this.loadingToken = false;
          this.meetingStatus = 'ACTIVE';
          this.startTimer();
        },
        error: () => {
          this.loadingToken = false;
          this.tokenError   = 'Impossible de se connecter à la salle de réunion.';
        },
      });
  }

  ngOnDestroy(): void {
    if (this.timerInterval) clearInterval(this.timerInterval);
    if (this.saveTimeout)   clearTimeout(this.saveTimeout);
  }

  // ── Timer ──────────────────────────────────────────────────────────────────

  private startTimer(): void {
    this.startTime = this.meeting.startedAt ? new Date(this.meeting.startedAt) : new Date();
    this.updateTimerDisplay();
    this.timerInterval = setInterval(() => this.updateTimerDisplay(), 1000);
  }

  private updateTimerDisplay(): void {
    if (!this.startTime) return;
    const elapsed = Math.floor((Date.now() - this.startTime.getTime()) / 1000);
    const m = Math.floor(elapsed / 60);
    const s = elapsed % 60;
    this.timerDisplay = `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  }

  // ── Notes auto-save ────────────────────────────────────────────────────────

  onNotesInput(): void {
    this.saveIndicator      = '✏️ Modification...';
    this.saveIndicatorClass = 'text-yellow-400';
    if (this.saveTimeout) clearTimeout(this.saveTimeout);
    this.saveTimeout = setTimeout(() => {
      this.meetingService.updateNotes(this.meeting.id, this.notesValue).subscribe({
        next: () => { this.saveIndicator = '✅ Sauvegardé'; this.saveIndicatorClass = 'text-emerald-400'; },
        error: () => { this.saveIndicator = '❌ Erreur';     this.saveIndicatorClass = 'text-red-400'; },
      });
    }, 1500);
  }

  // ── Transcript → Notes ─────────────────────────────────────────────────────

  onTranscriptChange(text: string): void {
    this.latestTranscriptText = text;
  }

  mergeTranscriptIntoNotes(): void {
    if (!this.latestTranscriptText) return;
    const separator = this.notesValue ? '\n\n--- Transcript ---\n' : '';
    this.notesValue += separator + this.latestTranscriptText;
    this.onNotesInput();
  }

  // ── End meeting + AI summary ───────────────────────────────────────────────

  endMeetingAndGenerateSummary(): void {
    if (this.showSummaryLoading) return;
    this.summaryError        = '';
    this.showSummaryLoading  = true;
    this.showSidePanel       = true;
    this.activeTab           = 'summary';

    this.meetingService.endMeeting(this.meeting.id, this.notesValue).subscribe({
      next: (result) => {
        this.aiSummary          = result.summary || '';
        this.showSummaryLoading = false;
        this.meetingStatus      = 'ENDED';
        if (this.timerInterval) clearInterval(this.timerInterval);
        if (this.aiSummary) this.parseSummary(this.aiSummary);
        this.meetingEnded.emit({ summary: result.summary, durationMinutes: result.durationMinutes });
        // Offer PDF download automatically
        this.showPdfBanner = true;
      },
      error: () => {
        this.showSummaryLoading = false;
        this.meetingStatus      = 'ENDED';
        this.summaryError       = 'Erreur lors de la génération du résumé. Cliquez sur Réessayer.';
        if (this.timerInterval) clearInterval(this.timerInterval);
      },
    });
  }

  retrySummary(): void {
    if (this.showSummaryLoading) return;
    this.summaryError       = '';
    this.showSummaryLoading = true;
    this.meetingService.regenerateSummary(this.meeting.id).subscribe({
      next: (result) => {
        this.aiSummary          = result.summary || '';
        this.showSummaryLoading = false;
        if (this.aiSummary) this.parseSummary(this.aiSummary);
      },
      error: () => {
        this.showSummaryLoading = false;
        this.summaryError       = 'Régénération échouée. Vérifiez la clé API Groq dans application.yml.';
      },
    });
  }

  // ── Parse summary sections ─────────────────────────────────────────────────

  private parseSummary(text: string): void {
    this.parsedSummary = [];
    const lines = text.split('\n');
    let currentTitle = '';
    let currentContent: string[] = [];
    for (const line of lines) {
      const m = line.match(/^##\s+(.+)/);
      if (m) {
        if (currentContent.length || currentTitle)
          this.parsedSummary.push({ title: currentTitle, content: currentContent.join('\n').trim() });
        currentTitle   = m[1].trim();
        currentContent = [];
      } else {
        currentContent.push(line);
      }
    }
    if (currentContent.length || currentTitle)
      this.parsedSummary.push({ title: currentTitle, content: currentContent.join('\n').trim() });
  }

  // ── Copy helpers ───────────────────────────────────────────────────────────

  copySummary(): void {
    if (!this.aiSummary) return;
    navigator.clipboard.writeText(this.aiSummary).then(() => {
      this.copyButtonText = '✅ Copié !';
      setTimeout(() => (this.copyButtonText = '📋 Copier'), 2000);
    });
  }

  copyNotes(): void {
    navigator.clipboard.writeText(this.notesValue).then(() => {
      this.copyNotesButtonText = '✅ Copié !';
      setTimeout(() => (this.copyNotesButtonText = '📋 Copier'), 2000);
    });
  }
}
