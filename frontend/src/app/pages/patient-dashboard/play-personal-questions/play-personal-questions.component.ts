import {
  Component,
  OnInit,
  signal,
  inject,
  DestroyRef,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, of, tap } from 'rxjs';
import { KeycloakService } from 'keycloak-angular';

import {
  PersonalQuestionService,
  type PersonalQuestionPlayData,
} from '@/core/services/personal-question.service';

type Phase = 'loading' | 'playing' | 'revealed' | 'results';

interface QuestionResult {
  questionText: string;
  correctAnswer: string;
  patientAnswer: string;
  markedCorrect: boolean;
}

@Component({
  selector: 'app-play-personal-questions',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="min-h-screen bg-gradient-to-b from-blue-50 to-white dark:from-slate-900 dark:to-slate-800">
      <!-- Top bar -->
      <header class="sticky top-0 z-10 bg-white/80 dark:bg-slate-900/80 backdrop-blur-sm border-b border-slate-200 dark:border-slate-700 px-4 py-3 sm:px-6">
        <div class="max-w-2xl mx-auto flex items-center justify-between">
          <div class="flex items-center gap-3">
            <span class="text-2xl">🧠</span>
            <span class="text-xl font-bold text-slate-800 dark:text-white">Personal Questions</span>
          </div>
          <button (click)="goBack()"
            class="text-sm px-4 py-2 rounded-full bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-600 transition-colors">
            ← Back
          </button>
        </div>
      </header>

      <main class="max-w-2xl mx-auto px-4 sm:px-6 py-6">

        <!-- Loading -->
        @if (phase() === 'loading') {
        <div class="flex flex-col items-center justify-center py-20">
          <div class="animate-spin text-5xl mb-4">🧩</div>
          <p class="text-lg text-slate-500 dark:text-slate-400">Loading questions...</p>
        </div>
        }

        <!-- Error -->
        @if (errorMessage()) {
        <div class="text-center py-20">
          <p class="text-5xl mb-4">😟</p>
          <h2 class="text-xl font-semibold text-slate-700 dark:text-slate-300 mb-2">Something went wrong</h2>
          <p class="text-slate-500 dark:text-slate-400 mb-6">{{ errorMessage() }}</p>
          <button (click)="goBack()"
            class="px-6 py-3 rounded-2xl bg-blue-500 hover:bg-blue-600 text-white text-lg font-bold shadow-lg transition-all">
            ← Go Back
          </button>
        </div>
        }

        <!-- Playing Phase: show question, text input -->
        @if (phase() === 'playing' && currentQuestion()) {
        <div class="mb-6">
          <!-- Progress -->
          <div class="flex items-center justify-between mb-3">
            <span class="text-sm font-medium text-slate-500 dark:text-slate-400">
              Question {{ currentIndex() + 1 }} of {{ playData()!.totalQuestions }}
            </span>
            <span class="text-sm font-medium text-slate-500 dark:text-slate-400">
              ⏱️ {{ formatTime(elapsedSeconds()) }}
            </span>
          </div>
          <div class="relative w-full h-3 bg-slate-100 dark:bg-slate-700 rounded-full overflow-hidden mb-6">
            <div class="h-full bg-gradient-to-r from-blue-400 to-blue-600 rounded-full transition-all duration-300"
              [style.width.%]="progressPercent()"></div>
          </div>

          <!-- Question Card -->
          <div class="rounded-2xl bg-white dark:bg-slate-800 border-2 border-slate-200 dark:border-slate-700 shadow-lg p-6 sm:p-8 mb-6">
            <div class="text-center mb-6">
              <span class="text-5xl mb-4 block">❓</span>
              <h2 class="text-xl sm:text-2xl font-bold text-slate-800 dark:text-white leading-relaxed">
                {{ currentQuestion()!.questionText }}
              </h2>
            </div>

            <!-- Answer Input -->
            <div class="mb-4">
              <label class="text-sm font-medium text-slate-600 dark:text-slate-400 mb-2 block">Your Answer:</label>
              <input
                type="text"
                class="w-full px-4 py-3 text-lg border-2 border-slate-200 dark:border-slate-600 rounded-xl bg-white dark:bg-slate-700 text-slate-800 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors"
                [value]="currentAnswer()"
                (input)="currentAnswer.set($any($event.target).value)"
                (keydown.enter)="revealAnswer()"
                placeholder="Type your answer here..."
                autofocus />
            </div>

            <button (click)="revealAnswer()"
              class="w-full py-4 rounded-xl bg-blue-500 hover:bg-blue-600 active:scale-[0.98] text-white text-lg font-bold shadow-lg transition-all"
              [disabled]="!currentAnswer().trim()">
              ✅ Check My Answer
            </button>
          </div>
        </div>
        }

        <!-- Revealed Phase: show correct answer + self-assessment buttons -->
        @if (phase() === 'revealed' && currentQuestion()) {
        <div class="mb-6">
          <!-- Progress -->
          <div class="flex items-center justify-between mb-3">
            <span class="text-sm font-medium text-slate-500 dark:text-slate-400">
              Question {{ currentIndex() + 1 }} of {{ playData()!.totalQuestions }}
            </span>
            <span class="text-sm font-medium text-slate-500 dark:text-slate-400">
              ⏱️ {{ formatTime(elapsedSeconds()) }}
            </span>
          </div>
          <div class="relative w-full h-3 bg-slate-100 dark:bg-slate-700 rounded-full overflow-hidden mb-6">
            <div class="h-full bg-gradient-to-r from-blue-400 to-blue-600 rounded-full transition-all duration-300"
              [style.width.%]="progressPercent()"></div>
          </div>

          <!-- Question + Answers Comparison -->
          <div class="rounded-2xl bg-white dark:bg-slate-800 border-2 border-slate-200 dark:border-slate-700 shadow-lg p-6 sm:p-8 mb-6">
            <div class="text-center mb-6">
              <span class="text-4xl mb-3 block">🤔</span>
              <h2 class="text-lg sm:text-xl font-bold text-slate-800 dark:text-white mb-1">
                {{ currentQuestion()!.questionText }}
              </h2>
            </div>

            <!-- Your answer -->
            <div class="rounded-xl bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 p-4 mb-4">
              <p class="text-xs font-medium text-blue-600 dark:text-blue-400 mb-1">Your Answer:</p>
              <p class="text-lg font-semibold text-blue-800 dark:text-blue-200">{{ currentAnswer() }}</p>
            </div>

            <!-- Correct answer -->
            <div class="rounded-xl bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 p-4 mb-6">
              <p class="text-xs font-medium text-green-600 dark:text-green-400 mb-1">Correct Answer:</p>
              <p class="text-lg font-semibold text-green-800 dark:text-green-200">{{ currentQuestion()!.correctAnswer }}</p>
            </div>

            <!-- Self-assessment -->
            <p class="text-center text-sm text-slate-500 dark:text-slate-400 mb-4">Was your answer correct?</p>
            <div class="flex gap-3">
              <button (click)="markAnswer(true)"
                class="flex-1 py-4 rounded-xl bg-green-500 hover:bg-green-600 active:scale-[0.98] text-white text-lg font-bold shadow-lg transition-all">
                ✅ Correct
              </button>
              <button (click)="markAnswer(false)"
                class="flex-1 py-4 rounded-xl bg-red-500 hover:bg-red-600 active:scale-[0.98] text-white text-lg font-bold shadow-lg transition-all">
                ❌ Wrong
              </button>
            </div>
          </div>
        </div>
        }

        <!-- Results Phase -->
        @if (phase() === 'results') {
        <div class="text-center mb-8 pt-4">
          <p class="text-5xl mb-4">{{ resultEmoji() }}</p>
          <h1 class="text-3xl sm:text-4xl font-bold text-slate-800 dark:text-white mb-2">
            {{ resultTitle() }}
          </h1>
          <p class="text-lg text-slate-500 dark:text-slate-400">
            You got {{ score() }} out of {{ playData()!.totalQuestions }} correct
          </p>
        </div>

        <!-- Score Display -->
        <div class="rounded-2xl bg-gradient-to-br from-blue-400 to-indigo-500 p-6 sm:p-8 text-center text-white shadow-lg mb-6">
          <p class="text-lg opacity-80 mb-1">Your Score</p>
          <p class="text-5xl sm:text-6xl font-bold">{{ score() }}/{{ playData()!.totalQuestions }}</p>
          <p class="text-lg opacity-80 mt-2">{{ scorePercentage() | number:'1.0-0' }}%</p>
        </div>

        <!-- Time -->
        <div class="rounded-2xl bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 p-5 text-center shadow-sm mb-6">
          <p class="text-3xl font-bold text-blue-600 dark:text-blue-400">⏱️ {{ formatTime(elapsedSeconds()) }}</p>
          <p class="text-sm text-slate-500 dark:text-slate-400 mt-1">Time Taken</p>
        </div>

        <!-- Per-question results -->
        <div class="space-y-3 mb-8">
          <h3 class="text-lg font-semibold text-slate-800 dark:text-white mb-2">Question Details</h3>
          @for (r of questionResults(); track $index; let i = $index) {
          <div class="rounded-xl border-2 p-4"
            [class]="r.markedCorrect
              ? 'border-green-200 dark:border-green-800 bg-green-50 dark:bg-green-900/20'
              : 'border-red-200 dark:border-red-800 bg-red-50 dark:bg-red-900/20'">
            <div class="flex items-start gap-3">
              <span class="text-2xl flex-shrink-0">{{ r.markedCorrect ? '✅' : '❌' }}</span>
              <div class="min-w-0 flex-1">
                <p class="font-medium text-sm text-slate-800 dark:text-white">{{ r.questionText }}</p>
                <p class="text-xs text-slate-500 dark:text-slate-400 mt-1">Your answer: <span class="font-semibold">{{ r.patientAnswer }}</span></p>
                <p class="text-xs mt-0.5"
                  [class]="r.markedCorrect ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400'">
                  Correct answer: <span class="font-semibold">{{ r.correctAnswer }}</span>
                </p>
              </div>
            </div>
          </div>
          }
        </div>

        <!-- Actions -->
        <div class="flex gap-3 pb-8">
          <button (click)="playAgain()"
            class="flex-1 py-4 rounded-xl bg-blue-500 hover:bg-blue-600 active:scale-[0.98] text-white text-lg font-bold shadow-lg transition-all">
            🔄 Play Again
          </button>
          <button (click)="goBack()"
            class="flex-1 py-4 rounded-xl bg-slate-200 dark:bg-slate-700 hover:bg-slate-300 dark:hover:bg-slate-600 active:scale-[0.98] text-slate-800 dark:text-white text-lg font-bold shadow-sm transition-all">
            ← Back
          </button>
        </div>
        }

      </main>
    </div>
  `,
})
export class PlayPersonalQuestionsComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly personalQuestionService = inject(PersonalQuestionService);
  private readonly keycloakService = inject(KeycloakService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  // Game state
  phase = signal<Phase>('loading');
  playData = signal<PersonalQuestionPlayData | null>(null);
  currentIndex = signal<number>(0);
  currentAnswer = signal<string>('');
  score = signal<number>(0);
  questionResults = signal<QuestionResult[]>([]);
  errorMessage = signal<string>('');

  // Timer
  elapsedSeconds = signal<number>(0);
  private timerInterval: any;

  // Computed
  currentQuestion = computed(() => {
    const data = this.playData();
    if (!data) return null;
    return data.questions[this.currentIndex()] ?? null;
  });

  progressPercent = computed(() => {
    const data = this.playData();
    if (!data || data.totalQuestions === 0) return 0;
    return ((this.currentIndex()) / data.totalQuestions) * 100;
  });

  scorePercentage = computed(() => {
    const data = this.playData();
    if (!data || data.totalQuestions === 0) return 0;
    return (this.score() / data.totalQuestions) * 100;
  });

  resultEmoji = computed(() => {
    const pct = this.scorePercentage();
    if (pct >= 80) return '🎉';
    if (pct >= 50) return '👍';
    return '💪';
  });

  resultTitle = computed(() => {
    const pct = this.scorePercentage();
    if (pct >= 80) return 'Excellent!';
    if (pct >= 50) return 'Good Job!';
    return 'Keep Trying!';
  });

  ngOnInit(): void {
    const gameId = Number(this.route.snapshot.paramMap.get('gameId'));
    if (!gameId) {
      this.errorMessage.set('Invalid game ID');
      return;
    }
    this.loadGame(gameId);
  }

  private loadGame(gameId: number): void {
    this.phase.set('loading');
    this.personalQuestionService.getGameForPlay(gameId)
      .pipe(
        tap(data => {
          this.playData.set(data);
          this.phase.set('playing');
          this.startTimer();
        }),
        catchError(err => {
          console.error('[PlayPersonalQuestions] Failed to load game', err);
          this.errorMessage.set('Failed to load the game. Please try again.');
          this.phase.set('loading');
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  revealAnswer(): void {
    if (!this.currentAnswer().trim()) return;
    this.phase.set('revealed');
  }

  markAnswer(correct: boolean): void {
    const q = this.currentQuestion();
    if (!q) return;

    // Record result
    const result: QuestionResult = {
      questionText: q.questionText,
      correctAnswer: q.correctAnswer,
      patientAnswer: this.currentAnswer(),
      markedCorrect: correct,
    };

    this.questionResults.update(results => [...results, result]);

    if (correct) {
      this.score.update(s => s + 1);
    }

    // Move to next question or finish
    const data = this.playData()!;
    const nextIndex = this.currentIndex() + 1;

    if (nextIndex >= data.totalQuestions) {
      // Game finished
      this.stopTimer();
      this.submitResults();
      this.phase.set('results');
    } else {
      this.currentIndex.set(nextIndex);
      this.currentAnswer.set('');
      this.phase.set('playing');
    }
  }

  private submitResults(): void {
    const data = this.playData();
    if (!data) return;

    const kc = this.keycloakService.getKeycloakInstance();
    const keycloakId = kc?.subject ?? kc?.tokenParsed?.['sub'] ?? '';
    if (!keycloakId) return;

    this.personalQuestionService.submitResults(data.gameId, keycloakId, {
      score: this.score(),
      totalQuestions: data.totalQuestions,
      durationSeconds: this.elapsedSeconds(),
    }).pipe(
      tap(response => {
        console.log('[PlayPersonalQuestions] Results submitted:', response);
      }),
      catchError(err => {
        console.error('[PlayPersonalQuestions] Failed to submit results', err);
        return of(null);
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  playAgain(): void {
    const data = this.playData();
    if (!data) return;

    this.currentIndex.set(0);
    this.currentAnswer.set('');
    this.score.set(0);
    this.questionResults.set([]);
    this.elapsedSeconds.set(0);
    this.errorMessage.set('');

    this.loadGame(data.gameId);
  }

  goBack(): void {
    this.stopTimer();
    this.router.navigate(['/patient']);
  }

  // ─── Timer ─────────────────────────────────────────────

  private startTimer(): void {
    this.stopTimer();
    this.elapsedSeconds.set(0);
    this.timerInterval = setInterval(() => {
      this.elapsedSeconds.update(s => s + 1);
    }, 1000);
  }

  private stopTimer(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }

  formatTime(seconds: number): string {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  }
}
