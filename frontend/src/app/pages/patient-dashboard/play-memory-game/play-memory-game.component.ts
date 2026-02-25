import { Component, OnInit, OnDestroy, signal, computed, inject, DestroyRef, ViewChild, ElementRef, PLATFORM_ID, NgZone } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, of, tap } from 'rxjs';
import { environment } from '@/environments/environment';
import { AuthService } from '@/core/auth/auth.service';
import { AudioGameService, type SpeechLanguage } from '@/core/services/audio-game.service';

import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardBadgeComponent } from '@/shared/components/badge';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardProgressBarComponent } from '@/shared/components/progress-bar';

import {
  CustomGameService,
  type UnifiedPlayData,
  type UnifiedPlayItem,
  type UnifiedPlayResult,
  type AnswerEntry,
} from '@/core/services/custom-game.service';

/** Dynamically load the Google Maps JS API (once) */
function loadGoogleMapsApi(apiKey: string): Promise<void> {
  return new Promise((resolve, reject) => {
    if (typeof google !== 'undefined' && google.maps) { resolve(); return; }
    const existing = document.getElementById('google-maps-script');
    if (existing) { existing.addEventListener('load', () => resolve()); return; }
    const script = document.createElement('script');
    script.id = 'google-maps-script';
    script.src = `https://maps.googleapis.com/maps/api/js?key=${apiKey}&libraries=streetView`;
    script.async = true; script.defer = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error('Failed to load Google Maps API'));
    document.head.appendChild(script);
  });
}

type Phase = 'loading' | 'playing' | 'revealed' | 'results';

interface RevealedState {
  playerAnswer: string;
  correctAnswer: string;
  isCorrect: boolean | null;   // null = not yet assessed
  skipped: boolean;
}

