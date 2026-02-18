import { Component, OnInit, signal, computed, inject, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, of, tap } from 'rxjs';

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

type Phase = 'loading' | 'playing' | 'revealed' | 'results';

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
                      <h2 class="text-2xl font-bold mb-2">Who is this?</h2>
                      <p class="text-muted-foreground">Select the correct name</p>
                    </div>
                  </z-card>
                  <div class="grid grid-cols-2 gap-4">
                    @for (choice of item.choices; track choice) {
                      <button (click)="selectAnswer(choice)"
                        class="p-6 rounded-xl text-lg font-semibold text-center border-2 transition-all"
                        [class]="selectedAnswer() === choice
                          ? 'border-primary bg-primary/10 text-primary scale-[1.02]'
                          : 'border-border hover:border-primary/50 hover:bg-muted'">
                        {{ choice }}
                      </button>
                    }
                  </div>
                }

                <!-- ═══ PLACE ═══ -->
                @case ('PLACE') {
                  <z-card class="p-6 mb-8">
                    <div class="text-center">
                      <span class="text-5xl mb-4 block">📍</span>
                      <h2 class="text-2xl font-bold mb-2">Can you name this place?</h2>
                      @if (item.hint) {
                        <p class="text-muted-foreground italic">"{{ item.hint }}"</p>
                      }
                    </div>
                  </z-card>
                  <div class="grid grid-cols-2 gap-4">
                    @for (choice of item.choices; track choice) {
                      <button (click)="selectAnswer(choice)"
                        class="p-6 rounded-xl text-lg font-semibold text-center border-2 transition-all"
                        [class]="selectedAnswer() === choice
                          ? 'border-primary bg-primary/10 text-primary scale-[1.02]'
                          : 'border-border hover:border-primary/50 hover:bg-muted'">
                        {{ choice }}
                      </button>
                    }
                  </div>
                }

                <!-- ═══ MOVIE ═══ -->
                @case ('MOVIE') {
                  <z-card class="p-0 overflow-hidden mb-8">
                    @if (item.posterUrl) {
                      <img [src]="'https://image.tmdb.org/t/p/w400' + item.posterUrl"
                        alt="Movie poster" class="w-full h-80 object-contain bg-muted" />
                    }
                    <div class="p-6 text-center">
                      <h2 class="text-2xl font-bold mb-2">Name a character from this movie</h2>
                      <p class="text-muted-foreground">{{ item.movieTitle }}</p>
                    </div>
                  </z-card>
                  <div class="grid grid-cols-2 gap-4">
                    @for (choice of item.choices; track choice) {
                      <button (click)="selectAnswer(choice)"
                        class="p-6 rounded-xl text-lg font-semibold text-center border-2 transition-all"
                        [class]="selectedAnswer() === choice
                          ? 'border-primary bg-primary/10 text-primary scale-[1.02]'
                          : 'border-border hover:border-primary/50 hover:bg-muted'">
                        {{ choice }}
                      </button>
                    }
                  </div>
                }

                <!-- ═══ QUESTION ═══ -->
                @case ('QUESTION') {
                  <z-card class="p-6 mb-8">
                    <div class="text-center">
                      <span class="text-5xl mb-4 block">🧠</span>
                      <h2 class="text-2xl font-bold mb-4">{{ item.questionText }}</h2>
                      <p class="text-muted-foreground">Type your answer below</p>
                    </div>
                  </z-card>
                  <div class="max-w-md mx-auto">
                    <input type="text" [(ngModel)]="questionInput" placeholder="Your answer..."
                      class="w-full rounded-xl border-2 border-border bg-background px-6 py-4 text-lg text-center focus:border-primary transition-colors"
                      (keydown.enter)="revealQuestionAnswer()" />
                    <button z-button class="w-full mt-4" [disabled]="!questionInput.trim()" (click)="revealQuestionAnswer()">
                      Check Answer
                    </button>
                  </div>
                }
              }

              <!-- Next button (MCQ types) -->
              @if (item.type !== 'QUESTION' && selectedAnswer()) {
                <div class="mt-8 text-center">
                  <z-button (click)="submitAndNext()">
                    {{ isLastItem() ? 'Finish Game' : 'Next Question' }}
                    <z-icon zType="chevron-right" class="ml-2 h-4 w-4" />
                  </z-button>
                </div>
              }
            }
          }

          <!-- ═══ REVEALED (for QUESTION type) ═══ -->
          @case ('revealed') {
            @if (currentItem(); as item) {
              <z-card class="p-8 text-center mb-6">
                <span class="text-5xl mb-4 block">🧠</span>
                <h2 class="text-xl font-bold mb-2">{{ item.questionText }}</h2>
                <p class="text-muted-foreground mb-6">Your answer: <strong>{{ questionInput }}</strong></p>
                <div class="bg-muted rounded-xl p-6 mb-6">
                  <p class="text-sm text-muted-foreground mb-1">Correct answer</p>
                  <p class="text-2xl font-bold text-primary">{{ item.correctAnswer }}</p>
                </div>
                <p class="text-sm text-muted-foreground mb-4">Did you get it right?</p>
                <div class="flex justify-center gap-4">
                  <z-button variant="outline" (click)="selfAssess(false)">
                    <z-icon zType="x" class="mr-2 h-4 w-4" /> Wrong
                  </z-button>
                  <z-button (click)="selfAssess(true)">
                    <z-icon zType="check" class="mr-2 h-4 w-4" /> Correct
                  </z-button>
                </div>
              </z-card>
            }
          }

          <!-- ═══ RESULTS ═══ -->
          @case ('results') {
            @if (results(); as r) {
              <div class="max-w-lg mx-auto text-center">
                <z-card class="p-8 mb-8">
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
                  <p class="text-2xl font-bold text-primary">{{ r.percentage | number:'1.0-0' }}%</p>
                </z-card>

                <!-- Per-item results -->
                @if (r.results && r.results.length > 0) {
                  <div class="space-y-2 mb-8 text-left">
                    @for (item of r.results; track $index) {
                      <div class="flex items-center gap-3 p-3 rounded-lg bg-muted">
                        <span class="text-xl">{{ item.correct ? '✅' : '❌' }}</span>
                        <div class="flex-1 min-w-0">
                          <p class="text-sm font-medium truncate">{{ item.label }}</p>
                          @if (!item.correct && item.correctAnswer) {
                            <p class="text-xs text-muted-foreground">Correct: {{ item.correctAnswer }}</p>
                          }
                        </div>
                      </div>
                    }
                  </div>
                }

                <div class="flex justify-center gap-4">
                  <z-button variant="outline" (click)="goBack()">
                    <z-icon zType="arrow-left" class="mr-2 h-4 w-4" /> Back
                  </z-button>
                  <z-button (click)="replayGame()">
                    <z-icon zType="rotate-ccw" class="mr-2 h-4 w-4" /> Play Again
                  </z-button>
                </div>
              </div>
            }
          }
        }
      </main>
    </div>
  `,
})
export class PlayMemoryGameComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly keycloakService = inject(KeycloakService);
  private readonly customGameService = inject(CustomGameService);
  private readonly destroyRef = inject(DestroyRef);

  phase = signal<Phase>('loading');
  playData = signal<UnifiedPlayData | null>(null);
  currentIndex = signal(0);
  selectedAnswer = signal<string>('');
  questionInput = '';
  answers = signal<AnswerEntry[]>([]);
  results = signal<UnifiedPlayResult | null>(null);

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

  ngOnInit() {
    this.gameId = this.route.snapshot.paramMap.get('gameId') || '';
    this.isRandom = this.gameId === 'random';
    this.loadGame();
  }

  private loadGame() {
    this.phase.set('loading');

    const keycloakId = this.keycloakService.getKeycloakInstance()?.subject || '';

    const obs = this.isRandom
      ? this.customGameService.getRandomPlayData(keycloakId, 10)
      : this.customGameService.getPlayData(Number.parseInt(this.gameId, 10));

    obs.pipe(
      tap(data => {
        this.playData.set(data);
        this.currentIndex.set(0);
        this.answers.set([]);
        this.selectedAnswer.set('');
        this.questionInput = '';
        this.phase.set('playing');
        this.startTimer();
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

  // ── MCQ answer ──

  selectAnswer(choice: string) {
    this.selectedAnswer.set(choice);
  }

  submitAndNext() {
    const item = this.currentItem();
    if (!item || !this.selectedAnswer()) return;

    this.answers.update(a => [...a, {
      type: item.type,
      itemId: item.itemId,
      selectedAnswer: this.selectedAnswer(),
    }]);

    this.advance();
  }

  // ── Question type ──

  revealQuestionAnswer() {
    if (!this.questionInput.trim()) return;
    this.phase.set('revealed');
  }

  selfAssess(correct: boolean) {
    const item = this.currentItem();
    if (!item) return;

    this.answers.update(a => [...a, {
      type: item.type,
      itemId: item.itemId,
      selectedAnswer: this.questionInput.trim(),
      selfAssessedCorrect: correct,
    }]);

    this.questionInput = '';
    this.phase.set('playing');
    this.advance();
  }

  // ── Flow control ──

  private advance() {
    this.selectedAnswer.set('');
    if (this.isLastItem()) {
      this.finishGame();
    } else {
      this.currentIndex.update(i => i + 1);
    }
  }

  private finishGame() {
    this.stopTimer();
    const data = this.playData();
    if (!data) return;

    this.customGameService.submitResults({
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
        // Fallback: show basic results
        this.results.set({
          attemptId: 0,
          score: 0,
          totalQuestions: data.totalQuestions,
          percentage: 0,
          durationSeconds: this.elapsedSeconds(),
          completedAt: new Date().toISOString(),
          results: [],
        });
        this.phase.set('results');
        return of(null);
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe();
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
}
