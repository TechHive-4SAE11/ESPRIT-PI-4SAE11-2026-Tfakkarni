import {
  Component,
  OnInit,
  signal,
  Input,
  inject,
  DestroyRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, of, tap, switchMap } from 'rxjs';

import { QuizService } from '@/core/services/quiz.service';
import { QuizDTO, QuestionDTO, AnswerDTO } from '@/core/models/quiz.model';
import { UserApiService } from '@/core/services/user-api.service';

@Component({
  selector: 'app-patient-quiz',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="space-y-6">
      <h1 class="text-3xl sm:text-4xl font-bold text-slate-800 dark:text-white mb-2">
        📝 Quizzes
      </h1>
      <p class="text-lg text-slate-500 dark:text-slate-400 mb-6">Test your memory with quizzes</p>

      @if (isLoading()) {
      <div class="text-center py-16">
        <p class="text-5xl mb-4 animate-pulse">⏳</p>
        <p class="text-slate-500 dark:text-slate-400 text-lg">Loading quizzes...</p>
      </div>
      } @else if (availableQuizzes().length > 0) {
      <div class="space-y-4">
        @for (quiz of availableQuizzes(); track quiz.id) {
        <button (click)="startQuiz(quiz)"
          class="w-full flex items-center gap-5 p-6 sm:p-8 rounded-2xl bg-blue-500 hover:bg-blue-600 active:scale-[0.98] text-white shadow-lg shadow-blue-500/25 transition-all text-left">
          <span class="text-4xl sm:text-5xl">📝</span>
          <div class="flex-1">
            <p class="text-xl sm:text-2xl font-bold">{{ quiz.topic }}</p>
            <p class="text-blue-100 text-sm sm:text-base">
              {{ quiz.questions?.length ?? 0 }} questions
            </p>
          </div>
          <span class="text-3xl text-white flex-shrink-0">▶</span>
        </button>
        }
      </div>
      } @else {
      <div class="text-center py-16">
        <p class="text-5xl mb-4">😊</p>
        <h2 class="text-xl font-semibold text-slate-700 dark:text-slate-300 mb-2">No quizzes available</h2>
        <p class="text-slate-500 dark:text-slate-400 text-lg">
          Ask your helper to create some quizzes for you!
        </p>
      </div>
      }

      <!-- Quiz Taking Interface -->
      @if (currentQuiz(); as quiz) {
      <div class="space-y-6">
        <div class="flex items-center justify-between">
          <h2 class="text-2xl font-bold text-slate-800 dark:text-white">{{ quiz.topic }}</h2>
          <button (click)="cancelQuiz()"
            class="px-4 py-2 rounded-lg bg-red-500 hover:bg-red-600 text-white text-sm font-semibold">
            Cancel
          </button>
        </div>

        @if (currentQuestion(); as question) {
        <div class="rounded-2xl bg-white dark:bg-slate-800 border-2 border-slate-200 dark:border-slate-700 p-6 sm:p-8 shadow-lg">
          <div class="mb-6">
            <p class="text-sm text-slate-500 dark:text-slate-400 mb-2">
              Question {{ currentQuestionIndex() + 1 }} of {{ quiz.questions?.length ?? 0 }}
            </p>
            <h3 class="text-xl sm:text-2xl font-bold text-slate-800 dark:text-white mb-4">
              {{ question.text }}
            </h3>
          </div>

          @if (questionAnswers().length > 0) {
          <div class="space-y-3">
            @for (answer of questionAnswers(); track answer.id) {
            <button (click)="selectAnswer(answer)"
              [class]="selectedAnswer()?.id === answer.id
                ? 'w-full p-4 rounded-xl bg-blue-500 text-white font-semibold text-left transition-all'
                : 'w-full p-4 rounded-xl bg-slate-100 dark:bg-slate-700 hover:bg-slate-200 dark:hover:bg-slate-600 text-slate-800 dark:text-white font-semibold text-left transition-all'">
              {{ answer.text }}
            </button>
            }
          </div>

          <div class="mt-6 flex gap-3">
            <button (click)="submitAnswer()" [disabled]="!selectedAnswer()"
              class="flex-1 px-6 py-4 rounded-xl bg-green-500 hover:bg-green-600 disabled:bg-slate-300 disabled:cursor-not-allowed text-white font-bold text-lg transition-all">
              Submit Answer
            </button>
          </div>
          } @else {
          <p class="text-slate-500 dark:text-slate-400">Loading answers...</p>
          }
        </div>
        } @else {
        <div class="text-center py-16">
          <p class="text-5xl mb-4">✅</p>
          <h2 class="text-xl font-semibold text-slate-700 dark:text-slate-300 mb-2">Quiz Completed!</h2>
          <p class="text-slate-500 dark:text-slate-400 text-lg mb-4">
            Your score: {{ quizScore() }} / {{ quiz.questions?.length ?? 0 }}
          </p>
          <button (click)="finishQuiz()"
            class="px-8 py-4 rounded-2xl bg-blue-500 hover:bg-blue-600 text-white text-lg font-bold shadow-lg transition-all">
            Done
          </button>
        </div>
        }
      </div>
      }
    </div>
  `,
})
export class PatientQuizComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly quizService = inject(QuizService);
  private readonly userApiService = inject(UserApiService);

  @Input() keycloakId = '';

  // State
  quizzes = signal<QuizDTO[]>([]);
  currentQuiz = signal<QuizDTO | null>(null);
  currentQuestion = signal<QuestionDTO | null>(null);
  currentQuestionIndex = signal<number>(0);
  questionAnswers = signal<AnswerDTO[]>([]);
  selectedAnswer = signal<AnswerDTO | null>(null);
  quizScore = signal<number>(0);
  isLoading = signal<boolean>(false);
  userNeonDbId = signal<number | null>(null);

  ngOnInit(): void {
    if (this.keycloakId) {
      this.loadUserInfo();
    }
  }

  private loadUserInfo(): void {
    this.userApiService.getUserByKeycloakId(this.keycloakId)
      .pipe(
        tap(userInfo => {
          this.userNeonDbId.set(userInfo.id);
          this.loadQuizzes();
        }),
        catchError(err => {
          console.error('[PatientQuiz] Failed to load user info', err);
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  private loadQuizzes(): void {
    const caregiverId = this.userNeonDbId();
    if (!caregiverId) return;

    this.isLoading.set(true);
    this.quizService.getQuizzesByCaregiverId(caregiverId)
      .pipe(
        tap(quizzes => {
          // Only show quizzes with questions
          const quizzesWithQuestions = quizzes.filter(q => q.questions && q.questions.length > 0);
          this.quizzes.set(quizzesWithQuestions);
        }),
        catchError(err => {
          console.error('[PatientQuiz] Failed to load quizzes', err);
          return of([]);
        }),
        finalize(() => this.isLoading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  startQuiz(quiz: QuizDTO): void {
    this.currentQuiz.set(quiz);
    this.currentQuestionIndex.set(0);
    this.quizScore.set(0);
    this.loadCurrentQuestion();
  }

  private loadCurrentQuestion(): void {
    const quiz = this.currentQuiz();
    if (!quiz || !quiz.questions) return;

    const index = this.currentQuestionIndex();
    const question = quiz.questions[index];

    if (!question || !question.id) {
      // Quiz completed
      this.currentQuestion.set(null);
      return;
    }

    this.currentQuestion.set(question);
    this.selectedAnswer.set(null);

    // Load answers for this question
    this.quizService.getAnswersByQuestionId(question.id)
      .pipe(
        tap(answers => this.questionAnswers.set(answers)),
        catchError(err => {
          console.error('[PatientQuiz] Failed to load answers', err);
          return of([]);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  selectAnswer(answer: AnswerDTO): void {
    this.selectedAnswer.set(answer);
  }

  submitAnswer(): void {
    const question = this.currentQuestion();
    const answer = this.selectedAnswer();
    const quiz = this.currentQuiz();

    if (!question || !answer || !quiz || !question.id || !answer.id || !quiz.id) return;

    this.quizService.submitAnswer({
      quizId: quiz.id,
      questionId: question.id,
      answerId: answer.id
    })
      .pipe(
        tap(response => {
          if (response.correct) {
            this.quizScore.update(score => score + 1);
          }
          // Move to next question
          this.currentQuestionIndex.update(idx => idx + 1);
          setTimeout(() => this.loadCurrentQuestion(), 500);
        }),
        catchError(err => {
          console.error('[PatientQuiz] Failed to submit answer', err);
          alert('Failed to submit answer. Please try again.');
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  cancelQuiz(): void {
    if (confirm('Are you sure you want to cancel this quiz? Your progress will be lost.')) {
      this.currentQuiz.set(null);
      this.currentQuestion.set(null);
      this.currentQuestionIndex.set(0);
      this.quizScore.set(0);
    }
  }

  finishQuiz(): void {
    const quiz = this.currentQuiz();
    if (quiz && quiz.id) {
      const totalQuestions = quiz.questions?.length ?? 0;
      const score = this.quizScore();
      
      this.quizService.completeQuiz(quiz.id, score)
        .pipe(
          tap(() => {
            this.currentQuiz.set(null);
            this.currentQuestion.set(null);
            this.currentQuestionIndex.set(0);
            this.quizScore.set(0);
            this.loadQuizzes(); // Reload to show updated scores
          }),
          catchError(err => {
            console.error('[PatientQuiz] Failed to complete quiz', err);
            return of(null);
          }),
          takeUntilDestroyed(this.destroyRef)
        )
        .subscribe();
    }
  }

  get availableQuizzes(): () => QuizDTO[] {
    return () => {
      return this.quizzes().filter(q => q.questions && q.questions.length > 0);
    };
  }
}
