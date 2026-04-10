import {
  Component, Input, Output, EventEmitter,
  OnInit, OnDestroy, NgZone, ChangeDetectorRef, inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MeetingService } from '@/core/services/meeting.service';

// ── Types ─────────────────────────────────────────────────────────────────────

export interface TranscriptSegment {
  index: number;
  label: string;
  text: string;
  aiSummary: string | null;
  summaryLoading: boolean;
  startedAt: Date;
}

// ── Component ─────────────────────────────────────────────────────────────────

@Component({
  selector: 'app-live-transcript',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
<div class="flex flex-col h-full bg-gray-900 text-white">

  <!-- ══ TOOLBAR ══ -->
  <div class="flex items-center justify-between px-3 py-2 bg-gray-800 border-b border-gray-700 shrink-0 flex-wrap gap-2">
    <div class="flex items-center gap-2 flex-wrap">

      <!-- MIC BUTTON -->
      <button
        (click)="toggleRecording()"
        [disabled]="meetingEnded || !browserSupported"
        class="flex items-center gap-1.5 text-xs font-semibold px-3 py-1.5 rounded-full transition-all select-none"
        [ngClass]="isRecording
          ? 'bg-red-600 hover:bg-red-700 text-white'
          : 'bg-emerald-700 hover:bg-emerald-600 text-white disabled:opacity-50'"
      >
        <span class="text-base leading-none">{{ isRecording ? '⏹' : '🎙' }}</span>
        {{ isRecording ? 'Arrêter' : 'Démarrer' }}
      </button>

      <!-- REC badge -->
      @if (isRecording) {
        <span class="flex items-center gap-1 text-red-400 text-xs font-mono">
          <span class="w-2 h-2 rounded-full bg-red-500 animate-ping"></span> REC
        </span>
      }

      <!-- LANG -->
      <select [(ngModel)]="selectedLang" (ngModelChange)="onLangChange($event)"
        [disabled]="isRecording"
        class="bg-gray-700 text-gray-300 text-xs rounded px-2 py-1 border border-gray-600 focus:outline-none">
        <option value="fr-FR">🇫🇷 Français</option>
        <option value="ar-SA">🇹🇳 Arabe</option>
        <option value="en-US">🇺🇸 English</option>
      </select>
    </div>

    <div class="flex items-center gap-1.5 flex-wrap">
      <!-- INTERVAL -->
      <select [(ngModel)]="summaryIntervalMin" [disabled]="isRecording"
        class="bg-gray-700 text-gray-300 text-xs rounded px-2 py-1 border border-gray-600 focus:outline-none"
        title="Fréquence résumés Groq">
        <option [ngValue]="1">Résumé / 1 min</option>
        <option [ngValue]="2">Résumé / 2 min</option>
        <option [ngValue]="3">Résumé / 3 min</option>
        <option [ngValue]="5">Résumé / 5 min</option>
      </select>

      <button (click)="copyTranscript()" [disabled]="!hasContent"
        class="text-xs bg-gray-700 hover:bg-gray-600 disabled:opacity-40 text-gray-300 px-2.5 py-1.5 rounded transition select-none">
        {{ copyText }}
      </button>
      <button (click)="exportTranscript()" [disabled]="!hasContent"
        class="text-xs bg-gray-700 hover:bg-gray-600 disabled:opacity-40 text-gray-300 px-2.5 py-1.5 rounded transition select-none">
        ⬇ Export
      </button>
      @if (!isRecording && hasContent) {
        <button (click)="clearTranscript()"
          class="text-xs bg-gray-700 hover:bg-red-800 text-gray-400 hover:text-red-300 px-2 py-1.5 rounded transition select-none">
          🗑
        </button>
      }
    </div>
  </div>

  <!-- ══ STATUS BAR ══ -->
  <div class="flex items-center gap-3 px-3 py-1.5 border-b border-gray-700 text-xs shrink-0 flex-wrap">
    <span class="text-gray-500">{{ wordCount }} mots</span>
    <span class="text-gray-500">{{ segments.length }} segments</span>
    @if (lastSaveTime) {
      <span class="text-emerald-500">✓ Sauvegardé {{ lastSaveTime }}</span>
    }
    @if (saveError) {
      <span class="text-red-500">⚠ Erreur sauvegarde</span>
    }
    @if (!browserSupported) {
      <span class="text-yellow-400 font-medium">
        ⚠ Utilisez Google Chrome pour la reconnaissance vocale
      </span>
    }
    @if (permissionDenied) {
      <span class="text-red-400 font-medium">
        🚫 Microphone refusé — autorisez-le dans les paramètres du navigateur
      </span>
    }
    @if (debugInfo) {
      <span class="text-orange-400">{{ debugInfo }}</span>
    }
  </div>

  <!-- ══ CONTENT ══ -->
  <div class="flex-1 overflow-y-auto p-3 space-y-3">

    <!-- Welcome -->
    @if (!isRecording && !hasContent && segments.length === 0 && !permissionDenied) {
      <div class="flex flex-col items-center justify-center h-full text-center py-10">
        <div class="text-5xl mb-4">🎙️</div>
        <p class="text-gray-300 font-semibold text-sm mb-1">Transcription en temps réel</p>
        <p class="text-gray-500 text-xs max-w-xs leading-relaxed">
          Cliquez sur <strong class="text-emerald-400">Démarrer</strong> et parlez —
          le texte apparaît instantanément. Groq génère un résumé toutes les {{ summaryIntervalMin }} min.
        </p>
        @if (!browserSupported) {
          <div class="mt-4 bg-yellow-900/40 border border-yellow-700 rounded-lg px-4 py-3 text-yellow-300 text-xs max-w-xs">
            ⚠️ Ouvrez cette page dans <strong>Google Chrome</strong> pour activer la transcription.
          </div>
        }
      </div>
    }

    <!-- Permission denied -->
    @if (permissionDenied) {
      <div class="flex flex-col items-center justify-center h-full text-center py-10">
        <div class="text-5xl mb-4">🎤</div>
        <p class="text-red-400 font-semibold text-sm mb-2">Microphone non autorisé</p>
        <p class="text-gray-500 text-xs max-w-xs leading-relaxed mb-4">
          Cliquez sur 🔒 dans la barre d'adresse, autorisez le microphone, puis rechargez.
        </p>
        <button (click)="retryPermission()"
          class="bg-emerald-700 hover:bg-emerald-600 text-white text-xs px-4 py-2 rounded-lg transition">
          🔄 Réessayer
        </button>
      </div>
    }

    <!-- Completed segments -->
    @for (seg of segments; track seg.index) {
      <div class="rounded-xl border overflow-hidden"
           [ngClass]="seg.summaryLoading ? 'border-orange-700' : 'border-gray-700'">
        <div class="flex items-center justify-between px-3 py-1.5 bg-gray-800 text-xs">
          <span class="text-emerald-400 font-semibold">{{ seg.label }}</span>
          <span class="text-gray-500">{{ seg.text.split(' ').length }} mots</span>
        </div>
        <div class="px-3 py-2 text-gray-200 text-xs leading-relaxed bg-gray-900">
          {{ seg.text }}
        </div>
        @if (seg.summaryLoading) {
          <div class="px-3 py-2 bg-orange-950/40 border-t border-orange-800 flex items-center gap-2">
            <div class="w-3 h-3 border border-orange-500 border-t-transparent rounded-full animate-spin shrink-0"></div>
            <span class="text-orange-400 text-xs">Groq génère le résumé...</span>
          </div>
        } @else if (seg.aiSummary) {
          <div class="px-3 py-2 bg-emerald-950/30 border-t border-emerald-800">
            <div class="text-emerald-400 text-xs font-semibold mb-1">🤖 Résumé Groq</div>
            <p class="text-emerald-100 text-xs leading-relaxed">{{ seg.aiSummary }}</p>
          </div>
        }
      </div>
    }

    <!-- Live current segment -->
    @if (isRecording || currentSegmentText || interimText) {
      <div class="rounded-xl border border-red-800 overflow-hidden">
        <div class="flex items-center gap-2 px-3 py-1.5 bg-gray-800 text-xs">
          @if (isRecording) {
            <span class="w-2 h-2 rounded-full bg-red-500 animate-ping shrink-0"></span>
          }
          <span class="text-red-400 font-semibold">En cours — {{ currentSegmentLabel }}</span>
        </div>
        <div class="px-3 py-2 bg-gray-900 text-xs leading-relaxed min-h-[3rem]">
          <span class="text-gray-200">{{ currentSegmentText }}</span>
          @if (interimText) {
            <span class="text-gray-500 italic"> {{ interimText }}</span>
          }
          @if (isRecording) {
            <span class="inline-block w-0.5 h-3.5 bg-red-400 ml-0.5 align-middle blink-cursor"></span>
          }
        </div>
      </div>
    }

  </div>

  <!-- ══ FOOTER ══ -->
  @if (isRecording) {
    <div class="px-3 py-2 bg-gray-800 border-t border-gray-700 flex items-center justify-between text-xs shrink-0">
      <span class="text-gray-400">
        Prochain résumé dans
        <span class="text-orange-400 font-mono font-semibold">{{ nextSummaryCountdown }}</span>
      </span>
      <button (click)="forceSegmentSummary()"
        class="text-orange-400 hover:text-orange-300 text-xs underline underline-offset-2 select-none">
        Résumer maintenant
      </button>
    </div>
  }

</div>

<style>
  .blink-cursor { animation: blink 1s step-end infinite; }
  @keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0; } }
