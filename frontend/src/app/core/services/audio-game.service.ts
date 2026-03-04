import { Injectable, signal, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { environment } from '@/environments/environment';

export type SpeechLanguage = 'en' | 'tn';

const LANGUAGE_STORAGE_KEY = 'tfk_language';
const GENDER_STORAGE_KEY = 'tfk_gender';

export interface AudioGenerateRequest {
  originalText?: string;
  targetLanguageCode: string;
  voiceId?: string;
  gameType: string;           // PHOTO | PLACE | MOVIE | QUESTION
  patientName?: string;
  patientGender?: string;     // male | female
}

@Injectable({
  providedIn: 'root',
})
export class AudioGameService {
  private readonly http = inject(HttpClient);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser = isPlatformBrowser(this.platformId);

  private readonly apiUrl = `${environment.apiBaseUrl}/api/games/audio`;

  /** Whether audio is currently playing */
  readonly isPlaying = signal(false);

  /** Whether audio is being fetched from the API */
  readonly audioLoading = signal(false);

  /** Error message for the last failed TTS attempt */
  readonly audioError = signal('');

  /** The current Audio element reference for controlling playback */
  private currentAudio: HTMLAudioElement | null = null;

  /** Cached audio blob for replay */
  private cachedBlob: Blob | null = null;

  // ── Language preference ────────────────────────────────────────────────────

  getPreferredLanguage(): SpeechLanguage {
    if (!this.isBrowser) return 'en';
    const stored = localStorage.getItem(LANGUAGE_STORAGE_KEY);
    return stored === 'tn' ? 'tn' : 'en';
  }

  setPreferredLanguage(lang: SpeechLanguage): void {
    if (!this.isBrowser) return;
    localStorage.setItem(LANGUAGE_STORAGE_KEY, lang);
  }

  // ── Gender cache ───────────────────────────────────────────────────────────

  getCachedGender(): string {
    if (!this.isBrowser) return 'male';
    return localStorage.getItem(GENDER_STORAGE_KEY) || 'male';
  }

  setCachedGender(gender: string): void {
    if (!this.isBrowser) return;
    localStorage.setItem(GENDER_STORAGE_KEY, gender);
  }

  // ── API calls ──────────────────────────────────────────────────────────────

  /**
   * Send text + config to the backend, get audio/mpeg blob back.
   */
  generateQuestionAudio(request: AudioGenerateRequest): Observable<Blob> {
    return this.http.post(`${this.apiUrl}/generate-question`, request, {
      responseType: 'blob',
    }).pipe(
      catchError(err => {
        console.error('[AudioGameService] TTS generation failed:', err);
        return throwError(() => new Error('Failed to generate audio'));
      }),
    );
  }

  // ── Playback ───────────────────────────────────────────────────────────────

  /**
   * Play an audio blob. Automatically stops any currently playing audio.
   * Returns the HTMLAudioElement for external control.
   */
  playAudio(blob: Blob): HTMLAudioElement {
    if (!this.isBrowser) return new Audio();

    this.stopAudio();
    this.cachedBlob = blob;

    const url = URL.createObjectURL(blob);
    const audio = new Audio(url);

    audio.onplay = () => this.isPlaying.set(true);
    audio.onended = () => {
      this.isPlaying.set(false);
      URL.revokeObjectURL(url);
    };
    audio.onerror = () => {
      this.isPlaying.set(false);
      URL.revokeObjectURL(url);
    };

    audio.play().catch(err => {
      console.warn('[AudioGameService] Auto-play blocked:', err);
      this.isPlaying.set(false);
    });

    this.currentAudio = audio;
    return audio;
  }

  /**
   * Replay the last fetched audio blob.
   */
  replayAudio(): void {
    if (this.cachedBlob) {
      this.playAudio(this.cachedBlob);
    }
  }

  /**
   * Stop any currently playing audio.
   */
  stopAudio(): void {
    if (this.currentAudio) {
      this.currentAudio.pause();
      this.currentAudio.currentTime = 0;
      this.currentAudio = null;
    }
    this.isPlaying.set(false);
  }

  /**
   * Clear cache (e.g., when switching questions).
   */
  clearCache(): void {
    this.cachedBlob = null;
    this.audioError.set('');
  }

  /**
   * High-level helper: fetch TTS audio and auto-play it.
   * Sets loading/error signals automatically.
   */
  fetchAndPlay(request: AudioGenerateRequest): void {
    this.audioLoading.set(true);
    this.audioError.set('');
    this.clearCache();

    this.generateQuestionAudio(request).subscribe({
      next: (blob) => {
        this.audioLoading.set(false);
        this.playAudio(blob);
      },
      error: (err) => {
        this.audioLoading.set(false);
        this.audioError.set(err.message || 'Audio generation failed');
        console.error('[AudioGameService] fetchAndPlay error:', err);
      },
    });
  }
}
