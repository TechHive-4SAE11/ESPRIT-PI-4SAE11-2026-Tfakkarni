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

  // Quiz session
  currentQuiz = signal<QuizDTO | null>(null);
  currentQuestion = signal<QuestionDTO | null>(null);
  currentQuestionIndex = signal<number>(0);
  questionAnswers = signal<AnswerDTO[]>([]);
  selectedAnswer = signal<AnswerDTO | null>(null);
  answerSubmitted = signal<boolean>(false);
  quizScore = signal<number>(0);
  lastValidation = signal<{ isCorrect: boolean; explanation: string } | null>(null);

  // ─── LOADING ─────────────────────────────────────────────────
  isLoading = signal<boolean>(false);
  isLoadingAnswers = signal<boolean>(false);
  isSubmitting = signal<boolean>(false);

  // ─── NOTIFICATION ────────────────────────────────────────────
  notification = signal<{ message: string; type: 'success' | 'error' | 'info' } | null>(null);

  // ─── USER ────────────────────────────────────────────────────
  userNeonDbId = signal<number | null>(null);

  // ─── COMPUTED ────────────────────────────────────────────────
  availableQuizzes = computed(() => this.quizzes().filter(q => q.questions && q.questions.length > 0));
  totalQuestions = computed(() => this.currentQuiz()?.questions?.length ?? 0);
  scorePercent = computed(() => {
    const t = this.totalQuestions();
    return t === 0 ? 0 : Math.round((this.quizScore() / t) * 100);
  });

  ngOnInit(): void {
    if (this.keycloakId) this.loadUserInfo();
  }

  private notify(message: string, type: 'success' | 'error' | 'info' = 'info'): void {
    this.notification.set({ message, type });
    setTimeout(() => this.notification.set(null), 3500);
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
    // getAverageScoreByCaregiver
    this.quizService.getAverageScoreByCaregiver(caregiverId).pipe(
      tap(s => this.avgScore.set(s || 0)),
      catchError(() => of(0))
    ).subscribe();
    // getWeakTopicsByCaregiver
    this.quizService.getWeakTopicsByCaregiver(caregiverId).pipe(
      tap(t => this.weakTopics.set(t || [])),
      catchError(() => of([]))
    ).subscribe();
    // getQuizCountByCaregiver
    this.quizService.getQuizCountByCaregiver(caregiverId).pipe(
      tap(c => this.quizCount.set(c || 0)),
      catchError(() => of(0))
    ).subscribe();
    // getRecentQuizzesByCaregiver
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

  // ─── START QUIZ — startQuiz ───────────────────────────────────
  startQuiz(quiz: QuizDTO): void {
    const qid = this.userNeonDbId();
    // Appel API startQuiz
    this.quizService.startQuiz(quiz.id!).pipe(
      tap(() => {
        this.currentQuiz.set(quiz);
        this.currentQuestionIndex.set(0);
        this.quizScore.set(0);
        this.answerSubmitted.set(false);
        this.lastValidation.set(null);
        this.view.set('playing');
        this.loadCurrentQuestion();
      }),
      catchError(() => {
        // Démarrer quand même localement si l'API échoue
        this.currentQuiz.set(quiz);
        this.currentQuestionIndex.set(0);
        this.quizScore.set(0);
        this.answerSubmitted.set(false);
        this.lastValidation.set(null);
        this.view.set('playing');
        this.loadCurrentQuestion();
        return of(null);
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  // ─── LOAD QUESTION — getQuestionById + getAnswersByQuestionId ─
  private loadCurrentQuestion(): void {
    const quiz = this.currentQuiz();
    if (!quiz?.questions) return;
    const index = this.currentQuestionIndex();
    const question = quiz.questions[index];

    if (!question?.id) {
      this.currentQuestion.set(null); // quiz terminé
      return;
    }

    this.currentQuestion.set(question);
    this.selectedAnswer.set(null);
    this.answerSubmitted.set(false);
    this.lastValidation.set(null);

    // getAnswersByQuestionId
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

  // ─── SUBMIT — validateAnswer + submitAnswer ───────────────────
  submitAnswer(): void {
    const question = this.currentQuestion();
    const answer = this.selectedAnswer();
    const quiz = this.currentQuiz();
    if (!question?.id || !answer?.id || !quiz?.id) return;

    this.isSubmitting.set(true);

    // validateAnswer pour afficher immédiatement le résultat
    this.quizService.validateAnswer({ questionId: question.id, answerId: answer.id }).pipe(
      tap(validation => {
        this.lastValidation.set({
          isCorrect: validation.valid,
          explanation: validation.explanation ?? ''
        });
        if (validation.valid) this.quizScore.update(s => s + 1);
        this.answerSubmitted.set(true);

        // Puis submitAnswer pour enregistrer dans le backend
        this.quizService.submitAnswer({ quizId: quiz.id!, questionId: question.id!, answerId: answer.id! }).pipe(
          catchError(() => of(null))
        ).subscribe();
      }),
      catchError(() => {
        // Fallback: utiliser uniquement isCorrect local
        if (answer.isCorrect) this.quizScore.update(s => s + 1);
        this.answerSubmitted.set(true);
        return of(null);
      }),
      finalize(() => this.isSubmitting.set(false)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  nextQuestion(): void {
    const next = this.currentQuestionIndex() + 1;
    if (next >= this.totalQuestions()) {
      this.currentQuestion.set(null);
    } else {
      this.currentQuestionIndex.set(next);
      this.loadCurrentQuestion();
    }
  }

  // ─── FINISH — completeQuiz ────────────────────────────────────
  finishQuiz(): void {
    const quiz = this.currentQuiz();
    if (!quiz?.id) return;
    this.quizService.completeQuiz(quiz.id, this.quizScore()).pipe(
      tap(() => {
        this.view.set('result');
        const cid = this.userNeonDbId();
        if (cid) this.loadStats(cid);
      }),
      catchError(() => {
        this.view.set('result');
        return of(null);
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe();
  }

  backToList(): void {
    this.currentQuiz.set(null);
    this.currentQuestion.set(null);
    this.questionAnswers.set([]);
    this.currentQuestionIndex.set(0);
    this.quizScore.set(0);
    this.lastValidation.set(null);
    this.view.set('list');
    this.loadQuizzes();
  }

  cancelQuiz(): void {
    if (confirm('Abandonner ce quiz ? Votre progression sera perdue.')) {
      this.backToList();
    }
  }

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
    return level === 1 ? 'Facile' : level === 2 ? 'Moyen' : 'Difficile';
  }
  getDifficultyColor(level: number): string {
    return level === 1
      ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300'
      : level === 2
        ? 'bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-300'
        : 'bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300';
  }
  getScoreColor(percent: number): string {
    if (percent >= 80) return '#10b981';
    if (percent >= 50) return '#f59e0b';
    return '#ef4444';
  }
}
