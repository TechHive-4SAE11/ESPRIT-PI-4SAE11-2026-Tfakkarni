import {
  Component,
  signal,
  inject,
  DestroyRef,
  OnInit
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, of, tap, switchMap } from 'rxjs';

import { ZardCardComponent } from '@/shared/components/card';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardIconComponent } from '@/shared/components/icon';
import { QuizService } from '@/core/services/quiz.service';
import { QuizDTO, QuestionDTO, AnswerDTO } from '@/core/models/quiz.model';

@Component({
  selector: 'app-public-quiz',
  standalone: true,
  imports: [
    CommonModule,
    ZardCardComponent,
    ZardButtonComponent,
    ZardIconComponent
  ],
  template: `
    <div class="w-full max-w-4xl mx-auto">
      @if (!quizStarted()) {
      <!-- Quiz Introduction -->
      <z-card class="p-8 text-center">
        <z-icon zType="brain" class="h-16 w-16 mx-auto mb-4 text-primary" />
        <h2 class="text-3xl font-bold mb-4">Alzheimer Risk Assessment Quiz</h2>
        <p class="text-lg text-muted-foreground mb-6 max-w-2xl mx-auto">
          Take this quick assessment to evaluate potential signs of Alzheimer's disease.
          This quiz consists of {{ totalQuestions() }} questions and takes approximately 5 minutes.
        </p>
        <p class="text-sm text-muted-foreground mb-8">
          <strong>Note:</strong> This is a preliminary assessment. Please consult with a healthcare professional for a proper diagnosis.
        </p>
        <button z-button zSize="lg" (click)="startQuiz()" [disabled]="isLoading()">
          @if (isLoading()) {
          <z-icon zType="loader-2" class="mr-2 animate-spin" />
          Loading Quiz...
          } @else {
          Start Assessment
          <z-icon zType="arrow-right" class="ml-2" />
          }
        </button>
      </z-card>
      }

      @if (quizStarted() && currentQuestion(); as question) {
      <!-- Quiz Question -->
      <z-card class="p-8">
        <div class="mb-6">
          <div class="flex items-center justify-between mb-4">
            <span class="text-sm text-muted-foreground">
              Question {{ currentQuestionIndex() + 1 }} of {{ totalQuestions() }}
            </span>
            <div class="w-32 h-2 bg-muted rounded-full overflow-hidden">
              <div class="h-full bg-primary transition-all duration-300" 
                   [style.width.%]="((currentQuestionIndex() + 1) / totalQuestions() * 100)">
              </div>
            </div>
          </div>
          <h3 class="text-2xl font-bold mb-4">{{ question.text }}</h3>
        </div>

        @if (questionAnswers().length > 0) {
        <div class="space-y-3 mb-6">
          @for (answer of questionAnswers(); track answer.id) {
          <button
            (click)="selectAnswer(answer)"
            [class]="selectedAnswer()?.id === answer.id
              ? 'w-full p-4 rounded-lg bg-primary text-primary-foreground font-semibold text-left transition-all border-2 border-primary'
              : 'w-full p-4 rounded-lg bg-muted hover:bg-muted/80 font-semibold text-left transition-all border-2 border-transparent'">
            {{ answer.text }}
          </button>
          }
        </div>

        <div class="flex gap-3">
          <button z-button (click)="submitAnswer()" [disabled]="!selectedAnswer()">
            @if (submitting()) {
            <z-icon zType="loader-2" class="mr-2 animate-spin" />
            Submitting...
            } @else {
            Next Question
            <z-icon zType="arrow-right" class="ml-2" />
            }
          </button>
        </div>
        } @else {
        <div class="text-center py-8">
          <z-icon zType="loader-2" class="h-8 w-8 mx-auto animate-spin text-muted-foreground" />
          <p class="text-muted-foreground mt-4">Loading answers...</p>
        </div>
        }
      </z-card>
      }

      @if (quizCompleted() && showResultsDialog()) {
      <!-- Quiz Results Modal -->
      <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm" (click)="closeDialog()">
        <z-card class="max-w-md w-full mx-4 p-6" (click)="$event.stopPropagation()">
          <div class="text-center mb-6">
            <h2 class="text-2xl font-bold mb-2">
              @if (scorePercentage() >= 50) {
              ⚠️ Assessment Results
              } @else {
              ✅ Assessment Results
              }
            </h2>
          </div>
          
          <div class="py-4">
            <div class="text-center mb-6">
              <div class="text-6xl font-bold mb-2" [class]="scorePercentage() >= 50 ? 'text-red-500' : 'text-green-500'">
                {{ scorePercentage() }}%
              </div>
              <p class="text-lg text-muted-foreground">
                You scored {{ correctAnswers() }} out of {{ totalQuestions() }} questions correctly.
              </p>
            </div>

            @if (scorePercentage() >= 50) {
            <div class="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4 mb-6">
              <div class="flex items-start gap-3">
                <z-icon zType="triangle-alert" class="h-6 w-6 text-red-500 flex-shrink-0 mt-0.5" />
                <div>
                  <h4 class="font-semibold text-red-900 dark:text-red-100 mb-2">
                    Potential Signs Detected
                  </h4>
                  <p class="text-sm text-red-800 dark:text-red-200">
                    Your assessment results indicate potential signs of Alzheimer's disease. 
                    We strongly recommend consulting with a healthcare professional for a proper evaluation.
                  </p>
                </div>
              </div>
            </div>
            } @else {
            <div class="bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg p-4 mb-6">
              <div class="flex items-start gap-3">
                <z-icon zType="check-circle" class="h-6 w-6 text-green-500 flex-shrink-0 mt-0.5" />
                <div>
                  <h4 class="font-semibold text-green-900 dark:text-green-100 mb-2">
                    Low Risk Detected
                  </h4>
                  <p class="text-sm text-green-800 dark:text-green-200">
                    Your assessment results show low risk indicators. However, if you have concerns, 
                    please consult with a healthcare professional.
                  </p>
                </div>
              </div>
            </div>
            }

            <div class="space-y-3">
              <p class="text-sm text-muted-foreground text-center mb-4">
                Would you like to create an account to track your progress and access personalized care?
              </p>
              <button z-button class="w-full" (click)="goToSignup()">
                Create Account
                <z-icon zType="arrow-right" class="ml-2" />
              </button>
              <button z-button zType="outline" class="w-full" (click)="closeDialog()">
                Maybe Later
              </button>
            </div>
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

  // State
  quizStarted = signal<boolean>(false);
  quizCompleted = signal<boolean>(false);
  showResultsDialog = signal<boolean>(false);
  isLoading = signal<boolean>(false);
  submitting = signal<boolean>(false);
  
  // Quiz data
  currentQuiz = signal<QuizDTO | null>(null);
  currentQuestion = signal<QuestionDTO | null>(null);
  currentQuestionIndex = signal<number>(0);
  questionAnswers = signal<AnswerDTO[]>([]);
  selectedAnswer = signal<AnswerDTO | null>(null);
  
  // Score tracking
  correctAnswers = signal<number>(0);
  totalQuestions = signal<number>(0);

  ngOnInit(): void {
    // Load a default public quiz (you may need to create one or use quiz ID 1)
    // For now, we'll try to load quiz with ID 1 or create a default one
  }

  startQuiz(): void {
    this.isLoading.set(true);
    
    // Try to get a public quiz - first try quiz ID 1, then fallback to all quizzes
    this.quizService.getQuizById(1)
      .pipe(
        switchMap(quiz => {
          if (quiz && quiz.questions && quiz.questions.length > 0) {
            return of(quiz);
          }
          // If quiz 1 doesn't have questions, try to get all quizzes
          return this.quizService.getAllQuizzes().pipe(
            switchMap(quizzes => {
              if (!quizzes || quizzes.length === 0) {
                return of(null);
              }
              const publicQuiz = quizzes.find(q => 
                q.topic?.toLowerCase().includes('public') || 
                q.topic?.toLowerCase().includes('assessment') ||
                q.topic?.toLowerCase().includes('alzheimer')
              ) || quizzes[0];
              
              if (publicQuiz && publicQuiz.id) {
                return this.quizService.getQuizById(publicQuiz.id);
              }
              return of(null);
            })
          );
        }),
        tap(quiz => {
          if (quiz && quiz.questions && quiz.questions.length > 0) {
            this.currentQuiz.set(quiz);
            this.totalQuestions.set(quiz.questions.length);
            this.quizStarted.set(true);
            this.currentQuestionIndex.set(0);
            this.loadCurrentQuestion();
          } else {
            alert('No quiz available. Please contact support or create an account to access quizzes.');
          }
        }),
        catchError(err => {
          console.error('[PublicQuiz] Failed to load quiz', err);
          alert('Failed to load quiz. Please try again later or create an account.');
          return of(null);
        }),
        finalize(() => this.isLoading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  private loadCurrentQuestion(): void {
    const quiz = this.currentQuiz();
    if (!quiz || !quiz.questions) return;

    const index = this.currentQuestionIndex();
    const question = quiz.questions[index];

    if (!question || !question.id) {
      // Quiz completed
      this.completeQuiz();
      return;
    }

    this.currentQuestion.set(question);
    this.selectedAnswer.set(null);

    // Load answers for this question
    this.quizService.getAnswersByQuestionId(question.id)
      .pipe(
        tap(answers => this.questionAnswers.set(answers)),
        catchError(err => {
          console.error('[PublicQuiz] Failed to load answers', err);
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
    const answer = this.selectedAnswer();
    if (!answer) return;

    this.submitting.set(true);

    // Check if answer is correct
    const isCorrect = answer.isCorrect;
    if (isCorrect) {
      this.correctAnswers.update(count => count + 1);
    }

    // Move to next question
    setTimeout(() => {
      this.currentQuestionIndex.update(idx => idx + 1);
      this.submitting.set(false);
      this.loadCurrentQuestion();
    }, 500);
  }

  private completeQuiz(): void {
    this.quizCompleted.set(true);
    this.showResultsDialog.set(true);
  }

  get scorePercentage(): () => number {
    return () => {
      const total = this.totalQuestions();
      if (total === 0) return 0;
      return Math.round((this.correctAnswers() / total) * 100);
    };
  }

  goToSignup(): void {
    this.closeDialog();
    this.router.navigate(['/signup']);
  }

  closeDialog(): void {
    this.showResultsDialog.set(false);
  }
}