@Component({
  selector: 'app-play-memory-game',
  standalone: true,
  imports: [CommonModule, FormsModule, ZardCardComponent, ZardIconComponent, ZardBadgeComponent, ZardButtonComponent, ZardProgressBarComponent],
  template: `
    <div class="min-h-screen bg-background text-foreground">
      <!-- Header -->
      <header class="border-b border-border px-6 py-4 flex items-center justify-between">
        <div class="flex items-center gap-3">
          <button z-button zType="ghost" zSize="sm" (click)="goBack()">
            <z-icon zType="arrow-left" class="mr-1" /> Back
          </button>
          <h1 class="text-xl font-bold text-primary">
            {{ playData()?.title || 'Loading...' }}
          </h1>
        </div>
        @if (phase() === 'playing' || phase() === 'revealed') {
          <div class="flex items-center gap-4">
            <z-badge zType="secondary">
              {{ currentIndex() + 1 }} / {{ playData()!.totalQuestions }}
            </z-badge>
            <span class="text-sm text-muted-foreground flex items-center gap-1">
              <z-icon zType="clock" />
              {{ formatTime(elapsedSeconds()) }}
            </span>
          </div>
        }
      </header>

      <main class="container mx-auto p-8 max-w-4xl">
        @switch (phase()) {
          @case ('loading') {
            <div class="flex flex-col items-center justify-center min-h-[400px]">
              <z-icon zType="loader-2" class="h-12 w-12 animate-spin text-primary mb-4" />
              <p class="text-muted-foreground">Loading game...</p>
            </div>
          }

          @case ('playing') {
            @if (currentItem(); as item) {
              <z-progress-bar [progress]="progressPercent()" class="mb-8 h-2" />

              @switch (item.type) {
                <!-- ═══ PHOTO ═══ -->
                @case ('PHOTO') {
                  <z-card class="p-0 overflow-hidden mb-8">
                    @if (item.imageBase64 && item.imageContentType) {
                      <img [src]="'data:' + item.imageContentType + ';base64,' + item.imageBase64"
                        alt="Who is this?" class="w-full h-80 object-contain bg-muted" />
                    }
                    <div class="p-6 text-center">
                      <div class="flex items-center justify-center gap-3 mb-2">
                        <h2 class="text-2xl font-bold">Who is this?</h2>
                        <button (click)="replayTtsAudio()" class="p-2 rounded-full hover:bg-muted transition-colors" title="Listen">
                          @if (audioGameService.audioLoading()) {
                            <z-icon zType="loader-2" class="h-6 w-6 animate-spin text-primary" />
                          } @else if (audioGameService.isPlaying()) {
                            <span class="text-2xl">🔊</span>
                          } @else {
                            <span class="text-2xl">🔈</span>
                          }
                        </button>
                      </div>
                      <p class="text-muted-foreground">Type the name of this person</p>
                    </div>
                  </z-card>
                }

                <!-- ═══ PLACE ═══ -->
                @case ('PLACE') {
                  <z-card class="p-0 overflow-hidden mb-8">
                    <!-- Street View -->
                    <div class="relative">
                      <div #streetViewContainer
                        class="w-full h-[350px] bg-muted"></div>
                      @if (panoramaLoading()) {
                        <div class="absolute inset-0 flex flex-col items-center justify-center bg-muted">
                          <z-icon zType="loader-2" class="h-10 w-10 animate-spin text-primary mb-3" />
                          <p class="text-muted-foreground">Loading Street View...</p>
                        </div>
                      }
                      @if (streetViewUnavailable()) {
                        <div class="absolute inset-0 flex flex-col items-center justify-center bg-muted">
                          <span class="text-5xl mb-3">🗺️</span>
                          <p class="font-medium text-muted-foreground">Street View not available here</p>
                          <p class="text-sm text-muted-foreground mt-1">Try to guess from the hint!</p>
                        </div>
                      }
                    </div>
                    @if (!streetViewUnavailable() && !panoramaLoading()) {
                      <div class="bg-muted/50 border-b border-border px-4 py-2 text-center text-sm text-muted-foreground">
                        👆 Drag to look around the Street View
                      </div>
                    }
                    <div class="p-6 text-center">
                      <div class="flex items-center justify-center gap-3 mb-2">
                        <h2 class="text-2xl font-bold">Can you name this place?</h2>
                        <button (click)="replayTtsAudio()" class="p-2 rounded-full hover:bg-muted transition-colors" title="Listen">
                          @if (audioGameService.audioLoading()) {
                            <z-icon zType="loader-2" class="h-6 w-6 animate-spin text-primary" />
                          } @else if (audioGameService.isPlaying()) {
                            <span class="text-2xl">🔊</span>
                          } @else {
                            <span class="text-2xl">🔈</span>
                          }
                        </button>
                      </div>
                      @if (item.hint) {
                        <p class="text-muted-foreground italic">"{{ item.hint }}"</p>
                      }
                    </div>
                  </z-card>
                }

                <!-- ═══ MOVIE ═══ -->
                @case ('MOVIE') {
                  <z-card class="p-0 overflow-hidden mb-8">
                    @if (item.posterUrl) {
                      <img [src]="item.posterUrl"
                        alt="Movie poster" class="w-full h-80 object-contain bg-muted" />
                    }
                    <div class="p-6 text-center">
                      <div class="flex items-center justify-center gap-3 mb-2">
                        <h2 class="text-2xl font-bold">Name a character from this movie</h2>
                        <button (click)="replayTtsAudio()" class="p-2 rounded-full hover:bg-muted transition-colors" title="Listen">
                          @if (audioGameService.audioLoading()) {
                            <z-icon zType="loader-2" class="h-6 w-6 animate-spin text-primary" />
                          } @else if (audioGameService.isPlaying()) {
                            <span class="text-2xl">🔊</span>
                          } @else {
                            <span class="text-2xl">🔈</span>
                          }
                        </button>
                      </div>
                      <p class="text-muted-foreground">{{ item.movieTitle }}</p>
                    </div>
                  </z-card>
                }

                <!-- ═══ QUESTION ═══ -->
                @case ('QUESTION') {
                  <z-card class="p-6 mb-8">
                    <div class="text-center">
                      <span class="text-5xl mb-4 block">🧠</span>
                      <div class="flex items-center justify-center gap-3 mb-4">
                        <h2 class="text-2xl font-bold">{{ item.questionText }}</h2>
                        <button (click)="replayTtsAudio()" class="p-2 rounded-full hover:bg-muted transition-colors" title="Listen">
                          @if (audioGameService.audioLoading()) {
                            <z-icon zType="loader-2" class="h-6 w-6 animate-spin text-primary" />
                          } @else if (audioGameService.isPlaying()) {
                            <span class="text-2xl">🔊</span>
                          } @else {
                            <span class="text-2xl">🔈</span>
                          }
                        </button>
                      </div>
                      <p class="text-muted-foreground">Type your answer below</p>
                    </div>
                  </z-card>
                }
              }

              <!-- Unified answer input for ALL types -->
              <div class="max-w-md mx-auto">
                <input type="text" [(ngModel)]="answerInput" placeholder="Type your answer..."
                  class="w-full rounded-xl border-2 border-border bg-background px-6 py-4 text-lg text-center focus:border-primary focus:outline-none transition-colors"
                  (keydown.enter)="submitAnswer()" />
                <div class="flex gap-3 mt-4">
                  <button z-button zType="outline" class="flex-1" (click)="skipAnswer()">
                    <z-icon zType="chevron-right" class="mr-2 h-4 w-4" />
                    I don't know
                  </button>
                  <button z-button class="flex-1" [disabled]="!answerInput.trim()" (click)="submitAnswer()">
                    <z-icon zType="check" class="mr-2 h-4 w-4" />
                    Submit Answer
                  </button>
                </div>
              </div>
            }
          }

          <!-- ═══ REVEALED — feedback after answer ═══ -->
          @case ('revealed') {
            @if (currentItem(); as item) {
              @if (revealedState(); as rev) {
                <z-progress-bar [progress]="progressPercent()" class="mb-8 h-2" />

                <!-- Show the question context again -->
                @switch (item.type) {
                  @case ('PHOTO') {
                    <z-card class="p-0 overflow-hidden mb-6">
                      @if (item.imageBase64 && item.imageContentType) {
                        <img [src]="'data:' + item.imageContentType + ';base64,' + item.imageBase64"
                          alt="Photo" class="w-full h-60 object-contain bg-muted" />
                      }
                    </z-card>
                  }
                  @case ('MOVIE') {
                    <z-card class="p-0 overflow-hidden mb-6">
                      @if (item.posterUrl) {
                        <img [src]="item.posterUrl"
                          alt="Movie poster" class="w-full h-60 object-contain bg-muted" />
                      }
                      <div class="p-4 text-center">
                        <p class="text-muted-foreground">{{ item.movieTitle }}</p>
                      </div>
                    </z-card>
                  }
                  @case ('PLACE') {
                    <z-card class="p-0 overflow-hidden mb-6">
                      <div #streetViewRevealContainer
                        class="w-full h-[250px] bg-muted"></div>
                      <div class="p-4 text-center">
                        @if (item.hint) {
                          <p class="text-muted-foreground italic">"{{ item.hint }}"</p>
                        }
                      </div>
                    </z-card>
                  }
                  @case ('QUESTION') {
                    <z-card class="p-4 mb-6 text-center">
                      <span class="text-4xl block mb-2">🧠</span>
                      <p class="font-semibold">{{ item.questionText }}</p>
                    </z-card>
                  }
                }

                <!-- Result feedback card -->
                <z-card class="p-8 text-center mb-6">
                  @if (rev.skipped) {
                    <span class="text-5xl mb-4 block">⏭️</span>
                    <h2 class="text-2xl font-bold text-muted-foreground mb-2">Skipped</h2>
                    <p class="text-muted-foreground mb-4">You chose to skip this one</p>

                    <div class="bg-muted rounded-xl p-6 mb-6">
                      <p class="text-sm text-muted-foreground mb-1">Correct answer</p>
                      <p class="text-2xl font-bold text-primary">{{ rev.correctAnswer || '—' }}</p>
                    </div>

                    <button z-button class="min-w-[200px]" (click)="nextAfterReveal()">
                      {{ isLastItem() ? 'See Results' : 'Next Question' }}
                      <z-icon zType="chevron-right" class="ml-2 h-4 w-4" />
                    </button>
                  } @else if (rev.isCorrect === null) {
                    <!-- Awaiting self-assessment -->
                    <span class="text-5xl mb-4 block">🤔</span>
                    <h2 class="text-2xl font-bold mb-4">Compare your answer</h2>

                    <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-6">
                      <div class="bg-muted rounded-xl p-5">
                        <p class="text-sm text-muted-foreground mb-1">Your answer</p>
                        <p class="text-xl font-bold">{{ rev.playerAnswer }}</p>
                      </div>
                      <div class="bg-primary/10 rounded-xl p-5 border border-primary/20">
                        <p class="text-sm text-muted-foreground mb-1">Expected answer</p>
                        <p class="text-xl font-bold text-primary">{{ rev.correctAnswer || '—' }}</p>
                      </div>
                    </div>

                    <p class="text-muted-foreground mb-4">Were you correct? (different language or spelling still counts!)</p>

                    <div class="flex gap-4 justify-center">
                      <button z-button zType="outline" class="min-w-[140px] border-red-300 text-red-600 hover:bg-red-50" (click)="selfAssess(false)">
                        ❌ I was wrong
                      </button>
                      <button z-button class="min-w-[140px] bg-green-600 hover:bg-green-700" (click)="selfAssess(true)">
                        ✅ I was correct
                      </button>
                    </div>
                  } @else if (rev.isCorrect) {
                    <span class="text-5xl mb-4 block">✅</span>
                    <h2 class="text-2xl font-bold text-green-600 mb-2">Correct!</h2>
                    <p class="text-muted-foreground mb-6">Your answer: <strong class="text-green-600">{{ rev.playerAnswer }}</strong></p>

                    <div class="bg-muted rounded-xl p-6 mb-6">
                      <p class="text-sm text-muted-foreground mb-1">Expected answer</p>
                      <p class="text-2xl font-bold text-primary">{{ rev.correctAnswer || '—' }}</p>
                    </div>

                    <button z-button class="min-w-[200px]" (click)="nextAfterReveal()">
                      {{ isLastItem() ? 'See Results' : 'Next Question' }}
                      <z-icon zType="chevron-right" class="ml-2 h-4 w-4" />
                    </button>
                  } @else {
                    <span class="text-5xl mb-4 block">❌</span>
                    <h2 class="text-2xl font-bold text-red-500 mb-2">Incorrect</h2>
                    <p class="text-muted-foreground mb-6">Your answer: <strong class="text-red-500">{{ rev.playerAnswer }}</strong></p>

                    <div class="bg-muted rounded-xl p-6 mb-6">
                      <p class="text-sm text-muted-foreground mb-1">Correct answer</p>
                      <p class="text-2xl font-bold text-primary">{{ rev.correctAnswer || '—' }}</p>
                    </div>

                    <button z-button class="min-w-[200px]" (click)="nextAfterReveal()">
                      {{ isLastItem() ? 'See Results' : 'Next Question' }}
                      <z-icon zType="chevron-right" class="ml-2 h-4 w-4" />
                    </button>
                  }
                </z-card>
              }
            }
          }

          <!-- ═══ RESULTS ═══ -->
          @case ('results') {
            @if (results(); as r) {
              <div class="max-w-2xl mx-auto">
                <!-- Score banner -->
                <z-card class="p-8 text-center mb-8">
                  <span class="text-6xl mb-6 block">
                    {{ r.percentage >= 80 ? '🎉' : r.percentage >= 50 ? '👍' : '💪' }}
                  </span>
                  <h2 class="text-3xl font-bold mb-2">
                    {{ r.percentage >= 80 ? 'Excellent!' : r.percentage >= 50 ? 'Good job!' : 'Keep trying!' }}
                  </h2>
                  <p class="text-muted-foreground mb-6">
                    You scored {{ r.score }} out of {{ r.totalQuestions }}
                  </p>
                  <z-progress-bar [progress]="r.percentage" class="mb-4 h-4" />
                  <p class="text-2xl font-bold text-primary mb-2">{{ r.percentage | number:'1.0-0' }}%</p>
                  <p class="text-sm text-muted-foreground">
                    Completed in {{ formatTime(r.durationSeconds || 0) }}
                  </p>
                </z-card>

                <!-- Detailed per-item results -->
                @if (r.results && r.results.length > 0) {
                  <h3 class="text-xl font-semibold mb-4">Detailed Results</h3>
                  <div class="space-y-3 mb-8">
                    @for (item of r.results; track $index; let i = $index) {
                      <z-card class="p-4">
                        <div class="flex items-start gap-4">
                          <!-- Number badge -->
                          <div class="w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold flex-shrink-0"
                               [class]="item.correct ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'">
                            {{ i + 1 }}
                          </div>
                          <!-- Details -->
                          <div class="flex-1 min-w-0">
                            <div class="flex items-center gap-2 mb-1">
                              <span class="text-xs px-2 py-0.5 rounded-full bg-muted text-muted-foreground">
                                {{ getTypeEmoji(item.type) }} {{ item.type }}
                              </span>
                              @if (item.correct) {
                                <z-badge zType="secondary" class="text-green-600">Correct</z-badge>
                              } @else if (item.selectedAnswer === 'I don\\'t know' || !item.selectedAnswer) {
                                <z-badge zType="secondary" class="text-yellow-600">Skipped</z-badge>
                              } @else {
                                <z-badge zType="secondary" class="text-red-500">Wrong</z-badge>
                              }
                            </div>
                            <p class="text-sm font-medium mb-1">{{ item.label }}</p>
                            <div class="text-sm">
                              @if (item.selectedAnswer === 'I don\\'t know' || !item.selectedAnswer) {
                                <p class="text-yellow-600">Skipped</p>
                              } @else {
                                <p [class]="item.correct ? 'text-green-600' : 'text-red-500'">
                                  Your answer: {{ item.selectedAnswer }}
                                </p>
                              }
                              @if (!item.correct) {
                                <p class="text-green-600 font-medium">Correct answer: {{ item.correctAnswer }}</p>
                              }
                            </div>
                          </div>
                          <!-- Status icon -->
                          <span class="text-xl flex-shrink-0">{{ item.correct ? '✅' : (item.selectedAnswer === 'I don\\'t know' || !item.selectedAnswer) ? '⏭️' : '❌' }}</span>
                        </div>
                      </z-card>
                    }
                  </div>
                }

                <!-- Actions -->
                <div class="flex justify-center gap-4">
                  <button z-button zType="outline" (click)="goBack()">
                    <z-icon zType="arrow-left" class="mr-2 h-4 w-4" /> Back to Dashboard
                  </button>
                  <button z-button (click)="replayGame()">
                    <z-icon zType="rotate-ccw" class="mr-2 h-4 w-4" /> Play Again
                  </button>
                </div>
              </div>
            }
          }
        }
      </main>
    </div>
  `,
})
export class PlayMemoryGameComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly keycloakService = inject(KeycloakService);
  private readonly authService = inject(AuthService);
  private readonly customGameService = inject(CustomGameService);
  readonly audioGameService = inject(AudioGameService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly ngZone = inject(NgZone);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser = isPlatformBrowser(this.platformId);

  @ViewChild('streetViewContainer') streetViewContainer!: ElementRef<HTMLDivElement>;
  @ViewChild('streetViewRevealContainer') streetViewRevealContainer!: ElementRef<HTMLDivElement>;

  phase = signal<Phase>('loading');
  playData = signal<UnifiedPlayData | null>(null);
  currentIndex = signal(0);
  answerInput = '';
  answers = signal<AnswerEntry[]>([]);
  results = signal<UnifiedPlayResult | null>(null);
  revealedState = signal<RevealedState | null>(null);

  // Street View
  panoramaLoading = signal(false);
  streetViewUnavailable = signal(false);
  private panorama: google.maps.StreetViewPanorama | null = null;
  private revealPanorama: google.maps.StreetViewPanorama | null = null;
  private mapsApiLoaded = false;
  private readonly apiKey = environment.googleMapsApiKey;

  // Timer
  elapsedSeconds = signal(0);
  private timerInterval: any;
  private startTime = 0;

  currentItem = computed((): UnifiedPlayItem | null => {
    const data = this.playData();
    const idx = this.currentIndex();
    return data && idx < data.items.length ? data.items[idx] : null;
  });

  progressPercent = computed(() => {
    const data = this.playData();
    if (!data) return 0;
    return ((this.currentIndex() + 1) / data.totalQuestions) * 100;
  });

  isLastItem = computed(() => {
    const data = this.playData();
    return data ? this.currentIndex() >= data.totalQuestions - 1 : false;
  });

  private gameId: string = '';
  private isRandom = false;
  private playerKeycloakId = '';

  // TTS
  private ttsLanguage: SpeechLanguage = 'en';
  private patientName = '';
  private patientGender = 'male';

  ngOnInit() {
    this.gameId = this.route.snapshot.paramMap.get('gameId') || '';
    this.isRandom = this.gameId === 'random';

    // Load TTS preferences from localStorage
    this.ttsLanguage = this.audioGameService.getPreferredLanguage();
    this.patientGender = this.audioGameService.getCachedGender();
    this.loadPatientName();

    this.loadGame();
  }

  ngOnDestroy() {
    this.destroyPanorama();
    this.stopTimer();
    this.audioGameService.stopAudio();
  }

  private loadGame() {
    this.phase.set('loading');

    this.playerKeycloakId = this.authService.getKeycloakId();
    if (!this.playerKeycloakId) {
      console.warn('[PlayMemoryGame] keycloakId is empty — Keycloak may not be initialized yet');
    }

    const obs = this.isRandom
      ? this.customGameService.getRandomPlayData(this.playerKeycloakId, 10)
      : this.customGameService.getPlayData(Number.parseInt(this.gameId, 10));

    obs.pipe(
      tap(data => {
        this.playData.set(data);
        this.currentIndex.set(0);
        this.answers.set([]);
        this.answerInput = '';
        this.revealedState.set(null);
        this.phase.set('playing');
        this.startTimer();
        // Init Street View if first item is PLACE
        this.initStreetViewForCurrentItem();
        // Auto-play TTS for the first question
        this.triggerTtsForCurrentItem();
      }),
      catchError(() => {
        this.goBack();
        return of(null);
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe();
  }

  goBack() {
    this.stopTimer();
    this.router.navigate(['/patient']);
  }

  replayGame() {
    this.stopTimer();
    this.loadGame();
  }

  // ── Submit typed answer ──

  submitAnswer() {
    const item = this.currentItem();
    if (!item || !this.answerInput.trim()) return;

    const playerAnswer = this.answerInput.trim();
    const correctAnswer = item.correctAnswer || '';

    // Destroy playing panorama before switching to revealed
    this.destroyPanorama();

    // Show both answers — do NOT auto-judge (isCorrect = null means "awaiting self-assessment")
    this.revealedState.set({ playerAnswer, correctAnswer, isCorrect: null, skipped: false });
    this.phase.set('revealed');

    // Init reveal panorama for PLACE
    if (item.type === 'PLACE' && item.latitude && item.longitude) {
      this.initStreetViewForReveal(item.latitude, item.longitude);
    }
  }

  // ── Skip / I don't know ──

  skipAnswer() {
    const item = this.currentItem();
    if (!item) return;

    const correctAnswer = item.correctAnswer || '';

    // Record as skipped
    this.answers.update(a => [...a, {
      type: item.type,
      itemId: item.itemId,
      selectedAnswer: '',
      selfAssessedCorrect: false,
    }]);

    // Destroy playing panorama before switching to revealed
    this.destroyPanorama();

    // Show feedback (skipped — no self-assessment needed)
    this.revealedState.set({ playerAnswer: '', correctAnswer, isCorrect: false, skipped: true });
    this.phase.set('revealed');

    // Init reveal panorama for PLACE
    if (item.type === 'PLACE' && item.latitude && item.longitude) {
      this.initStreetViewForReveal(item.latitude, item.longitude);
    }
  }

  // ── Self-assessment: patient decides if they were correct ──

  selfAssess(correct: boolean) {
    const item = this.currentItem();
    const rev = this.revealedState();
    if (!item || !rev) return;

    // Now record the answer with self-assessment
    this.answers.update(a => [...a, {
      type: item.type,
      itemId: item.itemId,
      selectedAnswer: rev.playerAnswer,
      selfAssessedCorrect: correct,
    }]);

    // Update revealed state with the decision
    this.revealedState.set({ ...rev, isCorrect: correct });
  }

  // ── Move to next after seeing feedback ──

  nextAfterReveal() {
    this.answerInput = '';
    this.revealedState.set(null);
    this.destroyPanorama();
    this.audioGameService.stopAudio();
    this.audioGameService.clearCache();

    if (this.isLastItem()) {
      this.finishGame();
    } else {
      this.currentIndex.update(i => i + 1);
      this.phase.set('playing');
      // Init Street View if next item is PLACE
      this.initStreetViewForCurrentItem();
      // Auto-play TTS for the next question
      this.triggerTtsForCurrentItem();
    }
  }

  // ── Finish and submit ──

  private finishGame() {
    this.stopTimer();
    const data = this.playData();
    if (!data) return;

    // Re-read keycloakId in case it wasn't available at loadGame() time
    if (!this.playerKeycloakId) {
      this.playerKeycloakId = this.authService.getKeycloakId();
    }
    if (!this.playerKeycloakId) {
      console.error('[PlayMemoryGame] Cannot submit — player keycloakId is empty');
    }

    this.customGameService.submitResults(this.playerKeycloakId, {
      gameId: data.gameId,
      score: 0, // server calculates
      totalQuestions: data.totalQuestions,
      durationSeconds: this.elapsedSeconds(),
      answers: this.answers(),
    }).pipe(
      tap(result => {
        this.results.set(result);
        this.phase.set('results');
      }),
      catchError(() => {
        // Fallback: compute results locally using self-assessment
        const localResults = this.answers().map((ans, i) => {
          const item = data.items[i];
          const correct = ans.selfAssessedCorrect === true;
          return {
            type: ans.type,
            itemId: ans.itemId,
            correct,
            correctAnswer: item?.correctAnswer || '—',
            selectedAnswer: ans.selectedAnswer || "I don't know",
            label: this.getItemLabel(item),
          };
        });
        const score = localResults.filter(r => r.correct).length;
        this.results.set({
          attemptId: 0,
          score,
          totalQuestions: data.totalQuestions,
          percentage: data.totalQuestions > 0 ? Math.round(score / data.totalQuestions * 100) : 0,
          durationSeconds: this.elapsedSeconds(),
          completedAt: new Date().toISOString(),
          results: localResults,
        });
        this.phase.set('results');
        return of(null);
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe();
  }

  private getItemLabel(item: UnifiedPlayItem | undefined): string {
    if (!item) return 'Unknown';
    switch (item.type) {
      case 'PHOTO': return item.correctAnswer || 'Photo';
      case 'PLACE': return item.correctAnswer || 'Place';
      case 'MOVIE': return item.movieTitle || 'Movie';
      case 'QUESTION': return item.questionText || 'Question';
      default: return 'Item';
    }
  }

  getTypeEmoji(type: string): string {
    const map: Record<string, string> = { PHOTO: '📷', PLACE: '📍', MOVIE: '🎬', QUESTION: '🧠' };
    return map[type] || '📄';
  }

  // ── Street View ──

  private initStreetViewForCurrentItem() {
    const item = this.currentItem();
    if (item?.type === 'PLACE' && item.latitude && item.longitude) {
      this.panoramaLoading.set(true);
      this.streetViewUnavailable.set(false);
      setTimeout(() => this.initPanorama(item.latitude!, item.longitude!, 'playing'), 0);
    }
  }

  private initStreetViewForReveal(lat: number, lng: number) {
    setTimeout(() => this.initPanorama(lat, lng, 'reveal'), 0);
  }

  private async initPanorama(lat: number, lng: number, target: 'playing' | 'reveal'): Promise<void> {
    if (!this.isBrowser) return;

    try {
      if (!this.mapsApiLoaded) {
        await loadGoogleMapsApi(this.apiKey);
        this.mapsApiLoaded = true;
      }

      const container = target === 'playing'
        ? this.streetViewContainer?.nativeElement
        : this.streetViewRevealContainer?.nativeElement;
      if (!container) { this.panoramaLoading.set(false); return; }

      const location = { lat, lng };
      const sv = new google.maps.StreetViewService();

      sv.getPanorama({ location, radius: 500 }, (data: google.maps.StreetViewPanoramaData | null, status: google.maps.StreetViewStatus) => {
        this.ngZone.run(() => {
          if (status === google.maps.StreetViewStatus.OK && data?.location?.latLng) {
            this.streetViewUnavailable.set(false);
            this.panoramaLoading.set(false);
            const pano = new google.maps.StreetViewPanorama(container, {
              position: data.location.latLng,
              pov: { heading: 0, pitch: 0 },
              zoom: 1,
              addressControl: false,
              showRoadLabels: false,
              linksControl: false,
              fullscreenControl: false,
              enableCloseButton: false,
              panControl: true,
              zoomControl: true,
              motionTracking: false,
              motionTrackingControl: false,
            });
            if (target === 'playing') { this.panorama = pano; }
            else { this.revealPanorama = pano; }
          } else {
            this.panoramaLoading.set(false);
            this.streetViewUnavailable.set(true);
          }
        });
      });
    } catch {
      this.panoramaLoading.set(false);
      this.streetViewUnavailable.set(true);
    }
  }

  private destroyPanorama() {
    if (this.panorama) { this.panorama = null; }
    if (this.revealPanorama) { this.revealPanorama = null; }
    this.panoramaLoading.set(false);
    this.streetViewUnavailable.set(false);
  }

  // ── Timer ──

  private startTimer() {
    this.startTime = Date.now();
    this.elapsedSeconds.set(0);
    this.timerInterval = setInterval(() => {
      this.elapsedSeconds.set(Math.floor((Date.now() - this.startTime) / 1000));
    }, 1000);
  }

  private stopTimer() {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }

  formatTime(seconds: number): string {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  // ── TTS ──────────────────────────────────────────────────────────────────

  /** Load patient name from Keycloak token for TTS personalization */
  private async loadPatientName(): Promise<void> {
    try {
      const fullName = await this.authService.getUsername();
      // Use first name only for the TTS greeting
      this.patientName = fullName.split(' ')[0] || '';
    } catch {
      this.patientName = '';
    }
  }

  /** Trigger TTS for the current item — auto-play audio */
  private triggerTtsForCurrentItem(): void {
    if (!this.isBrowser) return;

    const item = this.currentItem();
    if (!item) return;

    let originalText = '';
    switch (item.type) {
      case 'QUESTION':
        originalText = item.questionText || '';
        break;
      case 'PHOTO':
      case 'PLACE':
      case 'MOVIE':
        // Backend picks from fixed variants — no text needed
        break;
    }

    this.audioGameService.fetchAndPlay({
      originalText,
      targetLanguageCode: this.ttsLanguage,
      gameType: item.type,
      patientName: this.patientName,
      patientGender: this.patientGender,
    });
  }

  /** Replay the cached TTS audio (speaker button) */
  replayTtsAudio(): void {
    if (this.audioGameService.audioLoading()) return;

    // If we have cached audio, replay it; otherwise re-fetch
    if (this.audioGameService.isPlaying()) {
      this.audioGameService.stopAudio();
    } else {
      this.audioGameService.replayAudio();
    }
  }
}
