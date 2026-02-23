// src/app/features/patient/components/patient-quiz/patient-quiz.component.ts
import {
  Component,
  OnInit,
  signal,
  Input,
  inject,
  DestroyRef,
  computed
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, of, tap } from 'rxjs';

import { QuizService } from '@/core/services/quiz.service';
import { UserApiService } from '@/core/services/user-api.service';
import { QuizDTO, QuestionDTO, AnswerDTO } from '@/core/models/quiz.model';
import { ZardCardComponent } from '@/shared/components/card';
import { ZardIconComponent } from '@/shared/components/icon';
import { ZardButtonComponent } from '@/shared/components/button';

@Component({
  selector: 'app-patient-quiz',
  standalone: true,
  imports: [
    CommonModule,
    ZardCardComponent,
    ZardIconComponent,
    ZardButtonComponent
  ],
  template: `
    <div class="space-y-6">
      <h1 class="text-3xl sm:text-4xl font-bold text-slate-800 dark:text-white mb-2">
        📝 Quizzes
      </h1>
      <p class="text-lg text-slate-500 dark:text-slate-400 mb-6">Test your memory with quizzes</p>

      <!-- Loading State -->
      @if (isLoading()) {
        <div class="text-center py-16">
          <p class="text-5xl mb-4 animate-pulse">⏳</p>
          <p class="text-slate-500 dark:text-slate-400 text-lg">Loading quizzes...</p>
        </div>
      }

      <!-- Quiz List (Not Started) -->
      @if (!currentQuiz() && !isLoading()) {
        @if (availableQuizzes().length > 0) {
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
      }

      <!-- Quiz Taking Interface -->
      @if (currentQuiz()) {
        <div class="space-y-6">
          <!-- Quiz Header -->
          <div class="flex items-center justify-between">
            <h2 class="text-2xl font-bold text-slate-800 dark:text-white">{{ currentQuiz()?.topic }}</h2>
            <button (click)="cancelQuiz()"
                    class="px-4 py-2 rounded-lg bg-red-500 hover:bg-red-600 text-white text-sm font-semibold">
              Cancel
            </button>
          </div>

          <!-- Current Question -->
          @if (currentQuestion(); as question) {
            <div class="rounded-2xl bg-white dark:bg-slate-800 border-2 border-slate-200 dark:border-slate-700 p-6 sm:p-8 shadow-lg">
              <!-- Progress -->
              <div class="mb-6">
                <div class="flex justify-between text-sm text-slate-500 dark:text-slate-400 mb-2">
                  <span>Question {{ currentQuestionIndex() + 1 }} of {{ totalQuestions() }}</span>
                  <span>Score: {{ quizScore() }}</span>
                </div>
                <div class="w-full h-2 bg-slate-200 dark:bg-slate-700 rounded-full overflow-hidden">
                  <div class="h-full bg-blue-500 transition-all duration-300"
                       [style.width.%]="((currentQuestionIndex() + 1) / totalQuestions()) * 100"></div>
                </div>
              </div>

              <!-- Question Text -->
              <h3 class="text-xl sm:text-2xl font-bold text-slate-800 dark:text-white mb-6">
                {{ question.text }}
              </h3>

              <!-- Difficulty Badge -->
              <div class="mb-4">
                @if (question.difficultyLevel === 1) {
                  <span class="px-3 py-1 bg-green-100 text-green-700 dark:bg-green-900 dark:text-green-300 rounded-full text-xs font-semibold">
                    Easy
                  </span>
                } @else if (question.difficultyLevel === 2) {
                  <span class="px-3 py-1 bg-yellow-100 text-yellow-700 dark:bg-yellow-900 dark:text-yellow-300 rounded-full text-xs font-semibold">
                    Medium
                  </span>
                } @else {
                  <span class="px-3 py-1 bg-red-100 text-red-700 dark:bg-red-900 dark:text-red-300 rounded-full text-xs font-semibold">
                    Hard
                  </span>
                }
              </div>

              <!-- Answers -->
              @if (isLoadingAnswers()) {
                <div class="space-y-3">
                  @for (i of [1,2,3,4]; track i) {
                    <div class="h-14 bg-slate-200 dark:bg-slate-700 rounded-xl animate-pulse"></div>
                  }
                </div>
              } @else if (questionAnswers().length > 0) {
                <div class="space-y-3">
                  @for (answer of questionAnswers(); track answer.id) {
                    <button (click)="selectAnswer(answer)"
                            [disabled]="answerSubmitted()"
                            [class]="getAnswerButtonClass(answer)">
                      {{ answer.text }}
                      @if (selectedAnswer()?.id === answer.id && answerSubmitted()) {
                        @if (answer.isCorrect) {
                          <span class="ml-2">✅</span>
                        } @else {
                          <span class="ml-2">❌</span>
                        }
                      }
                    </button>
                  }
                </div>

                <!-- Submit Button -->
                @if (!answerSubmitted()) {
                  <div class="mt-6">
                    <button (click)="submitAnswer()" [disabled]="!selectedAnswer()"
                            class="w-full px-6 py-4 rounded-xl bg-green-500 hover:bg-green-600 disabled:bg-slate-300 disabled:cursor-not-allowed text-white font-bold text-lg transition-all">
                      Submit Answer
                    </button>
                  </div>
                }

                <!-- Next Button -->
                @if (answerSubmitted()) {
                  <div class="mt-6">
                    <button (click)="nextQuestion()"
                            class="w-full px-6 py-4 rounded-xl bg-blue-500 hover:bg-blue-600 text-white font-bold text-lg transition-all">
                      @if (currentQuestionIndex() + 1 >= totalQuestions()) {
                        Finish Quiz
                      } @else {
                        Next Question
                      }
                    </button>
                  </div>
                }

                <!-- Explanation -->
                @if (answerSubmitted() && selectedAnswer()?.explanation) {
                  <div class="mt-4 p-4 bg-slate-100 dark:bg-slate-700 rounded-lg text-sm">
                    <p class="font-semibold mb-1">Explanation:</p>
                    <p>{{ selectedAnswer()?.explanation }}</p>
                  </div>
                }
              }
            </div>
          } @else {
            <!-- Quiz Completed -->
            <div class="text-center py-16">
              <p class="text-5xl mb-4">✅</p>
              <h2 class="text-xl font-semibold text-slate-700 dark:text-slate-300 mb-2">Quiz Completed!</h2>
              <p class="text-slate-500 dark:text-slate-400 text-lg mb-4">
                Your score: {{ quizScore() }} / {{ totalQuestions() }}
              </p>
              <div class="w-32 h-32 mx-auto mb-4">
                <svg viewBox="0 0 36 36" class="w-full h-full">
                  <path d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                        fill="none" stroke="#E2E8F0" stroke-width="3" />
                  <path d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                        fill="none" stroke="#3B82F6" stroke-width="3"
                        [attr.stroke-dasharray]="getScorePercentage() + ', 100'" />
                </svg>
              </div>
              <p class="text-2xl font-bold text-blue-600 mb-4">{{ getScorePercentage() }}%</p>
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
  styles: [`
    :host {
      display: block;
    }
  `]
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
  answerSubmitted = signal<boolean>(false);
  quizScore = signal<number>(0);

  // Loading states
  isLoading = signal<boolean>(false);
  isLoadingAnswers = signal<boolean>(false);

  // User info
  userNeonDbId = signal<number | null>(null);

  // Computed
  availableQuizzes = computed(() =>
    this.quizzes().filter(q => q.questions && q.questions.length > 0)
  );

  totalQuestions = computed(() =>
    this.currentQuiz()?.questions?.length ?? 0
  );

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
    this.answerSubmitted.set(false);
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
    this.answerSubmitted.set(false);

    // Load answers for this question
    this.isLoadingAnswers.set(true);
    this.quizService.getAnswersByQuestionId(question.id)
      .pipe(
        tap(answers => this.questionAnswers.set(answers)),
        catchError(err => {
          console.error('[PatientQuiz] Failed to load answers', err);
          return of([]);
        }),
        finalize(() => this.isLoadingAnswers.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  selectAnswer(answer: AnswerDTO): void {
    if (this.answerSubmitted()) return;
    this.selectedAnswer.set(answer);
  }

  getAnswerButtonClass(answer: AnswerDTO): string {
    const baseClass = 'w-full p-4 rounded-xl font-semibold text-left transition-all ';

    if (this.answerSubmitted()) {
      if (answer.id === this.selectedAnswer()?.id) {
        if (answer.isCorrect) {
          return baseClass + 'bg-green-500 text-white';
        } else {
          return baseClass + 'bg-red-500 text-white';
        }
      } else if (answer.isCorrect) {
        return baseClass + 'bg-green-200 dark:bg-green-800 text-green-800 dark:text-green-200';
      }
      return baseClass + 'bg-slate-100 dark:bg-slate-700 text-slate-500 dark:text-slate-400 opacity-50';
    }

    if (answer.id === this.selectedAnswer()?.id) {
      return baseClass + 'bg-blue-500 text-white';
    }

    return baseClass + 'bg-slate-100 dark:bg-slate-700 hover:bg-slate-200 dark:hover:bg-slate-600 text-slate-800 dark:text-white';
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
          this.answerSubmitted.set(true);
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

  nextQuestion(): void {
    const nextIndex = this.currentQuestionIndex() + 1;

    if (nextIndex >= this.totalQuestions()) {
      // Quiz completed, show summary
      this.currentQuestion.set(null);
    } else {
      // Load next question
      this.currentQuestionIndex.set(nextIndex);
      this.loadCurrentQuestion();
    }
  }

  cancelQuiz(): void {
    if (confirm('Are you sure you want to cancel this quiz? Your progress will be lost.')) {
      this.currentQuiz.set(null);
      this.currentQuestion.set(null);
      this.currentQuestionIndex.set(0);
      this.quizScore.set(0);
      this.answerSubmitted.set(false);
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
            this.answerSubmitted.set(false);
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

  getScorePercentage(): number {
    const total = this.totalQuestions();
    if (total === 0) return 0;
    return Math.round((this.quizScore() / total) * 100);
  }
}
