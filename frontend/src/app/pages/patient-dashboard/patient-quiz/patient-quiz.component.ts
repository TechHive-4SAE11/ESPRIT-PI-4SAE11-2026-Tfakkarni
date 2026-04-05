import {
  Component, OnInit, signal, Input, inject, DestroyRef, computed
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, of, tap } from 'rxjs';

import { QuizService } from '@/core/services/quiz.service';
import { UserApiService } from '@/core/services/user-api.service';
import { QuizDTO, QuestionDTO, AnswerDTO } from '@/core/models/quiz.model';

// What triggered the end of the quiz
type QuizEndReason =
  | 'alzheimer-risk'   // Reached Level 3 AND risk score >= 60 (many wrong answers)
  | 'low-risk'         // Finished any level with risk score < 60 (mostly correct answers)
  | 'passed-all';      // All three levels fully completed with low risk

@Component({
  selector: 'app-patient-quiz',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './patient-quiz.component.html'
})
export class PatientQuizComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly quizService = inject(QuizService);
  private readonly userApiService = inject(UserApiService);

  @Input() keycloakId = '';

  // ─── VIEW STATE ───────────────────────────────────────────────
  view = signal<'list' | 'playing' | 'result' | 'history'>('list');

  // ─── DATA ────────────────────────────────────────────────────
  quizzes = signal<QuizDTO[]>([]);
  recentQuizzes = signal<QuizDTO[]>([]);
  weakTopics = signal<string[]>([]);
  avgScore = signal<number>(0);
  quizCount = signal<number>(0);

  // ─── QUIZ SESSION ─────────────────────────────────────────────
  currentQuiz = signal<QuizDTO | null>(null);
  currentQuestion = signal<QuestionDTO | null>(null);
  currentQuestionIndex = signal<number>(0);
  questionAnswers = signal<AnswerDTO[]>([]);
  selectedAnswer = signal<AnswerDTO | null>(null);
  answerSubmitted = signal<boolean>(false);
  lastValidation = signal<{ isCorrect: boolean; explanation: string } | null>(null);

  // ─── ADAPTIVE LEVEL LOGIC ─────────────────────────────────────
  /** Current difficulty level being played (1 = Easy, 2 = Medium, 3 = Hard) */
  currentLevel = signal<number>(1);
  /** Questions for the current level only */
  levelQuestions = signal<QuestionDTO[]>([]);
  /** Total questions in current level */
  levelTotalQuestions = signal<number>(0);
  /** Correct answers in the current level (used for risk calculation) */
  levelCorrectAnswers = signal<number>(0);
  /** Incorrect answers in the current level */
  levelWrongAnswers = signal<number>(0);
  /** All questions grouped by difficulty level */
  questionsByLevel: Record<number, QuestionDTO[]> = {};

  // ─── RESULT STATE ─────────────────────────────────────────────
  /** Why the quiz ended — drives the result view message */
  quizEndReason = signal<QuizEndReason>('low-risk');
  /** Final risk percentage displayed in the result view (% wrong on the decisive level) */
  finalRiskPercent = signal<number>(0);
  /** The highest level the patient reached during this attempt */
  levelReached = signal<number>(1);

  // ─── LOADING ─────────────────────────────────────────────────
  isLoading = signal<boolean>(false);
  isLoadingAnswers = signal<boolean>(false);
  isSubmitting = signal<boolean>(false);
  isSaving = signal<boolean>(false);

  // ─── NOTIFICATION ────────────────────────────────────────────
  notification = signal<{ message: string; type: 'success' | 'error' | 'info' } | null>(null);

  // ─── USER ────────────────────────────────────────────────────
  userNeonDbId = signal<number | null>(null);

  // ─── COMPUTED ────────────────────────────────────────────────
  availableQuizzes = computed(() => this.quizzes().filter(q => q.questions && q.questions.length > 0));

  /**
   * Risk score for the current level = % of WRONG answers.
   * Higher wrong answers → higher risk score.
   * Threshold: >= 60 means high risk → advance to harder level (or flag Alzheimer's at L3).
   *            <  60 means low risk  → go back to previous level (or finish as safe).
   */
  levelRiskPercent = computed(() => {
    const total = this.levelTotalQuestions();
    if (total === 0) return 0;
    return Math.round((this.levelWrongAnswers() / total) * 100);
  });

  ngOnInit(): void {
    if (this.keycloakId) this.loadUserInfo();
  }

  private notify(message: string, type: 'success' | 'error' | 'info' = 'info'): void {
    this.notification.set({ message, type });
    setTimeout(() => this.notification.set(null), 4000);
  }

  // ─── INIT ────────────────────────────────────────────────────
  private loadUserInfo(): void {
    this.userApiService.getUserByKeycloakId(this.keycloakId).pipe(
      tap(u => {
        this.userNeonDbId.set(u.id);
        this.loadQuizzes();
        this.loadStats(u.id);
      }),
      catchError(() => of(null)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  private loadStats(caregiverId: number): void {
    this.quizService.getAverageScoreByCaregiver(caregiverId).pipe(
      tap(s => this.avgScore.set(s || 0)),
      catchError(() => of(0))
    ).subscribe();
    this.quizService.getWeakTopicsByCaregiver(caregiverId).pipe(
      tap(t => this.weakTopics.set(t || [])),
      catchError(() => of([]))
    ).subscribe();
    this.quizService.getQuizCountByCaregiver(caregiverId).pipe(
      tap(c => this.quizCount.set(c || 0)),
      catchError(() => of(0))
    ).subscribe();
    this.quizService.getRecentQuizzesByCaregiver(caregiverId, 5).pipe(
      tap(r => this.recentQuizzes.set(r || [])),
      catchError(() => of([]))
    ).subscribe();
  }

  // ─── QUIZ LIST ───────────────────────────────────────────────
  loadQuizzes(): void {
    const cid = this.userNeonDbId();
    if (!cid) return;
    this.isLoading.set(true);
    this.quizService.getQuizzesByCaregiverId(cid).pipe(
      tap(qs => this.quizzes.set(qs)),
      catchError(() => of([])),
      finalize(() => this.isLoading.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  // ─── START QUIZ ───────────────────────────────────────────────
  startQuiz(quiz: QuizDTO): void {
    // Group questions by difficultyLevel (1, 2, 3)
    this.questionsByLevel = {};
    for (const q of (quiz.questions ?? [])) {
      const lvl = q.difficultyLevel ?? 1;
      if (!this.questionsByLevel[lvl]) this.questionsByLevel[lvl] = [];
      this.questionsByLevel[lvl].push(q);
    }

    this.quizService.startQuiz(quiz.id!).pipe(
      catchError(() => of(null)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.currentQuiz.set(quiz);
      this.answerSubmitted.set(false);
      this.lastValidation.set(null);
      this.levelReached.set(1);
      this.view.set('playing');
      // ✅ Spec: always start at Level 1
      this.startLevel(1);
    });
  }

  /** Initialise a level and start asking its questions */
  private startLevel(level: number): void {
    const questions = this.questionsByLevel[level] ?? [];
    if (questions.length === 0) {
      // No questions for this level inside the quiz → fetch from API by difficulty level
      this.fetchAndStartLevel(level);
      return;
    }
    this.currentLevel.set(level);
    // Track the maximum level reached
    if (level > this.levelReached()) {
      this.levelReached.set(level);
    }
    this.levelQuestions.set(questions);
    this.levelTotalQuestions.set(questions.length);
    this.levelCorrectAnswers.set(0);
    this.levelWrongAnswers.set(0);
    this.currentQuestionIndex.set(0);
    this.loadLevelQuestion();
  }

  /**
   * Fetches ALL questions at a given difficulty level from the API
   * (fallback when the current quiz object doesn't have questions at that level).
   * This keeps the adaptive logic working even if the doctor only added questions
   * for levels 1 and 2 to a specific quiz.
   */
  private fetchAndStartLevel(level: number): void {
    this.isLoadingAnswers.set(true);
    this.quizService.getQuestionsByDifficultyLevel(level).pipe(
      tap(questions => {
        if (questions && questions.length > 0) {
          // Cache them so subsequent calls to evaluateLevelProgress work
          this.questionsByLevel[level] = questions;
          this.currentLevel.set(level);
          if (level > this.levelReached()) {
            this.levelReached.set(level);
          }
          this.levelQuestions.set(questions);
          this.levelTotalQuestions.set(questions.length);
          this.levelCorrectAnswers.set(0);
          this.levelWrongAnswers.set(0);
          this.currentQuestionIndex.set(0);
          this.loadLevelQuestion();
        } else {
          // No questions at this level anywhere → end quiz safely
          this.notify(`ℹ️ No level ${level} questions available. Assessment complete.`, 'info');
          this.endQuiz('low-risk', this.levelRiskPercent(), level - 1);
        }
      }),
      catchError(() => {
        this.notify('⚠️ Could not load level questions. Assessment complete.', 'error');
        this.endQuiz('low-risk', this.levelRiskPercent(), this.currentLevel());
        return of([]);
      }),
      finalize(() => this.isLoadingAnswers.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  /**
   * Called when all questions in a level have been answered.
   *
   * Risk score = % of WRONG answers on this level.
   *   • Risk >= 60 → many mistakes → high risk:
   *       – Levels 1 & 2 → advance to harder level (expose to harder questions)
   *       – Level 3       → Alzheimer's risk warning
   *   • Risk < 60  → mostly correct → low risk:
   *       – Level > 1     → go back to previous level
   *       – Level 1       → assessment done, patient is safe
   */
  private evaluateLevelProgress(): void {
    const risk = this.levelRiskPercent();
    const level = this.currentLevel();

    if (risk >= 60) {
      // High risk (many wrong answers) → advance to harder level
      if (level < 3) {
        this.notify(
          `⚠️ Level ${level}: ${risk}% risk score — moving to Level ${level + 1}.`,
          'info'
        );
        // startLevel handles both: questions in quiz OR fetch from API
        this.startLevel(level + 1);
      } else {
        // Level 3 completed with high risk → Alzheimer's risk detected
        this.endQuiz('alzheimer-risk', risk, level);
      }
    } else {
      // Low risk (mostly correct) → regress to easier level
      if (level > 1) {
        const prevLevel = level - 1;
        this.notify(
          `✅ Level ${level}: ${risk}% risk — returning to Level ${prevLevel}.`,
          'success'
        );
        this.startLevel(prevLevel);
      } else {
        // Low risk at Level 1 → patient is safe
        this.endQuiz('low-risk', risk, level);
      }
    }
  }

  /** Centralised quiz-end handler — saves to backend then shows result */
  private endQuiz(reason: QuizEndReason, riskPercent: number, level: number): void {
    this.quizEndReason.set(reason);
    this.finalRiskPercent.set(riskPercent);
    this.levelReached.set(Math.max(this.levelReached(), level));
    this.saveAndShowResult();
  }

  // ─── LOAD QUESTION ───────────────────────────────────────────
  private loadLevelQuestion(): void {
    const questions = this.levelQuestions();
    const index = this.currentQuestionIndex();
    const question = questions[index];

    if (!question?.id) {
      this.evaluateLevelProgress();
      return;
    }

    this.currentQuestion.set(question);
    this.selectedAnswer.set(null);
    this.answerSubmitted.set(false);
    this.lastValidation.set(null);

    this.isLoadingAnswers.set(true);
    this.quizService.getAnswersByQuestionId(question.id).pipe(
      tap(answers => this.questionAnswers.set(answers)),
      catchError(() => of([])),
      finalize(() => this.isLoadingAnswers.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  selectAnswer(answer: AnswerDTO): void {
    if (this.answerSubmitted()) return;
    this.selectedAnswer.set(answer);
  }

  // ─── SUBMIT ───────────────────────────────────────────────────
  submitAnswer(): void {
    const question = this.currentQuestion();
    const answer = this.selectedAnswer();
    const quiz = this.currentQuiz();
    if (!question?.id || !answer?.id || !quiz?.id) return;

    this.isSubmitting.set(true);

    this.quizService.validateAnswer({ questionId: question.id, answerId: answer.id }).pipe(
      tap(validation => {
        this.lastValidation.set({
          isCorrect: validation.valid,
          explanation: validation.explanation ?? ''
        });
        // Risk score: count WRONG answers (more wrong = more risk)
        if (validation.valid) {
          this.levelCorrectAnswers.update(c => c + 1);
        } else {
          this.levelWrongAnswers.update(w => w + 1);
        }
        this.answerSubmitted.set(true);
        // Fire-and-forget: save answer to backend
        this.quizService.submitAnswer({
          quizId: quiz.id!,
          questionId: question.id!,
          answerId: answer.id!
        }).pipe(catchError(() => of(null))).subscribe();
      }),
      catchError(() => {
        // Fallback: use local isCorrect flag
        if (answer.isCorrect) {
          this.levelCorrectAnswers.update(c => c + 1);
        } else {
          this.levelWrongAnswers.update(w => w + 1);
        }
        this.answerSubmitted.set(true);
        return of(null);
      }),
      finalize(() => this.isSubmitting.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  nextQuestion(): void {
    const next = this.currentQuestionIndex() + 1;
    if (next >= this.levelQuestions().length) {
      this.evaluateLevelProgress();
    } else {
      this.currentQuestionIndex.set(next);
      this.loadLevelQuestion();
    }
  }

  // ─── SAVE + SHOW RESULT ───────────────────────────────────────
  /**
   * Saves score and levelReached to the backend so the doctor dashboard has
   * a complete picture, then transitions to the result view.
   *
   * The score stored = final risk % (0–100). Higher = more risk.
   */
  private saveAndShowResult(): void {
    const quiz = this.currentQuiz();
    if (!quiz?.id) {
      this.view.set('result');
      return;
    }

    const riskScore = this.finalRiskPercent();
    const levelReached = this.levelReached();

    this.isSaving.set(true);
    this.quizService.completeQuiz(quiz.id, riskScore, levelReached).pipe(
      tap(() => {
        const cid = this.userNeonDbId();
        if (cid) this.loadStats(cid); // refresh doctor dashboard stats
      }),
      catchError(() => of(null)),
      finalize(() => {
        this.isSaving.set(false);
        this.view.set('result');
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  // ─── BACK / CANCEL ────────────────────────────────────────────
  backToList(): void {
    this.currentQuiz.set(null);
    this.currentQuestion.set(null);
    this.questionAnswers.set([]);
    this.levelQuestions.set([]);
    this.currentQuestionIndex.set(0);
    this.levelCorrectAnswers.set(0);
    this.levelWrongAnswers.set(0);
    this.currentLevel.set(1);
    this.levelReached.set(1);
    this.lastValidation.set(null);
    this.questionsByLevel = {};
    this.view.set('list');
    this.loadQuizzes();
  }

  cancelQuiz(): void {
    if (confirm('Abandonner ce quiz ? Votre progression sera perdue.')) {
      this.backToList();
    }
  }

  // ─── STYLING HELPERS ──────────────────────────────────────────
  getAnswerClass(answer: AnswerDTO): string {
    const base = 'w-full p-4 rounded-xl font-semibold text-left transition-all border-2 ';
    if (this.answerSubmitted()) {
      if (answer.id === this.selectedAnswer()?.id) {
        return base + (answer.isCorrect
          ? 'border-emerald-500 bg-emerald-500 text-white shadow-emerald-200'
          : 'border-red-500 bg-red-500 text-white');
      }
      if (answer.isCorrect) return base + 'border-emerald-400 bg-emerald-50 dark:bg-emerald-900/30 text-emerald-800 dark:text-emerald-300';
      return base + 'border-slate-200 dark:border-slate-700 bg-slate-100 dark:bg-slate-800 text-slate-400 opacity-50';
    }
    if (answer.id === this.selectedAnswer()?.id) {
      return base + 'border-blue-500 bg-blue-500 text-white shadow-lg shadow-blue-200 dark:shadow-none';
    }
    return base + 'border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 hover:border-blue-400 hover:bg-blue-50 dark:hover:bg-slate-700 text-slate-800 dark:text-white';
  }

  getDifficultyLabel(level: number): string {
    return level === 1 ? 'Easy' : level === 2 ? 'Medium' : 'Hard';
  }

  getDifficultyColor(level: number): string {
    return level === 1
      ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300'
      : level === 2
        ? 'bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300'
        : 'bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300';
  }

  getRiskColor(percent: number): string {
    if (percent >= 60) return '#ef4444'; // red — high risk
    if (percent >= 30) return '#f59e0b'; // amber — moderate
    return '#10b981'; // green — low risk
  }
}