</style>
  `,
})
export class LiveTranscriptComponent implements OnInit, OnDestroy {

  @Input() meetingId!: number;
  @Input() meetingEnded = false;
  @Input() patientName = '';
  @Input() doctorName = '';
  @Output() transcriptChange = new EventEmitter<string>();

  private meetingService = inject(MeetingService);
  private zone           = inject(NgZone);
  private cdr            = inject(ChangeDetectorRef);

  // ── State ──────────────────────────────────────────────────────────────────
  isRecording      = false;
  browserSupported = false;
  permissionDenied = false;
  debugInfo        = '';

  selectedLang       = 'fr-FR';
  summaryIntervalMin = 3;

  fullTranscript     = '';
  currentSegmentText = '';
  interimText        = '';
  segments: TranscriptSegment[] = [];

  lastSaveTime = '';
  saveError    = false;
  copyText     = '📋 Copier';

  nextSummaryCountdown = '';

  get hasContent(): boolean {
    return !!(this.fullTranscript || this.currentSegmentText.trim());
  }

  get wordCount(): number {
    return (this.fullTranscript + ' ' + this.currentSegmentText)
      .trim().split(/\s+/).filter(Boolean).length;
  }

  // ── Private ────────────────────────────────────────────────────────────────
  private recognition: any     = null;
  private autoSaveTimeout: any = null;
  private countdownInterval: any = null;
  private restartTimeout: any  = null;
  private segmentStartTime: Date  = new Date();
  private meetingStartTime: Date  = new Date();
  private segmentIndex = 0;

  // ── Lifecycle ──────────────────────────────────────────────────────────────

  ngOnInit(): void {
    const SR = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    this.browserSupported = !!SR;
    if (!SR) {
      console.warn('[LiveTranscript] Web Speech API not supported');
      return;
    }
    this.initRecognition(SR);
  }

  ngOnDestroy(): void {
    this.isRecording = false;
    if (this.recognition) {
      try { this.recognition.abort(); } catch (_) {}
    }
    this.stopCountdown();
    if (this.autoSaveTimeout) clearTimeout(this.autoSaveTimeout);
    if (this.restartTimeout)  clearTimeout(this.restartTimeout);
  }

  // ── Init ───────────────────────────────────────────────────────────────────

  private initRecognition(SR: any): void {
    this.recognition = new SR();
    this.recognition.continuous      = true;
    this.recognition.interimResults  = true;
    this.recognition.lang            = this.selectedLang;
    this.recognition.maxAlternatives = 1;

    // *** KEY FIX: wrap all callbacks in zone.run() ***
    this.recognition.onresult = (event: any) => {
      this.zone.run(() => {
        let interim = '';
        for (let i = event.resultIndex; i < event.results.length; i++) {
          const result = event.results[i];
          if (result.isFinal) {
            const text = result[0].transcript;
            console.log('[LiveTranscript] Final result:', text);
            this.currentSegmentText += text + ' ';
            this.fullTranscript = this.buildFullTranscript();
            this.scheduleAutoSave();
            this.transcriptChange.emit(this.fullTranscript);
          } else {
            interim += result[0].transcript;
          }
        }
        this.interimText = interim;
        // cdr.detectChanges() is NOT needed here because zone.run() triggers it
      });
    };

    this.recognition.onstart = () => {
      this.zone.run(() => {
        console.log('[LiveTranscript] Started');
        this.debugInfo       = '';
        this.permissionDenied = false;
      });
    };

    this.recognition.onerror = (event: any) => {
      this.zone.run(() => {
        console.warn('[LiveTranscript] Error:', event.error);
        switch (event.error) {
          case 'not-allowed':
          case 'permission-denied':
            this.permissionDenied = true;
            this.isRecording      = false;
            this.stopCountdown();
            this.debugInfo = '';
            break;
          case 'no-speech':
            // Non-fatal — browser will continue listening
            break;
          case 'network':
            this.debugInfo = '⚠ Erreur réseau (Google Speech nécessite Internet)';
            break;
          case 'audio-capture':
            this.debugInfo = '⚠ Aucun microphone détecté';
            this.isRecording = false;
            this.stopCountdown();
            break;
          default:
            this.debugInfo = `⚠ Erreur: ${event.error}`;
        }
      });
    };

    this.recognition.onend = () => {
      this.zone.run(() => {
        console.log('[LiveTranscript] Ended. isRecording=', this.isRecording);
        // Chrome stops recognition after ~60s silence even in continuous mode — restart it
        if (this.isRecording) {
          if (this.restartTimeout) clearTimeout(this.restartTimeout);
          this.restartTimeout = setTimeout(() => {
            if (this.isRecording) {
              try {
                this.recognition.lang = this.selectedLang;
                this.recognition.start();
                console.log('[LiveTranscript] Auto-restarted');
              } catch (e) {
                console.warn('[LiveTranscript] Restart failed:', e);
              }
            }
          }, 300);
        }
      });
    };
  }

  // ── Recording ──────────────────────────────────────────────────────────────

  toggleRecording(): void {
    if (this.isRecording) {
      this.stopRecording();
    } else {
      this.startRecording();
    }
  }

  private startRecording(): void {
    if (!this.recognition) return;
    this.permissionDenied  = false;
    this.debugInfo         = '';
    this.isRecording       = true;
    this.segmentStartTime  = new Date();
    if (this.segments.length === 0) {
      this.meetingStartTime = new Date();
    }
    this.currentSegmentText = '';
    this.interimText        = '';
    this.recognition.lang   = this.selectedLang;

    // Start recognition OUTSIDE Angular zone to avoid CD overhead on every audio frame
    this.zone.runOutsideAngular(() => {
      try {
        this.recognition.start();
      } catch (e: any) {
        this.zone.run(() => {
          console.error('[LiveTranscript] Start failed:', e);
          this.isRecording = false;
          this.debugInfo   = `Erreur au démarrage: ${e?.message ?? e}`;
        });
      }
    });

    this.startCountdown();
  }

  private stopRecording(): void {
    this.isRecording = false;
    this.stopCountdown();
    if (this.restartTimeout) clearTimeout(this.restartTimeout);

    this.zone.runOutsideAngular(() => {
      try { this.recognition.stop(); } catch (_) {}
    });

    if (this.currentSegmentText.trim()) {
      this.flushCurrentSegment(false);
    }
    this.interimText = '';
  }

  onLangChange(lang: string): void {
    this.selectedLang = lang;
    if (this.recognition) this.recognition.lang = lang;
  }

  retryPermission(): void {
    this.permissionDenied = false;
    this.debugInfo        = '';
    this.startRecording();
  }

  // ── Segment ────────────────────────────────────────────────────────────────

  forceSegmentSummary(): void {
    if (!this.currentSegmentText.trim()) return;
    const prevStart = this.segmentStartTime;
    this.flushCurrentSegment(true);
    this.segmentStartTime = new Date();
    this.startCountdown();
  }

  private flushCurrentSegment(withAiSummary: boolean): void {
    this.segmentIndex++;
    const now   = new Date();
    const label = `Segment ${this.segmentIndex} (${this.fmtElapsed(this.segmentStartTime)}–${this.fmtElapsed(now)})`;

    const seg: TranscriptSegment = {
      index: this.segmentIndex,
      label,
      text:           this.currentSegmentText.trim(),
      aiSummary:      null,
      summaryLoading: withAiSummary,
      startedAt:      this.segmentStartTime,
    };
    this.segments.push(seg);
    this.currentSegmentText = '';
    this.fullTranscript     = this.buildFullTranscript();

    this.meetingService.saveTranscript(
      this.meetingId,
      this.fullTranscript,
      withAiSummary,
      label,
    ).subscribe({
      next: (result) => {
        // Already inside Angular zone (HttpClient runs in zone)
        seg.aiSummary      = result.summary ?? null;
        seg.summaryLoading = false;
        this.lastSaveTime  = this.formatTime(new Date());
        this.saveError     = false;
      },
      error: () => {
        seg.summaryLoading = false;
        this.saveError     = true;
      },
    });
  }

  // ── Auto-save ──────────────────────────────────────────────────────────────

  private scheduleAutoSave(): void {
    if (this.autoSaveTimeout) clearTimeout(this.autoSaveTimeout);
    this.autoSaveTimeout = setTimeout(() => {
      const text = this.buildFullTranscript();
      if (!text) return;
      this.meetingService.saveTranscript(this.meetingId, text, false, 'auto-save')
        .subscribe({
          next: () => {
            this.lastSaveTime = this.formatTime(new Date());
            this.saveError    = false;
          },
          error: () => { this.saveError = true; },
        });
    }, 8000);
  }

  // ── Countdown ──────────────────────────────────────────────────────────────

  private startCountdown(): void {
    this.stopCountdown();
    this.segmentStartTime = new Date();
    this.updateCountdown();
    this.countdownInterval = setInterval(() => this.updateCountdown(), 1000);
  }

  private stopCountdown(): void {
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
      this.countdownInterval = null;
    }
    this.nextSummaryCountdown = '';
  }

  private updateCountdown(): void {
    const elapsed   = (Date.now() - this.segmentStartTime.getTime()) / 1000;
    const totalSec  = this.summaryIntervalMin * 60;
    const remaining = Math.max(0, totalSec - elapsed);

    if (remaining <= 0 && this.isRecording) {
      this.forceSegmentSummary();
      return;
    }

    const m = Math.floor(remaining / 60);
    const s = Math.floor(remaining % 60);
    this.nextSummaryCountdown = `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  }

  // ── Utils ──────────────────────────────────────────────────────────────────

  private buildFullTranscript(): string {
    const segTexts = this.segments.map(s => `[${s.label}]\n${s.text}`).join('\n\n');
    const current  = this.currentSegmentText.trim()
      ? `[${this.currentSegmentLabel}]\n${this.currentSegmentText.trim()}`
      : '';
    return [segTexts, current].filter(Boolean).join('\n\n');
  }

  get currentSegmentLabel(): string {
    const now = new Date();
    return `Segment ${this.segmentIndex + 1} (${this.fmtElapsed(this.segmentStartTime)}–${this.fmtElapsed(now)})`;
  }

  /** Elapsed time from meeting start */
  private fmtElapsed(d: Date): string {
    const ref      = this.meetingStartTime;
    const totalSec = Math.max(0, Math.floor((d.getTime() - ref.getTime()) / 1000));
    const m        = Math.floor(totalSec / 60);
    const s        = totalSec % 60;
    return `${m}:${String(s).padStart(2, '0')}`;
  }

  private formatTime(d: Date): string {
    return d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  }

  // ── Copy / Export ──────────────────────────────────────────────────────────

  copyTranscript(): void {
    navigator.clipboard.writeText(this.buildFullTranscript()).then(() => {
      this.copyText = '✅ Copié !';
      setTimeout(() => (this.copyText = '📋 Copier'), 2000);
    });
  }

  exportTranscript(): void {
    const lines: string[] = [
      'TRANSCRIPTION — Réunion médicale Tfakkarni',
      `Patient: ${this.patientName}  |  Médecin: Dr. ${this.doctorName}`,
      `Exporté: ${new Date().toLocaleString('fr-FR')}`,
      '─'.repeat(60), '',
      this.buildFullTranscript(),
    ];
    if (this.segments.some(s => s.aiSummary)) {
      lines.push('', '─'.repeat(60), 'RÉSUMÉS GROQ PAR SEGMENT', '─'.repeat(60));
      this.segments.filter(s => s.aiSummary).forEach(s => {
        lines.push('', s.label, s.aiSummary!);
      });
    }
    const blob = new Blob([lines.join('\n')], { type: 'text/plain;charset=utf-8' });
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement('a');
    a.href     = url;
    a.download = `transcript_${this.patientName.replace(/\s/g, '_')}_${Date.now()}.txt`;
    a.click();
    URL.revokeObjectURL(url);
  }

  clearTranscript(): void {
    if (!confirm('Effacer tout le transcript ?')) return;
    this.segments           = [];
    this.fullTranscript     = '';
    this.currentSegmentText = '';
    this.interimText        = '';
    this.segmentIndex       = 0;
    this.lastSaveTime       = '';
  }
}
