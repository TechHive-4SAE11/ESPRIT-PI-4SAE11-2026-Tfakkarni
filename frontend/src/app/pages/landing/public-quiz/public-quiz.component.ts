import {
  Component,
  signal,
  computed,
  inject,
  DestroyRef,
  OnInit
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, of, tap } from 'rxjs';

import { ZardCardComponent } from '@/shared/components/card';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardIconComponent } from '@/shared/components/icon';
import { QuizService } from '@/core/services/quiz.service';
import { QuestionDTO, AnswerDTO } from '@/core/models/quiz.model';

// View states for the public quiz flow
type QuizView = 'intro' | 'level-transition' | 'playing' | 'result';

@Component({
  selector: 'app-public-quiz',
  standalone: true,
  imports: [CommonModule, ZardCardComponent, ZardButtonComponent, ZardIconComponent],
  template: `
    <div class="w-full max-w-4xl mx-auto">

      <!-- ══════════ INTRO ══════════ -->
      @if (view() === 'intro') {
      <z-card class="p-8 text-center">
        <z-icon zType="brain" class="h-16 w-16 mx-auto mb-4 text-primary" />
        <h2 class="text-3xl font-bold mb-4">Cognitive Assessment</h2>
        <p class="text-lg text-muted-foreground mb-4 max-w-2xl mx-auto">
          This adaptive assessment evaluates your memory and cognitive function across
          <strong>3 difficulty levels</strong>.
        </p>
        <div class="flex justify-center gap-3 mb-6">
          @for (lvl of [1,2,3]; track lvl) {
          <div class="flex flex-col items-center gap-1">
            <span class="w-10 h-10 rounded-full flex items-center justify-center text-sm font-bold border-2"
              [class]="lvl === 1 ? 'border-emerald-400 bg-emerald-50 text-emerald-700'
                      : lvl === 2 ? 'border-amber-400 bg-amber-50 text-amber-700'
                      : 'border-red-400 bg-red-50 text-red-700'">
              {{ lvl }}
            </span>
            <span class="text-xs text-muted-foreground">{{ lvl === 1 ? 'Easy' : lvl === 2 ? 'Medium' : 'Hard' }}</span>
          </div>
          @if (lvl < 3) { <span class="text-slate-300 mt-3">→</span> }
          }
        </div>
        <p class="text-sm text-muted-foreground mb-6">
          Risk score ≥ 60% (many wrong answers) → advance to a harder level &nbsp;|&nbsp; Risk &lt; 60% → return to a previous level
        </p>
        <button z-button zSize="lg" (click)="startGlobalQuiz()" [disabled]="isLoading()">
          @if (isLoading()) {
          <z-icon zType="loader-2" class="mr-2 animate-spin" />
          Loading...
          } @else {
          Start Assessment
          <z-icon zType="arrow-right" class="ml-2" />
          }
        </button>
      </z-card>
      }

      <!-- ══════════ LEVEL TRANSITION SCREEN ══════════ -->
      @if (view() === 'level-transition') {
      <z-card class="p-10 text-center">
        <div class="mb-6">
          <!-- Level road -->
          <div class="flex justify-center items-center gap-3 mb-6">
            @for (lvl of [1,2,3]; track lvl) {
            <div class="flex flex-col items-center gap-1">
              <span class="w-12 h-12 rounded-full flex items-center justify-center text-sm font-bold border-2 transition-all"
                [class]="currentLevel() > lvl
                  ? 'border-emerald-500 bg-emerald-500 text-white shadow-md'
                  : currentLevel() === lvl
                    ? 'border-blue-500 bg-blue-500 text-white shadow-lg scale-110'
                    : 'border-slate-300 bg-slate-100 text-slate-400'">
                {{ currentLevel() > lvl ? '✓' : lvl }}
              </span>
              <span class="text-xs font-medium"
                [class]="currentLevel() === lvl ? 'text-blue-600 font-bold' : 'text-muted-foreground'">
                {{ lvl === 1 ? 'Easy' : lvl === 2 ? 'Medium' : 'Hard' }}
              </span>
            </div>
            @if (lvl < 3) { <span class="text-slate-300 text-xl mb-4">→</span> }
            }
          </div>

          <!-- Transition message -->
          <div class="rounded-2xl p-6 mb-6" [class]="transitionIsAdvance()
            ? 'bg-amber-50 dark:bg-amber-900/20 border border-amber-200'
            : 'bg-emerald-50 dark:bg-emerald-900/20 border border-emerald-200'">
            <p class="text-3xl mb-2">{{ transitionIsAdvance() ? '⚠️' : '✅' }}</p>
            <p class="text-xl font-bold mb-1" [class]="transitionIsAdvance() ? 'text-amber-700' : 'text-emerald-700'">
              {{ transitionMessage() }}
            </p>
            <p class="text-sm text-muted-foreground">
              {{ transitionIsAdvance()
                ? 'High risk score detected — assessing with harder questions.'
                : 'Low risk score — returning to an easier level to confirm baseline.' }}
            </p>
          </div>

          <button z-button zSize="lg" (click)="beginCurrentLevel()" [disabled]="isLoading()">
            @if (isLoading()) {
            <z-icon zType="loader-2" class="mr-2 animate-spin" />
            Loading questions...
            } @else {
            Continue to Level {{ currentLevel() }}
            <z-icon zType="arrow-right" class="ml-2" />
            }
          </button>
        </div>
      </z-card>
      }

      <!-- ══════════ PLAYING ══════════ -->
      @if (view() === 'playing' && currentQuestion(); as question) {
      <z-card class="p-8">

        <!-- Level badges + progress -->
        <div class="mb-6">
          <div class="flex items-center gap-2 mb-3">
            @for (lvl of [1,2,3]; track lvl) {
            <span class="px-3 py-0.5 rounded-full text-xs font-bold border-2 transition-all"
              [class]="currentLevel() === lvl
                ? 'bg-blue-600 border-blue-600 text-white shadow-md'
                : completedLevels().includes(lvl)
                  ? 'bg-emerald-100 border-emerald-400 text-emerald-700'
                  : 'bg-slate-100 border-slate-300 text-slate-400'">
              {{ lvl === 1 ? '1 · Easy' : lvl === 2 ? '2 · Medium' : '3 · Hard' }}
              {{ completedLevels().includes(lvl) ? ' ✓' : '' }}
            </span>
            @if (lvl < 3) { <span class="text-slate-300">→</span> }
            }
          </div>
          <div class="flex justify-between text-sm text-muted-foreground mb-2">
            <span>Level {{ currentLevel() }} — Question {{ currentQuestionIndex() + 1 }} / {{ totalQuestions() }}</span>
            <span>⚠️ Risk: <strong [class]="riskPercentage() >= 60 ? 'text-red-500' : 'text-emerald-600'">{{ riskPercentage() }}%</strong></span>
          </div>
          <div class="w-full h-2.5 bg-muted rounded-full overflow-hidden">
            <div class="h-full rounded-full transition-all duration-500"
              [class]="currentLevel() === 1 ? 'bg-emerald-500' : currentLevel() === 2 ? 'bg-amber-500' : 'bg-red-500'"
              [style.width.%]="((currentQuestionIndex() + 1) / totalQuestions()) * 100">
            </div>
          </div>
        </div>

        <!-- Question -->
        <h3 class="text-2xl font-bold mb-6">{{ question.text }}</h3>

        @if (questionAnswers().length > 0) {
        <div class="space-y-3 mb-6">
          @for (answer of questionAnswers(); track answer.id) {
          <button (click)="selectAnswer(answer)"
            [class]="selectedAnswer()?.id === answer.id
              ? 'w-full p-4 rounded-xl bg-blue-600 text-white font-semibold text-left transition-all border-2 border-blue-600 shadow-lg'
              : 'w-full p-4 rounded-xl bg-slate-50 dark:bg-slate-800 hover:bg-blue-50 dark:hover:bg-slate-700 font-semibold text-left transition-all border-2 border-slate-200 dark:border-slate-700'">
            {{ answer.text }}
          </button>
          }
        </div>
        <button z-button (click)="submitAnswer()" [disabled]="!selectedAnswer() || submitting()">
          @if (submitting()) {
          <z-icon zType="loader-2" class="mr-2 animate-spin" />
          } @else {
          Next
          <z-icon zType="arrow-right" class="ml-2" />
          }
        </button>
        } @else {
        <div class="text-center py-8">
          <z-icon zType="loader-2" class="h-8 w-8 mx-auto animate-spin text-muted-foreground" />
          <p class="text-muted-foreground mt-4">Loading answers...</p>
        </div>
        }
      </z-card>
      }

      <!-- ══════════ RESULT ══════════ -->
      @if (view() === 'result') {
      <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4"
           (click)="closeResult()">
        <z-card class="max-w-md w-full p-6 max-h-[90vh] overflow-y-auto" (click)="$event.stopPropagation()">

          <!-- Header -->
          <div class="text-center mb-6">
            @if (riskPercentage() >= 60) {
            <p class="text-5xl mb-2">⚠️</p>
            <h2 class="text-2xl font-bold text-red-600 dark:text-red-400">Potential Signs Detected</h2>
            } @else {
            <p class="text-5xl mb-2">✅</p>
            <h2 class="text-2xl font-bold text-emerald-600 dark:text-emerald-400">Low Risk Detected</h2>
            }
          </div>

          <!-- Risk score -->
          <div class="text-center mb-5">
            <p class="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-1">
              Alzheimer's Risk Score
            </p>
            <div class="text-6xl font-bold mb-1"
              [class]="riskPercentage() >= 60 ? 'text-red-500' : 'text-emerald-500'">
              {{ riskPercentage() }}%
            </div>
            <p class="text-sm text-muted-foreground">
              {{ correctAnswers() }} / {{ totalQuestions() }} correct answers &nbsp;|&nbsp;
              Reached Level {{ currentLevel() }}
            </p>
            <p class="text-xs text-muted-foreground mt-1">Risk score = % incorrect answers. Higher is worse.</p>
          </div>

          <!-- Level reached badge -->
          <div class="flex justify-center gap-2 mb-5">
            @for (lvl of [1,2,3]; track lvl) {
            <span class="px-2 py-0.5 rounded-full text-xs font-bold border"
              [class]="completedLevels().includes(lvl)
                ? 'border-emerald-400 bg-emerald-50 text-emerald-700'
                : 'border-slate-200 bg-slate-50 text-slate-400'">
              L{{ lvl }} {{ completedLevels().includes(lvl) ? '✓' : '—' }}
            </span>
            }
          </div>

          <!-- Risk explanation -->
          @if (riskPercentage() >= 60) {
          <div class="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-xl p-4 mb-4">
            <p class="text-sm font-semibold text-red-700 dark:text-red-300 mb-1">⚠️ What this means</p>
            <p class="text-sm text-red-700 dark:text-red-300">
              Your results suggest significant difficulty with memory and cognitive tasks, which may indicate possible
              early signs of Alzheimer's disease. <strong>We strongly recommend consulting a healthcare professional as soon as possible.</strong>
            </p>
          </div>
          } @else {
          <div class="bg-emerald-50 dark:bg-emerald-900/20 border border-emerald-200 rounded-xl p-4 mb-4">
            <p class="text-sm text-emerald-700 dark:text-emerald-300">
              Your assessment shows <strong>no significant risk</strong> of Alzheimer's disease. Keep up regular brain exercises!
              If you have any doubts, we still recommend consulting your doctor.
            </p>
          </div>
          }

          <!-- NOT saved notice -->
          <div class="bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl p-3 text-center mb-4">
            <p class="text-sm font-semibold text-slate-600 dark:text-slate-400">
              🚫 This result is <strong>NOT saved</strong> — you are anonymous
            </p>
            <p class="text-xs text-slate-500 mt-1">
              Create a free account so your doctor can track your cognitive health over time.
            </p>
          </div>

          <!-- CTAs -->
          <div class="space-y-2">
            <button z-button class="w-full" (click)="goToSignup()">
              {{ riskPercentage() >= 60 ? '🏥 Register & Consult a Doctor' : '📊 Create Account to Track Progress' }}
              <z-icon zType="arrow-right" class="ml-2" />
            </button>
            <button z-button zType="outline" class="w-full" (click)="closeResult()">
              Close (results will be lost)
            </button>
          </div>
        </z-card>
      </div>
      }

    </div>
  `,
})
export class PublicQuizComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly quizService = inject(QuizService);
  private readonly router = inject(Router);

  // ─── View State ───────────────────────────────────────────────
  view = signal<QuizView>('intro');

  // ─── Quiz Data ────────────────────────────────────────────────
  allQuestions = signal<QuestionDTO[]>([]);
  currentQuestion = signal<QuestionDTO | null>(null);
  currentQuestionIndex = signal<number>(0);
  questionAnswers = signal<AnswerDTO[]>([]);
  selectedAnswer = signal<AnswerDTO | null>(null);
  currentLevel = signal<number>(1);

  // Correct answers in the current level
  correctAnswers = signal<number>(0);
  // Wrong answers in the current level
  wrongAnswers = signal<number>(0);
  totalQuestions = signal<number>(0);
  // Levels the patient has already fully completed
  completedLevels = signal<number[]>([]);

  // ─── Level Transition ─────────────────────────────────────────
  transitionMessage = signal<string>('');
  transitionIsAdvance = signal<boolean>(true);

  // ─── Loading ──────────────────────────────────────────────────
  isLoading = signal<boolean>(false);
  submitting = signal<boolean>(false);

  // ─── Preloaded questions for next level (during transition) ───
  private preloadedQuestions = signal<QuestionDTO[] | null>(null);

  // ─── Computed ─────────────────────────────────────────────────
  /**
   * Risk score = % of WRONG answers.
   * Higher means more cognitive difficulty detected.
   * Threshold >= 60 → high risk → advance to harder level.
   */
  riskPercentage = computed(() => {
    const total = this.totalQuestions();
    return total === 0 ? 0 : Math.round((this.wrongAnswers() / total) * 100);
  });

  ngOnInit(): void { }

  // ─── START ────────────────────────────────────────────────────
  startGlobalQuiz(): void {
    this.completedLevels.set([]);
    this.correctAnswers.set(0);
    this.wrongAnswers.set(0);
    this.totalQuestions.set(0);
    this.preloadedQuestions.set(null);
    this.fetchQuestionsForLevel(1);
  }

  /**
   * Fetch questions from the backend for the given level.
   * When called from startGlobalQuiz or beginCurrentLevel → switch to playing.
   * When prefetching during a transition screen → only store in preloadedQuestions.
   */
  private fetchQuestionsForLevel(level: number, prefetchOnly = false): void {
    this.isLoading.set(true);
    if (!prefetchOnly) {
      this.currentLevel.set(level);
    }

    this.quizService.getQuestionsByDifficultyLevel(level)
      .pipe(
        tap(questions => {
          if (questions && questions.length > 0) {
            if (prefetchOnly) {
              // Just cache; don't touch the view
              this.preloadedQuestions.set(questions);
            } else {
              this.allQuestions.set(questions);
              this.totalQuestions.set(questions.length);
              this.currentQuestionIndex.set(0);
              this.correctAnswers.set(0);
              this.wrongAnswers.set(0);
              this.view.set('playing');
              this.loadCurrentQuestion();
            }
          } else {
            console.warn(`[PublicQuiz] No questions for level ${level}.`);
            if (!prefetchOnly) this.completeAssessment();
          }
        }),
        catchError(err => {
          console.error('[PublicQuiz] Failed to load questions', err);
          if (!prefetchOnly) this.view.set('intro');
          return of(null);
        }),
        finalize(() => this.isLoading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  /** Called from the level-transition screen "Continue" button */
  beginCurrentLevel(): void {
    const preloaded = this.preloadedQuestions();
    const level = this.currentLevel();
    if (preloaded && preloaded.length > 0) {
      // Use the preloaded questions to avoid a second network call
      this.allQuestions.set(preloaded);
      this.totalQuestions.set(preloaded.length);
      this.currentQuestionIndex.set(0);
      this.correctAnswers.set(0);
      this.wrongAnswers.set(0);
      this.preloadedQuestions.set(null);
      this.view.set('playing');
      this.loadCurrentQuestion();
    } else {
      // Fallback: fetch now
      this.fetchQuestionsForLevel(level);
    }
  }

  private loadCurrentQuestion(): void {
    const questions = this.allQuestions();
    const index = this.currentQuestionIndex();

    if (index >= questions.length) {
      this.evaluateLevelProgress();
      return;
    }

    const question = questions[index];
    if (!question?.id) {
      this.evaluateLevelProgress();
      return;
    }

    this.currentQuestion.set(question);
    this.selectedAnswer.set(null);
    this.questionAnswers.set([]);

    this.quizService.getAnswersByQuestionId(question.id)
      .pipe(
        tap(answers => this.questionAnswers.set(answers)),
        catchError(() => of([])),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  selectAnswer(answer: AnswerDTO): void {
    this.selectedAnswer.set(answer);
  }

  submitAnswer(): void {
    if (this.submitting()) return;
    const answer = this.selectedAnswer();
    if (!answer) return;

    this.submitting.set(true);

    // Risk score: count wrong answers (more wrong = more risk)
    if (answer.isCorrect) {
      this.correctAnswers.update(c => c + 1);
    } else {
      this.wrongAnswers.update(w => w + 1);
    }

    setTimeout(() => {
      this.currentQuestionIndex.update(i => i + 1);
      this.submitting.set(false);
      this.loadCurrentQuestion();
    }, 400);
  }

  // ─── LEVEL EVALUATION ─────────────────────────────────────────
  private evaluateLevelProgress(): void {
    const risk = this.riskPercentage();  // % wrong answers
    const level = this.currentLevel();

    // Mark this level as completed
    this.completedLevels.update(cl => cl.includes(level) ? cl : [...cl, level]);

    if (risk >= 60) {
      // High risk (many wrong answers) → expose to harder questions
      if (level < 3) {
        const nextLevel = level + 1;
        this.transitionMessage.set(`Level ${level}: ${risk}% risk score — progressing to Level ${nextLevel} ⚠️`);
        this.transitionIsAdvance.set(true);
        this.currentLevel.set(nextLevel);
        this.view.set('level-transition');
        // Prefetch next level in the background
        this.fetchQuestionsForLevel(nextLevel, true);
      } else {
        // Level 3 with high risk → Alzheimer's warning
        this.completeAssessment();
      }
    } else {
      // Low risk (mostly correct) → regress to easier level
      if (level > 1) {
        const prevLevel = level - 1;
        this.transitionMessage.set(`Level ${level}: ${risk}% risk — returning to Level ${prevLevel} ✅`);
        this.transitionIsAdvance.set(false);
        this.currentLevel.set(prevLevel);
        this.view.set('level-transition');
        this.fetchQuestionsForLevel(prevLevel, true);
      } else {
        // Low risk at Level 1 → patient is safe
        this.completeAssessment();
      }
    }
  }

  private completeAssessment(): void {
    // ─── ANONYMOUS USER — score is NOT saved anywhere ───
    // Results are only in component signals (in-memory).
    // Registered patients use the patient dashboard to save scores.
    this.view.set('result');
  }

  goToSignup(): void {
    this.closeResult();
    this.router.navigate(['/signup']);
  }

  closeResult(): void {
    this.view.set('intro');
    // Reset everything
    this.allQuestions.set([]);
    this.currentQuestion.set(null);
    this.questionAnswers.set([]);
    this.correctAnswers.set(0);
    this.wrongAnswers.set(0);
    this.totalQuestions.set(0);
    this.completedLevels.set([]);
    this.currentLevel.set(1);
    this.preloadedQuestions.set(null);
  }
}
